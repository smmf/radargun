package org.radargun.cachewrappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.radargun.CacheWrapper;
import org.radargun.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.backend.jvstm.datagrid.radargun.DataGridDecorator;
import pt.ist.fenixframework.backend.jvstm.datagrid.radargun.RadarGunDataGridConfig;
import pt.ist.fenixframework.backend.jvstm.lf.JvstmLockFreeBackEnd;

public class FFWrapperLF implements CacheWrapper {

    private static final Logger logger = LoggerFactory.getLogger(FFWrapperLF.class);

    public static final String FF_CONFIG_CLASS = "pt.ist.fenixframework.backend.jvstm.datagrid.radargun.RadarGunDataGridConfig";
    public static final String DATA_GRID_CONFIG_FILE = "dataGridConfigFile";

    public static final String FF_CONFIG_OPTIONS_FILE = "rg-fenix-framework-config-options.properties";

    protected static JvstmLockFreeBackEnd backEnd;

    private String info;

    private static Properties loadFFConfigOptions() {
        Properties props = new Properties();

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(FF_CONFIG_OPTIONS_FILE);
        if (in == null) {
            logger.info("Resource '" + FF_CONFIG_OPTIONS_FILE + "' not found");
            return null;
        }

        logger.info("Found fenix framework config options in '" + FF_CONFIG_OPTIONS_FILE + "'.");

        // add the new properties
        try {
            props.load(in);
        } catch (IOException e) {
            logger.warn("Error reading " + FF_CONFIG_OPTIONS_FILE, e);
            return null;
        } finally {
            try {
                in.close();
            } catch (Throwable ignore) {
            }
        }
        return props;
    }

    @Override
    public void setUp(String config, boolean isLocal, int nodeIndex, TypedProperties confAttributes) throws Exception {
        logger.info("config={}, isLocal={}, nodeIndex={}, confAttributes={}", config, isLocal, nodeIndex, confAttributes);
        this.info = confAttributes.getProperty(DataGridDecorator.DECORATE_KEY);

        RadarGunDataGridConfig ffConfig = new RadarGunDataGridConfig(config, isLocal, nodeIndex, confAttributes);

        Properties ffProperties = loadFFConfigOptions();
        if (ffProperties != null) {
            logger.info("Setting extra config properties for Fenix Framework.");
            ffConfig.setExtraProperties(ffProperties);
        }
        logger.info("Will initialize Fenix Framework with RadarGunDataGridConfig");
        FenixFramework.initialize(ffConfig);

        logger.info("FF STARTED=" + FenixFramework.isInitialized());
        backEnd = JvstmLockFreeBackEnd.getInstance();
    }

    @Override
    public void tearDown() throws Exception {
        FenixFramework.shutdown();
    }

    @Override
    public boolean isRunning() {
        return FenixFramework.isInitialized();
    }

    @Override
    public void put(String bucket, Object key, Object value) throws Exception {
        logger.debug("PUT: key={}, value={}", key, value);
        boolean autoCommit = false;

        if (FenixFramework.getTransaction() == null) {
            startTransaction();
            autoCommit = true;
        }

        try {
            backEnd.vboxFromId(key.toString()).put(value);
        } finally {
            if (autoCommit) {
                endTransaction(true);
            }
        }
    }

    @Override
    public Object get(String bucket, Object key) throws Exception {
        logger.debug("GET: key={}", key);
        boolean autoCommit = false;

        if (FenixFramework.getTransaction() == null) {
            startTransaction();
            autoCommit = true;
        }

        try {
            return backEnd.vboxFromId(key.toString()).get();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null; // the compiler requires it
        } finally {
            if (autoCommit) {
                endTransaction(true);
            }
        }
    }

    @Override
    public Object remove(String bucket, Object key) throws Exception {
        logger.debug("REMOVE: key={}", key);
        boolean autoCommit = false;

        if (FenixFramework.getTransaction() == null) {
            startTransaction();
            autoCommit = true;
        }

        try {
            Object previous = get(bucket, key);
            put(bucket, key, null);
            return previous;
        } finally {
            if (autoCommit) {
                endTransaction(true);
            }
        }
    }

    @Override
    public void empty() throws Exception {
        logger.warn("Fenix Framework never forgets...");
//        logger.debug("GET: EMPTY");
//        boolean autoCommit = false;
//
//        if (FenixFramework.getTransaction() == null) {
//            startTransaction();
//            autoCommit = true;
//        }
//
//        try {
//            backend.deleteAll();
//            // TODO Auto-generated method stub
//            throw new UnsupportedOperationException(
//                    "not yet implemented. should remove/clear all elements from the underlying cache");
//        } finally {
//            if (autoCommit) {
//                endTransaction(true);
//            }
//        }
    }

    @Override
    public int getNumMembers() {
        return backEnd.getNumMembers();
    }

    @Override
    public String getInfo() {
        return "FFWrapperLF using " + this.info;
    }

    @Override
    public Object getReplicatedData(String bucket, String key) throws Exception {
        return get(bucket, key);
    }

    @Override
    public boolean isTransactional(String bucket) {
        return true;
    }

    @Override
    public void startTransaction() {
        try {
            FenixFramework.getTransactionManager().begin();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void endTransaction(boolean successful) {
        try {
            if (successful) {
                FenixFramework.getTransactionManager().commit();
            } else {
                FenixFramework.getTransactionManager().rollback();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getLocalSize() {
        return -1;
    }

    @Override
    public int getTotalSize() {
        return -1;
    }

}