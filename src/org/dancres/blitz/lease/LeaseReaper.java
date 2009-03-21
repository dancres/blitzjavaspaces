package org.dancres.blitz.lease;

import java.util.ArrayList;

import java.util.logging.*;

import net.jini.config.ConfigurationException;
import net.jini.config.Configuration;

import org.dancres.blitz.Logging;
import org.dancres.blitz.ActiveObject;
import org.dancres.blitz.ActiveObjectRegistry;

/**
   <p>Responsible for the co-ordination of cleanup of leased resources which
   have expired.  Any entity interested in performing such cleanup should
   implement <code>Reapable</code> and register with a LeaseReaper.</p>

   @see org.dancres.blitz.lease.Reapable
 */
public class LeaseReaper implements ActiveObject, Runnable {
    public static final long MANUAL_REAP = -1;

    private static final String MODULE_NAME = "org.dancres.blitz.lease";
    private static final String FILTER_PROPERTY = "filters";
    private static final String NAME_PROPERTY = "name";

    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.lease.LeaseReaper");

    private String theName;

    private long theReapTime;

    private Thread theReapThread;

    private ReapFilter theFilters;

    private ArrayList theReapables = new ArrayList();

    private boolean isExiting = false;

    /**
       <p>Accepts a configuration file similar to:</p>

       <pre>
       import org.dancres.blitz.lease.ReapFilter;

       import org.dancres.blitz.TxnReapFilter;

       org.dancres.blitz.lease {
           // DO NOT set this to null, use an empty array
           filters = new ReapFilter[] {new TxnReapFilter()};
           name = "EntryReaper";
       }
       </pre>

       <p>Which specifies the ReapFilters to install (can be a zero-length
       array or unspecified).</p>

       <p>Loads a set of ReapFilter instances from the configuration file 
       and aggregates those into a collection which implements ReapFilter.
       Whenever reaping is triggered, each Reapable will be passed this
       collection which it should use to ensure that it's appropriate
       to delete a resource.</p>

       @param aConfig configuration file as above
       @param aReapPeriod how often to wake up and perform a reap
     */
    public LeaseReaper(Configuration aConfig, long aReapPeriod)
        throws ConfigurationException {

        theName = (String) aConfig.getEntry(MODULE_NAME,
                                             NAME_PROPERTY,
                                             String.class, "TheReaper");

        ReapFilter[] myFilters =
            (ReapFilter[]) aConfig.getEntry(MODULE_NAME,
                                             FILTER_PROPERTY,
                                             ReapFilter[].class,
                                             new ReapFilter[0]);

        theFilters = new ReapFilterCollection(myFilters);

        theReapTime = aReapPeriod;

        theLogger.log(Level.INFO, "Reaper::" + theName + ":theReapTime: " +
                      theReapTime);

        for (int i = 0; i < myFilters.length; i++) {
            theLogger.log(Level.INFO, "Reaper::" + theName + ":Filter = " +
                          myFilters[i]);
        }
        
        /*
          If the reap time is greater than zero, it's not the MANUAL_REAP
          and therefore we need to be active.  If it's zero or MANUAL_REAP
          we don't want to do this.
        */
        if (theReapTime > 0) {
            theLogger.log(Level.SEVERE, "Active lease reaping enabled");
            ActiveObjectRegistry.add(this);
        } else if (theReapTime == MANUAL_REAP) {
            theLogger.log(Level.SEVERE, "Manual lease reaping enabled");
        }
    }

    /**
       Use this constructor to create a LeaseReaper purely by programmatical
       means with no reference to configuration files.  <b>Reaping is automatic
       or disabled, no manual option is supported</b>.
     */
    public LeaseReaper(String aName, ReapFilter[] aFilters,
                       long aReapPeriod)
        throws ConfigurationException {

        theName = aName;

        ReapFilter[] myFilters = (aFilters != null) ? aFilters :
            new ReapFilter[0];

        theFilters = new ReapFilterCollection(myFilters);

        theReapTime = aReapPeriod;

        theLogger.log(Level.INFO, "Reaper::" + theName + ":theReapTime: " +
                      theReapTime);

        for (int i = 0; i < myFilters.length; i++) {
            theLogger.log(Level.INFO, "Reaper::" + theName + ":Filter = " +
                          myFilters[i]);
        }
        
        if (theReapTime != 0)
            ActiveObjectRegistry.add(this);
    }

    public boolean isActive() {
        /*
          We want everyone to do book-keeping for both MANUAL_REAP and automatic
          reaping modes so if reapTime is non-zero we want people to do the
          necessary.
         */
        return (theReapTime != 0);
    }

    public void begin() {
        theReapThread = new Thread(this, "LeaseReaper:" + theName);
        theReapThread.start();
    }

    public void halt() {
        synchronized(this) {
            isExiting = true;
            notify();
        }

        try {
            theReapThread.join();
        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Couldn't wait for lease reaper: " +
                          theName, anIE);
        }
    }

    public void run() {
        theLogger.log(Level.INFO, "Reaper started up: " + theName);

        while (!isExiting) {
            try {
                synchronized(this) {
                    wait(theReapTime);
                }

                if (isExiting)
                    break;
                else
                    reapImpl();
            } catch (InterruptedException anIE) {
                break;
            }
        }

        theLogger.log(Level.INFO, "Reaper exited: " + theName);
    }

    public void add(Reapable aReapable) {
        synchronized(theReapables) {
            theReapables.add(aReapable);
        }
    }

    public boolean filter(LeasedResource anObject) {
        return theFilters.filter(anObject);
    }

    /**
       Request a manual reap
     */
    public void reap() {
        if (theReapTime != MANUAL_REAP) {
            theLogger.log(Level.SEVERE, "Manual reap requested but not configured in file.");
            return;
        }

        theLogger.log(Level.INFO, "Manual reap requested");
        reapImpl();
    }

    private void reapImpl() {
        Reapable[] myReapables;

        synchronized(theReapables) {
            myReapables = new Reapable[theReapables.size()];

            myReapables = (Reapable[]) theReapables.toArray(myReapables);
        }

        for (int i = 0; i < myReapables.length; i++) {
            try {
                myReapables[i].reap(theFilters);
            } catch (Throwable aT) {
                theLogger.log(Level.SEVERE,
                              "Reaper encountered exception: " + theName, aT);
            }
        }
    }

    private static class ReapFilterCollection implements ReapFilter {
        private ReapFilter[] theFilters;

        ReapFilterCollection(ReapFilter[] aFilters) {
            theFilters = aFilters;
        }

        public boolean filter(LeasedResource anObject) {
            for (int i = 0; i < theFilters.length; i++) {
                if (theFilters[i].filter(anObject))
                    return true;
            }

            return false;
        }
    }
}
