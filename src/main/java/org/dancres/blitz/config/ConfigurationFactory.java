package org.dancres.blitz.config;

import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.ConfigurationException;

import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;

import com.sun.jini.config.Config;

import org.dancres.blitz.Logging;

public class ConfigurationFactory {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.config.ConfigurationFactory",
                          Level.INFO);

    private static String[] theArgs = {"config/blitz.config"};
    
    static {
        String myDefault = System.getProperty("org.dancres.blitz.config");
        
        if (myDefault != null)
            theArgs = new String[] {myDefault};
    }
    
    private static Configuration theConfig;

    public static final String BLITZ_MODULE = "org.dancres.blitz";

    private static final ProxyPreparer DEFAULT_PREPARER =
        new BasicProxyPreparer();

    /**
       Configure the arguments for finding a Configuration
     */
    public static void setup(String[] anArgs) {
        theLogger.log(Level.INFO,
                      "ConfigurationFactory will load config from: " +
                      anArgs[0]);
        theArgs = anArgs;
    }

    /**
       Attempt to load configured config file or the default which is
       "config/blitz.config"
     */
    private static synchronized void load() throws ConfigurationException {
        theLogger.log(Level.INFO, "Loading config from: " + theArgs[0]);

        theConfig =
            ConfigurationProvider.getInstance(theArgs,
                                              ConfigurationFactory.class.getClassLoader());
    }

    /**
       Attempt to obtain the configuration
     */
    public static synchronized Configuration getConfig() 
        throws ConfigurationException {
        if (theConfig == null) {
            load();
        }

        return theConfig;
    }

    public static synchronized ProxyPreparer getPreparer(String aName)
        throws ConfigurationException {

        return (ProxyPreparer)
            Config.getNonNullEntry(getConfig(), BLITZ_MODULE, aName,
                                   ProxyPreparer.class, DEFAULT_PREPARER);
    }

    public static synchronized Object getEntry(String aName, Class aType)
        throws ConfigurationException {
        return getConfig().getEntry(BLITZ_MODULE, aName, aType);
    }

    public static synchronized Object getEntry(String aName, Class aType,
                                               Object aDefault)
        throws ConfigurationException {

        return getConfig().getEntry(BLITZ_MODULE, aName, aType, aDefault);
    }

    public static synchronized Object getEntry(String aName, Class aType,
                                               Object aDefault, Object aData)
        throws ConfigurationException {
        return getConfig().getEntry(BLITZ_MODULE, aName, aType,
                                    aDefault, aData);
    }
}
