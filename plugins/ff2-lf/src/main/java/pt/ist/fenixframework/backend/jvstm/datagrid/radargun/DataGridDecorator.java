package pt.ist.fenixframework.backend.jvstm.datagrid.radargun;

import java.net.URLClassLoader;

import org.radargun.CacheWrapper;
import org.radargun.features.AtomicOperationsCapable;
import org.radargun.utils.TypedProperties;
import org.radargun.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.backend.jvstm.lf.JvstmLockFreeConfig;
import pt.ist.fenixframework.backend.jvstm.repository.DataGrid;
import pt.ist.fenixframework.backend.jvstm.repository.PersistenceException;

public class DataGridDecorator implements DataGrid {

    private static final Logger logger = LoggerFactory.getLogger(DataGridDecorator.class);

    public static final String DECORATE_KEY = "decorate";

    private static final String MISSING_PRODUCT_PLUGIN =
            "Please provide the name of a valid plugin to decorate using the 'decorated=' property";

    private static final String UNDERLYING_WRAPPER_EXCEPTION = "Exception in underlying cache wrapper";

    private static final Class<? extends CacheWrapper> INFINISPAN_WRAPPER_CLASS = initInfinispanWrapperClass();

    private static Class<? extends CacheWrapper> initInfinispanWrapperClass() {
        try {
            Class<? extends CacheWrapper> result =
                    Class.forName("org.radargun.cachewrappers.InfinispanWrapper").asSubclass(CacheWrapper.class);
            logger.info("Successfuly obtained INFINISPAN_WRAPPER_CLASS");
            return result;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private CacheWrapper cacheWrapper;
    private boolean inTransaction = false;

    @Override
    public void init(JvstmLockFreeConfig ffConfig) {
        RadarGunDataGridConfig radarGunConfig = (RadarGunDataGridConfig) ffConfig;

        TypedProperties confAttributes = radarGunConfig.getConfAttributes();
        String product = (String) confAttributes.remove(DECORATE_KEY);

        if (product == null) {
            throw new IllegalArgumentException(MISSING_PRODUCT_PLUGIN);
        }

        try {
            this.cacheWrapper = getCacheWrapper(product);
        } catch (Exception e) {
            logger.error("Could not obtain decorated cache wrapper", e);
            throw new IllegalArgumentException(MISSING_PRODUCT_PLUGIN, e);
        }

        String config = radarGunConfig.getConfig();
        boolean isLocal = radarGunConfig.getIsLocal();
        int nodeIndex = radarGunConfig.getNodeIndex();

        logger.info("config={}, isLocal={}, nodeIndex={}, confAttributes={}", config, isLocal, nodeIndex, confAttributes);

        try {
            this.cacheWrapper.setUp(config, isLocal, nodeIndex, confAttributes);
        } catch (Exception e) {
            logger.error("Could not setup decorated cache wrapper", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            this.cacheWrapper.tearDown();
        } catch (Exception e) {
            logger.warn(UNDERLYING_WRAPPER_EXCEPTION, e);
            throw new PersistenceException(UNDERLYING_WRAPPER_EXCEPTION, e);
        }
    }

    @Override
    public Object get(Object key) {
        try {
            return this.cacheWrapper.get(null, key);
        } catch (Exception e) {
            logger.warn(UNDERLYING_WRAPPER_EXCEPTION, e);
            throw new PersistenceException(UNDERLYING_WRAPPER_EXCEPTION, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        try {
            this.cacheWrapper.put(null, key, value);
        } catch (Exception e) {
            logger.warn(UNDERLYING_WRAPPER_EXCEPTION, e);
            throw new PersistenceException(UNDERLYING_WRAPPER_EXCEPTION, e);
        }
    }

    @Override
    public void putIfAbsent(Object key, Object value) {
        try {
            if (canUsePutIfAbsent()) {
                AtomicOperationsCapable atomicWrapper = (AtomicOperationsCapable) this.cacheWrapper;
                atomicWrapper.putIfAbsent(null, key, value);
            } else {
                Object current = this.cacheWrapper.get(null, key);
                if (current == null) {
                    this.cacheWrapper.put(null, key, value);
                }
            }
        } catch (Exception e) {
            logger.warn(UNDERLYING_WRAPPER_EXCEPTION, e);
            throw new PersistenceException(UNDERLYING_WRAPPER_EXCEPTION, e);
        }
    }

    private boolean canUsePutIfAbsent() {
        return (INFINISPAN_WRAPPER_CLASS != null) && (INFINISPAN_WRAPPER_CLASS.isInstance(this.cacheWrapper));
    }

    @Override
    public void beginTransaction() {
        try {
            this.cacheWrapper.startTransaction();
            this.inTransaction = true;
        } catch (Exception e) {
            this.inTransaction = false;
        }
    }

    @Override
    public void commitTransaction() {
        try {
            this.cacheWrapper.endTransaction(true);
        } finally {
            this.inTransaction = false;
        }
    }

    @Override
    public void rollbackTransaction() {
        try {
            this.cacheWrapper.endTransaction(false);
        } finally {
            this.inTransaction = false;
        }
    }

    @Override
    public boolean inTransaction() {
        return this.inTransaction;
    }

    // Adapted from LocalBenchmark.getCacheWrapper
    private CacheWrapper getCacheWrapper(String product) throws Exception {
        String fqnClass = Utils.getCacheWrapperFqnClass(product);

        URLClassLoader loader = Utils.buildProductSpecificClassLoader(product, getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        return (CacheWrapper) loader.loadClass(fqnClass).newInstance();
    }

}
