package org.dancres.blitz.lease;

import java.util.logging.*;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Logging;

import org.dancres.blitz.util.Time;

import org.dancres.blitz.config.ConfigurationFactory;

/**
   Responsible for handling bounding of lease durations based on configuration
   items <code>notifyLeaseBound</code> and <code>entryLeaseBound</code>
 */
public class LeaseBounds {
    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.lease.LeaseBounds");

    private static long theNotifyBound;
    private static long theEntryBound;
    private static long theViewBound;
    private static long theTxnBound;

    static {
        try {
            theNotifyBound =
                ((Long) ConfigurationFactory.getEntry("notifyLeaseBound",
                                                      long.class,
                                                      new Long(0))).longValue();
            theEntryBound =
                ((Long) ConfigurationFactory.getEntry("entryLeaseBound",
                                                      long.class,
                                                      new Long(0))).longValue();
            theViewBound =
                ((Long) ConfigurationFactory.getEntry("viewLeaseBound",
                                                      long.class,
                                                      new Long(0))).longValue();
            theTxnBound =
                ((Long) ConfigurationFactory.getEntry("loopbackTxnLeaseBound",
                    long.class,
                    new Long(0))).longValue();

            theLogger.log(Level.INFO, "LeaseBounds: " + theNotifyBound +
                          ", " + theEntryBound + ", " + theViewBound + ", " +
                          theTxnBound);

        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE, "Got config problem", aCE);
        }
    }

    public static long boundWrite(long aLeaseDuration) {
        return Time.getLeaseDuration(aLeaseDuration, theEntryBound);
    }

    public static long boundNotify(long aLeaseDuration) {
        return Time.getLeaseDuration(aLeaseDuration, theNotifyBound);
    }

    public static long boundView(long aLeaseDuration) {
        return Time.getLeaseDuration(aLeaseDuration, theViewBound);
    }

    public static long boundTxn(long aLeaseDuration) {
        return Time.getLeaseDuration(aLeaseDuration, theTxnBound);
    }
}
