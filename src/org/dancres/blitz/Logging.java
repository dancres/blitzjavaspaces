package org.dancres.blitz;

import java.io.IOException;

import java.util.logging.*;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.config.ConfigurationFactory;


/**
   The source for logger instances.  This allows centralized control of
   default logging level etc.
 */
public class Logging {
    private static Handler theConsole = new ConsoleHandler();
    // private static Handler theConsole;

    static {
        // try {
        //theConsole = new FileHandler("/tmp/blitz.log");
        theConsole.setLevel(Level.ALL);
        //} catch (IOException anIOE) {
        // }
    }

    public static Logger newLogger(String aName) {
        Logger myLogger = Logger.getLogger(aName);

        try {
            /*
              Allow user to override configuration of logging levels
              from Blitz.config file
             */
            Boolean myDisableConfig = (Boolean)
                ConfigurationFactory.getEntry("ignoreLogConfig", Boolean.class,
                                              new Boolean(false));

            if (myDisableConfig.booleanValue()) {
                return myLogger;
            }

            Level myDefaultLevel = (Level)
                ConfigurationFactory.getEntry("defaultLogLevel", Level.class,
                                              Level.WARNING);


            String myLevelName = aName + "LogLevel";
            myLevelName = myLevelName.replaceAll("\\.", "_");

            // System.out.println("Looking for: " + myLevelName);

            Level myLevel = (Level)
                ConfigurationFactory.getEntry(myLevelName, Level.class,
                                              myDefaultLevel);

            // System.out.println("Logger: " + aName + ": " + myLevel);
            myLogger.setLevel(myLevel);

            // If we've got a default logger it'll be sending to parent
            // handlers
            if (myLogger.getUseParentHandlers()) {
                myLogger.setUseParentHandlers(false);
                myLogger.addHandler(theConsole);
            }

        } catch (ConfigurationException aCE) {
            System.err.println("Error reading logging config");
            aCE.printStackTrace(System.err);
        }

        return myLogger;
    }

    public static Logger newLogger(String aName, Level aLevel) {
        Logger myLogger = Logger.getLogger(aName);

        myLogger.setLevel(aLevel);

        return myLogger;
    }
}
