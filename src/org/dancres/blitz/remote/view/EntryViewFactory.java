package org.dancres.blitz.remote.view;

import java.io.IOException;

import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

import net.jini.config.ConfigurationException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.*;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.lease.LeaseReaper;
import org.dancres.blitz.lease.Reapable;
import org.dancres.blitz.lease.ReapFilter;

import org.dancres.blitz.util.Time;

import org.dancres.blitz.config.ConfigurationFactory;

/**
   Manages and tracks current <code>EntryView</code> instances instantiated
   from a <code>SpaceImpl</code> instance.  In particular it assigns those
   instances a unique id and handles leasing aspects.
 */
public class EntryViewFactory implements Reapable {
    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote.view.EntryViewFactory");

    static class LifecycleImpl implements Lifecycle {
        public void init() {
            theFactory = new EntryViewFactory();
        }

        public void deinit() {
            theFactory = null;
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }    

    private static EntryViewFactory theFactory;

    public static EntryViewFactory get() {
        return theFactory;
    }

    private HashMap theActiveViews = new HashMap();

    private LeaseReaper theReaper;

    private boolean shouldUpdate;

    private EntryViewFactory() {
        try {
            long myReapInterval =
                ((Long) ConfigurationFactory.getEntry("viewReapInterval",
                                                      long.class,
                                                      new Long(30 * 60 * 1000))).longValue();
            theReaper = new LeaseReaper("View", null, myReapInterval);
            
            theReaper.add(this);

            shouldUpdate = ((Boolean)
                 ConfigurationFactory.getEntry("updateContents",
                                               boolean.class,
                                               new Boolean(true))).booleanValue();

        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE, "Failed to load config", aCE);
        }
    }

    public ViewRegistration newView(MangledEntry[] aTemplates, Transaction aTxn,
                                    boolean holdLocks, long aLeaseDuration,
                                    long aLimit, SpaceImpl aSpace)
        throws IOException, TransactionException {

        // Hold locks should only stick for a non-null transaction, other
        // wise we're just testing....
        
        EntryView myView = aSpace.getView(aTemplates, aTxn, holdLocks,
                shouldUpdate, aLimit);

        EntryViewUID myUid = new EntryViewUID(UuidFactory.generate());

        long myExpiry = Time.getAbsoluteTime(aLeaseDuration);

        synchronized(this) {
            theActiveViews.put(myUid, new ViewHolder(myView, myExpiry));
        }

        return new ViewRegistration(myUid, myExpiry);
    }

    public EntryView getView(EntryViewUID aUID) {

        synchronized(this) {
            ViewHolder myHolder = (ViewHolder) theActiveViews.get(aUID);

            if ((myHolder == null) || 
                (myHolder.hasExpired(System.currentTimeMillis()))) {

                return null;
            } else {
                return myHolder.getView();
            }
        }
    }

    public EntryView delete(EntryViewUID aUID) {

        synchronized(this) {
            ViewHolder myHolder = (ViewHolder) theActiveViews.remove(aUID);

            if (myHolder != null) {
                myHolder.getView().close();
                return myHolder.getView();
            }

            return null;
        }
    }

    /**
       @todo Fix up the missing synchronization inside the loop (should be around the get and expire test)
     */
    public void reap(ReapFilter aFilter) {
        /*
          No reap filters will be configured so we can ignore those - see
          initialization in constructor
         */
        long myTime = System.currentTimeMillis();

        Object[] myKeys;

        synchronized(this) {
            myKeys = theActiveViews.keySet().toArray();
        }

        for (int i = 0; i < myKeys.length; i++) {
            ViewHolder myHolder =
                (ViewHolder) theActiveViews.get(myKeys[i]);

            if (myHolder.hasExpired(myTime)) {
                delete((EntryViewUID) myKeys[i]);
            }
        }
    }

    /* ***********************************************************************
     * Lease renewal/cancel
     * ***********************************************************************/

    boolean renew(EntryViewUID aUID, long anExpiry) {
        synchronized(this) {
            ViewHolder myHolder = (ViewHolder) theActiveViews.get(aUID);

            if (myHolder != null) {
                return myHolder.testAndSetExpiry(System.currentTimeMillis(),
                                                 anExpiry);
            }

            return false;
        }
    }

    boolean cancel(EntryViewUID aUID) {
        return (delete(aUID) != null);
    }

    class ViewHolder {
        private long theExpiry;
        private EntryView theView;

        ViewHolder(EntryView aView, long anExpiry) {
            theView = aView;
            theExpiry = anExpiry;
        }

        boolean hasExpired(long aTime) {
            return (theExpiry < aTime);
        }

        boolean testAndSetExpiry(long aTime, long anExpiry) {
            if (theExpiry < aTime)
                return false;

            theExpiry = anExpiry;

            return true;
        }

        EntryView getView() {
            return theView;
        }
    }
}