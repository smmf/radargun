package org.radargun.cachewrappers;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radargun.CacheWrapper;
import org.radargun.features.AtomicOperationsCapable;
import org.radargun.features.BulkOperationsCapable;
import org.radargun.utils.TypedProperties;


/**
 * An implementation of SerializableCacheWrapper that uses EHCache as an underlying implementation.
 * <p/>
 * Pass in a -Dbind.address=IP_ADDRESS ehcache propery files allows referencing system properties through syntax
 * ${bind.address}.
 *
 * @author Manik Surtani (manik@surtani.org)
 */
public class EHCacheWrapper implements CacheWrapper, AtomicOperationsCapable, BulkOperationsCapable {
//   protected TransactionManager tm;
   private CacheManager manager;
   private Cache cache;
   private Log log = LogFactory.getLog("org.radargun.cachewrappers.EHCacheWrapper");
   boolean localMode;
   private String configFile, cacheName;

   public void setUp(String config, boolean isLocal, int nodeIndex, TypedProperties confAttributes) throws Exception {
      if (log.isTraceEnabled()) log.trace("Entering EHCacheWrapper.setUp()");
      localMode = isLocal;
      log.debug("Initializing the cache with props " + config);

      configFile  = confAttributes.containsKey("file") ? confAttributes.getProperty("file") : config;
      cacheName = confAttributes.containsKey("cache") ? confAttributes.getProperty("cache") : "x";


      log.debug("Initializing the cache with props " + config);
      URL url = getClass().getClassLoader().getResource(configFile);
      manager = new CacheManager(url);
      log.info("Caches available:");

      
      for (String s : manager.getCacheNames()) log.info("    * " + s);
      cache = manager.getCache(cacheName);

//      tm = cache.getTransactionManagerLookup().getTransactionManager();

      log.info("Using named cache " + cache);
      if (!localMode) {
         log.info("Bounded peers: " + manager.getCachePeerListener("RMI").getBoundCachePeers());
         log.info("Remote peers: " + manager.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(cache));
      }

      log.debug("Finish Initializing the cache");
   }

   public void tearDown() throws Exception {
      manager.shutdown();
   }

   @Override
   public boolean isRunning() {
      return manager.getStatus() == Status.STATUS_ALIVE;
   }

   public void putSerializable(final Serializable key, final Serializable value) throws Exception {
      doTransactionallyFlat(new Callable<Void>() {

         @Override
         public Void call() throws Exception {
            Element element = new Element(key, value);
            cache.put(element);
            return null;
         }
      });
   }

   public Object getSerializable(final Serializable key) throws Exception {
      return doTransactionallyFlat(new Callable<Object>() {

         @Override
         public Object call() throws Exception {
            return cache.get(key);
         }
      });
   }

   public void empty() throws Exception {
      doTransactionallyFlat(new Callable<Void>() {

         @Override
         public Void call() throws Exception {
            cache.removeAll();
            return null;
         }
      });
   }

   public void put(String path, Object key, Object value) throws Exception {
      putSerializable((Serializable) key, (Serializable) value);
   }

   public Object get(String bucket, Object key) throws Exception {
      Object s = getSerializable((Serializable) key);
      if (s instanceof Element) {
         return ((Element) s).getValue();
      } else return s;
   }

   @Override
   public Object remove(final String bucket, final Object key) throws Exception {
      return doTransactionallyFlat(new Callable<Object>() {

         @Override
         public Object call() throws Exception {
           return cache.remove(key);
         }
      });
   }

   @Override
   public boolean replace(String bucket, Object key, Object oldValue, Object newValue) throws Exception {
      Serializable sKey = (Serializable) key;
      final Element oldElement = new Element(sKey, (Serializable) oldValue);
      final Element newElement = new Element(sKey, (Serializable) newValue);
      boolean autoCommit = false;

      return doTransactionallyFlat(new Callable<Boolean>() {

         @Override
         public Boolean call() throws Exception {
            return cache.replace(oldElement, newElement);
         }
      });
   }

