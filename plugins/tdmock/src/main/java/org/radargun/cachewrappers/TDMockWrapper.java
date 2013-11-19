package org.radargun.cachewrappers;

import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.radargun.CacheWrapper;
import org.radargun.utils.TypedProperties;
import org.radargun.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//public class TDMockWrapper extends MockWrapper {
public class TDMockWrapper implements CacheWrapper/*
                                                   * , BulkOperationsCapable,
                                                   * AtomicOperationsCapable
                                                   */{

   private static final Logger logger = LoggerFactory.getLogger(TDMockWrapper.class);
   //      private static final Logger logger = LoggerFactory.getLogger(MockWrapper.class);

   //      private static final String STR_TRUE = "true";
   private static final String REAL_WRAPPER = "infinispan53";
   private static final String MISSING_PRODUCT_PLUGIN = "Please provide the name of a valid plugin to decorate using the 'decorated=' property";
   private static final String UNDERLYING_WRAPPER_EXCEPTION = "Exception in underlying cache wrapper";

   private static final int MAX_DISTRIBUTION_THRESHOLD = 500;

   protected final ConcurrentHashMap<Object, Object> chm = new ConcurrentHashMap<Object, Object>();

   private CacheWrapper realWrapper;
   private AtomicInteger distributionThreshold = new AtomicInteger(0);

   private int getSleepNanos = 0;
   private int putSleepNanos = 0;
   private int commitSleepNanos = 0;

   private static final ThreadLocal<Boolean> usingRealWrapper = new ThreadLocal<Boolean>();

   private boolean shouldUseRealWrapper() {
      Boolean value = usingRealWrapper.get();
      return (value != null && value) || (this.distributionThreshold.get() < MAX_DISTRIBUTION_THRESHOLD);
   }

   @Override
   public void setUp(String configuration, boolean isLocal, int nodeIndex, TypedProperties confAttributes)
         throws Exception {
      logger.info("Setup TDMock");
      logger.info("config={}, isLocal={}, nodeIndex={}, confAttributes={}", configuration, isLocal, nodeIndex,
            confAttributes);

      String getSleepStr = confAttributes.getProperty("getSleepNanos");
      String putSleepStr = confAttributes.getProperty("putSleepNanos");
      String commitSleepStr = confAttributes.getProperty("commitSleepNanos");

      try {
         this.getSleepNanos = getSleepStr == null ? 0 : Integer.parseInt(getSleepStr);
         this.putSleepNanos = putSleepStr == null ? 0 : Integer.parseInt(putSleepStr);
         this.commitSleepNanos = commitSleepStr == null ? 0 : Integer.parseInt(commitSleepStr);
      } catch (NumberFormatException e) {
         logger.error("Failed to configure sleep time for either get/put/commit:", e);
         throw e;
      }

      logger.info("Sleep times: GET={}ns, PUT={}ns, COMMIT={}ns", this.getSleepNanos, this.putSleepNanos,
            this.commitSleepNanos);

      try {
         this.realWrapper = getCacheWrapper(REAL_WRAPPER);
      } catch (Exception e) {
         logger.error("Could not obtain decorated cache wrapper", e);
         throw new IllegalArgumentException(MISSING_PRODUCT_PLUGIN, e);
      }

      //     logger.info("config={}, isLocal={}, nodeIndex={}, confAttributes={}", configuration, isLocal, nodeIndex, confAttributes);

      try {
         this.realWrapper.setUp(configuration, isLocal, nodeIndex, confAttributes);
      } catch (Exception e) {
         logger.error("Could not setup decorated cache wrapper", e);
         throw new RuntimeException(e);
      }

   }

   public void tearDown() throws Exception {
      logger.info("TearDown");
      this.realWrapper.tearDown();
   }

   @Override
   public boolean isRunning() {
      return true;
   }

   @Override
   public void put(String bucket, Object key, Object value) throws Exception {
      //      super.put(bucket, key, value);
      logger.debug("PUT: bucket={{}}, key={{}}, value={{}}", bucket, key, value);
      if (shouldUseRealWrapper()) {
         chm.put(key, value); // don't sleep here
         logger.debug("---> also to the realWrapper");
         this.realWrapper.put(bucket, key, value);
      } else {
         sleepyPut(key, value);
      }
   }

   @Override
   public Object get(String bucket, Object key) throws Exception {
      logger.debug("GET: bucket={{}}, key={{}}", bucket, key);

      if (shouldUseRealWrapper()) {
         logger.debug("---> from realWrapper");
         return realWrapper.get(bucket, key);
      } else {
         logger.debug("---> from mock");
         return sleepyGet(key);
      }
   }

   @Override
   public Object remove(String bucket, Object key) throws Exception {
      throw new UnsupportedOperationException("not yet implemented");
      //      return chm.remove(key);
   }

   //   @Override
   //   public boolean replace(String bucket, Object key, Object oldValue, Object newValue) throws Exception {
   //      return chm.replace(key, oldValue, newValue);
   //   }
   //
   //   @Override
   //   public Object putIfAbsent(String bucket, Object key, Object value) throws Exception {
   //      return chm.putIfAbsent(key, value);
   //   }
   //
   //   @Override
   //   public boolean remove(String bucket, Object key, Object oldValue) throws Exception {
   //            return chm.remove(key, oldValue);
   //   }
   //
   //   @Override
   //   public Map<Object, Object> getAll(String bucket, Set<Object> keys, boolean preferAsyncOperations) throws Exception {
   //      Map<Object, Object> values = new HashMap<Object, Object>(keys.size());
   //      for (Object key : keys) {
   //         values.put(key, chm.get(key));
   //      }
   //      return values;
   //   }
   //
   //   @Override
   //   public Map<Object, Object> putAll(String bucket, Map<Object, Object> entries, boolean preferAsyncOperations) throws Exception {
   //      chm.putAll(entries);
   //      return null;
   //   }
   //
   //   @Override
   //   public Map<Object, Object> removeAll(String bucket, Set<Object> keys, boolean preferAsyncOperations) throws Exception {
   //      Map<Object, Object> values = new HashMap<Object, Object>(keys.size());
   //      for (Object key : keys) {
   //         values.put(key, chm.remove(key));
   //      }
   //      return values;
   //   }

   public void empty() throws Exception {
      chm.clear();
      this.realWrapper.empty();
   }

   public int getNumMembers() {
      //      if (logger.isDebugEnabled()) {
      //         new Throwable("Will return 1 from getNumMembers().  Is this correct?").printStackTrace();
      //      }
      //      int num = StartHelper.cheatNumMembers.get();
      //      logger.debug("Cheating on the number of members. I know it's supposed to be {}", num);
      //      return num;
      return this.realWrapper.getNumMembers();
   }

   public String getInfo() {
      return "Temporarily Distributed Mock Wrapper";
   }

   @Override
   public Object getReplicatedData(String bucket, String key) throws Exception {
      return get(bucket, key);
   }

   @Override
   public boolean isTransactional(String bucket) {
      return true;
   }

   public void startTransaction() {
      logger.debug("Begin tx requested");
      if (shouldUseRealWrapper()) {
         logger.debug("---> start tx on realWrapper");

         this.realWrapper.startTransaction();
         usingRealWrapper.set(Boolean.TRUE);
         this.distributionThreshold.incrementAndGet();
      }
   }

   public void endTransaction(boolean successful) {
      logger.debug("{} tx requested", (successful ? "Commit" : "Rollback"));
      if (shouldUseRealWrapper()) {
         logger.debug("---> end tx on realWrapper");
         usingRealWrapper.set(Boolean.FALSE);
         this.realWrapper.endTransaction(successful);
      } else {
         // just simulate the access with a delay
         if (successful) {
            //            sleep(commitSleepNanos, 0);
            sleep(commitSleepNanos);
         }
      }
   }

   @Override
   public int getLocalSize() {
      return chm.size();
   }

   @Override
   public int getTotalSize() {
      return -1;
   }

   // Adapted from LocalBenchmark.getCacheWrapper
   private CacheWrapper getCacheWrapper(String product) throws Exception {
      String fqnClass = Utils.getCacheWrapperFqnClass(product);

      URLClassLoader loader = Utils.buildProductSpecificClassLoader(product, getClass().getClassLoader());
      Thread.currentThread().setContextClassLoader(loader);
      return (CacheWrapper) loader.loadClass(fqnClass).newInstance();
   }

   private Object sleepyGet(Object key) {
      //      sleep(0, this.getSleepNanos);
      sleep(this.getSleepNanos);
      return chm.get(key);
   }

   private void sleepyPut(Object key, Object value) {
      //      sleep(0, this.putSleepNanos);
      sleep(this.putSleepNanos);
      chm.put(key, value);
   }

   public static void sleep(long nanos) {
      long now = System.nanoTime();
      long requestedTime = now + nanos;

      while (now < requestedTime) {
         now = System.nanoTime();
      }
   }

   public static void sleep(int millis, int nanos) {
      try {
         Thread.sleep(millis, nanos);
      } catch (InterruptedException e) {
         logger.info("Sleep was interrupted.");
      }
   }

}
