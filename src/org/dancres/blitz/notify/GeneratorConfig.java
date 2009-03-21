package org.dancres.blitz.notify;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.config.ConfigurationFactory;

import net.jini.security.ProxyPreparer;

/**
   Holds all EventGenerator configurable state
 */
class GeneratorConfig {
    private static final ProxyPreparer RECOVERY_PREPARER;
    private static final ProxyPreparer PREPARER;
    private static final int SAVE_INTERVAL;
    private static final int RESTART_JUMP;

    static {
        try {
            RECOVERY_PREPARER =
                ConfigurationFactory.getPreparer("recoveredNotifyPreparer");

            PREPARER =
                ConfigurationFactory.getPreparer("notifyPreparer");

            SAVE_INTERVAL = ((Integer)
                   ConfigurationFactory.getEntry("eventgenSaveInterval", 
                                                 int.class,
                                                 new Integer(50))).intValue();

            RESTART_JUMP = ((Integer)
                  ConfigurationFactory.getEntry("eventgenRestartJump", 
                                                int.class,
                                                new Integer(1000))).intValue();
        } catch (ConfigurationException aCE) {
            throw new RuntimeException("EvGen has problems with config",
                                       aCE);
        }
    }

    static ProxyPreparer getRecoveryPreparer() {
        return RECOVERY_PREPARER;
    }

    static ProxyPreparer getPreparer() {
        return PREPARER;
    }

    static int getSaveInterval() {
        return SAVE_INTERVAL;
    }

    static int getRestartJump() {
        return RESTART_JUMP;
    }
}