   @Override
   public Object putIfAbsent(String bucket, final Object key, final Object value) throws Exception {
      return doTransactionallyFlat(new Callable<Object>() {

         @Override
         public Object call() throws Exception {
            Element element = new Element((Serializable) key, (Serializable) value);
            Element previous = cache.putIfAbsent(element);
            return previous.getValue();
         }
      });
   }

   @Override
   public boolean remove(String bucket, Object key, Object oldValue) throws Exception {
      final Element element = new Element((Serializable) key, (Serializable) oldValue);
      return doTransactionallyFlat(new Callable<Boolean>() {

         @Override
         public Boolean call() throws Exception {
            return cache.removeElement(element);
         }
      });
   }

   public int getNumMembers() {
      return localMode ? 1 : manager.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(cache).size() + 1;
   }

   public String getInfo() {
      return "EHCache " + (localMode ? "" : (" remote peers: " + manager.getCachePeerListener("RMI").getBoundCachePeers())) + ", config: " + configFile + ", cacheName: " + cacheName;
   }

   public Object getReplicatedData(String path, String key) throws Exception {
      Object o = get(path, key);
      if (log.isTraceEnabled()) {
         log.trace("Result for the key: '" + key + "' is value '" + o + "'");
      }
      return o;
   }

   @Override
   public boolean isTransactional(String bucket) {
      return true;
   }

   public void startTransaction() {
      manager.getTransactionController().begin(10); // this is a problem. It looks like timeouts are used to restart a tx that touches the same keys (mapTxIdToCommitVersion)
   }

   public void endTransaction(boolean successful) {
      try {
         if (successful) {
            manager.getTransactionController().commit();
         } else {
            manager.getTransactionController().rollback();
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public int getLocalSize() {
      try {
         return doTransactionallyFlat(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
               return cache.getKeys().size();
            }
         });
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public int getTotalSize() {
      return -1;
   }

   @Override
   public Map<Object, Object> getAll(String bucket, final Set<Object> keys, boolean preferAsync) throws Exception {
      return doTransactionallyFlat(new Callable<Map<Object, Object>>() {

         @Override
         public Map<Object, Object> call() throws Exception {
            Map<Object, Object> map = new HashMap<Object, Object>();
            Map<Object, Element> elements = cache.getAll(keys);
            for (Element element : elements.values()) {
               map.put(element.getObjectKey(), element.getObjectValue());
            }
            return map;
         }
      });
   }

   @Override
   public Map<Object, Object> putAll(String bucket, final Map<Object, Object> entries, boolean preferAsync)
         throws Exception {
      return doTransactionallyFlat(new Callable<Map<Object, Object>>() {

         @Override
         public Map<Object, Object> call() throws Exception {
            ArrayList<Element> elements = new ArrayList<Element>(entries.size());
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
               elements.add(new Element(entry.getKey(), entry.getValue()));
            }
            cache.putAll(elements);
            return null;
         }
      });
   }

   @Override
   public Map<Object, Object> removeAll(String bucket, final Set<Object> keys, boolean preferAsync) throws Exception {
      return doTransactionallyFlat(new Callable<Map<Object, Object>>() {

         @Override
         public Map<Object, Object> call() throws Exception {
            cache.removeAll(keys);
            return null;
         }
      });
   }
   
   protected <T> T doTransactionallyFlat(Callable<T> command) throws Exception {
      boolean inTopLevel = false;

      // decide whether to wrap in a transaction
      if (manager.getTransactionController().getCurrentTransactionContext() == null) {
         inTopLevel = true;
         startTransaction();
      }

      boolean commandFinished = false;
      try {
         // execute command
         T result = command.call();
         commandFinished = true;
         return result;
      } finally {
         // end tx if needed
         if (inTopLevel) {
            endTransaction(commandFinished);
         }
      }
   }
}
