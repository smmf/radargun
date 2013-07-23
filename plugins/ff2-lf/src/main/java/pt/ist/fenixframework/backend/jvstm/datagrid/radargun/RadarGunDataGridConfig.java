package pt.ist.fenixframework.backend.jvstm.datagrid.radargun;

import java.util.Properties;

import org.radargun.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.backend.BackEndId;
import pt.ist.fenixframework.backend.jvstm.lf.JvstmLockFreeConfig;

/**
 * This is an extension to the configuration manager used by the fenix-framework-backend-jvstm-datagrid backend. It adds specific
 * config properties for using any Radargun plugin.
 * 
 * @see Config
 */
public class RadarGunDataGridConfig extends JvstmLockFreeConfig {

    private static final Logger logger = LoggerFactory.getLogger(RadarGunDataGridConfig.class);

    private static final String RADAR_GUN_DATA_GRID_DECORATOR_CLASS_NAME = DataGridDecorator.class.getName();

    private final String config;
    private final boolean isLocal;
    private final int nodeIndex;
    private final TypedProperties confAttributes;

    public RadarGunDataGridConfig(String config, boolean isLocal, int nodeIndex, TypedProperties confAttributes) {
        appNameFromString(BackEndId.getBackEndId().getAppName());
        this.config = config;
        this.isLocal = isLocal;
        this.nodeIndex = nodeIndex;
        this.confAttributes = confAttributes;
    }

    public String getConfig() {
        return config;
    }

    public boolean getIsLocal() {
        return isLocal;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public TypedProperties getConfAttributes() {
        return confAttributes;
    }

    public void setExtraProperties(Properties props) {
        populate(props);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void checkConfig() {
        if (super.dataGridClassName == null) {
            super.dataGridClassName = RADAR_GUN_DATA_GRID_DECORATOR_CLASS_NAME;
        }

        checkRequired(this.config, "config");
        checkRequired(this.isLocal, "isLocal");
        checkRequired(this.nodeIndex, "nodeIndex");
        checkRequired(this.confAttributes, "confAttributes");
        super.checkConfig();
    }

}