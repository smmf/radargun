package org.radargun.cachewrappers;

import java.util.concurrent.ConcurrentHashMap;

import jvstm.Transaction;
import jvstm.VBox;

import org.radargun.CacheWrapper;
import org.radargun.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jvstm1Wrapper implements CacheWrapper {

    private static final Logger logger = LoggerFactory.getLogger(Jvstm1Wrapper.class);

    private static final ConcurrentHashMap<Object, VBox> cache = new ConcurrentHashMap<Object, VBox>();

    private static VBox cacheLookup(Object vboxId) {
        VBox vbox = cache.get(vboxId);

        if (vbox == null) {
            vbox = new VBox();
            VBox vboxInCache = cache.putIfAbsent(vboxId, vbox);
            if (vboxInCache != null) {
                vbox = vboxInCache;
            }
        }
        return vbox;
    }

    @Override
    public void setUp(String config, boolean isLocal, int nodeIndex, TypedProperties confAttributes) throws Exception {
    }

    @Override
    public void tearDown() throws Exception {
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void put(String bucket, Object key, Object value) throws Exception {
        cacheLookup(key).put(value);
    }

    @Override
    public Object get(String bucket, Object key) throws Exception {
        return cacheLookup(key).get();
    }

    @Override
    public void empty() throws Exception {
        cache.clear();
    }

    @Override
    public Object remove(String bucket, Object key) throws Exception {
        VBox vbox = cacheLookup(key);
        Object previousValue = vbox.get();
        vbox.put(null);
        return previousValue;
    }

    @Override
    public String getInfo() {
        return "JVSTM lock-based commit";
    }

    @Override
    public int getLocalSize() {
        return cache.size();
    }

    @Override
    public int getNumMembers() {
        return 1;
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
        Transaction.begin();
    }

    @Override
    public void endTransaction(boolean successful) {
        if (successful) {
            Transaction.commit();
        } else {
            Transaction.abort();
        }
    }

    @Override
    public int getTotalSize() {
        return cache.size();
    }

}
