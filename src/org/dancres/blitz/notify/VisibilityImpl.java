package org.dancres.blitz.notify;

import java.io.IOException;
import java.io.Serializable;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.UnknownEventException;

import net.jini.core.transaction.TransactionException;

import net.jini.space.AvailabilityEvent;
import net.jini.space.JavaSpace;

import org.dancres.blitz.txn.TxnId;
import org.dancres.blitz.txn.TxnManager;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.Types;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.task.Tasks;

/**
   <p> Represents a single registration via registerForVisbility.  If it's
   transactional, it will never be saved as it's considered temporary.  If
   it's transactional it will be saved to disk.  In addition, it's state will
   be saved to the log periodically (on a number of allocations basis) to
   preserve data in case of a crash. <p>

   <p> This class uses notifyPreparer to do initial preparation of the
   passed RemoteEventListener when constructed from scratch (i.e. it's a new
   registration).  It uses recoveredNotifyPreparer in those cases where it
   has been de-serialized from storage and is about to be used for the first
   time post a restart </p>

   <p> Instances of this object can be "tainted" for various reasons including
   certain kinds of exception from the remote notify implementation, lease
   expiry or transaction commit.  Tainting prevents any further operations
   against this instance such as event dispatch, logging changes to disk and
   lease renewal.  Tainting also results in cleanup for the registration being
   scheduled.</p>

   @todo Refactor the commonet stuff out of here and EventGeneratorImpl
 */
public class VisibilityImpl extends EventGeneratorBase {
    private MangledEntry[] theTemplates;
    private MarshalledObject theHandback;
    private MarshalledObject theMarshalledListener;
    private long theSourceId;
    private long theSeqNum;
    private long theLeaseTime;
    private long theStartSeqNum;
    private boolean doVisible;

    private int thePingCount;

    /**
       Indicates if we've done proxy preparation on the RemoteEventListener
     */
    private boolean isPrepared;

    /**
       Indicates when we're no longer generating events either because we
       couldn't get an appropriate response from the client or because
       we're being deleted.
     */
    private AtomicBoolean isTainted = new AtomicBoolean(false);

    private RemoteEventListener theListener;

    /*
      VisibilityImpls against a specific transaction are never saved so
      we make this transient to help in picking up code which hasn't
      taken account of this fact.....
    */
    private TxnId theTxnId;

    static VisibilityImpl restoreGenerator(VisibilityImplState aState) {
        return new VisibilityImpl(aState);
    }

    private VisibilityImpl(VisibilityImplState aState) {
        theOID = aState.getOID();
        theTemplates = aState.getTemplates();
        theHandback = aState.getHandback();
        theMarshalledListener = aState.getListener();
        theSourceId = aState.getSourceId();
        theSeqNum = aState.getSeqNum();
        theLeaseTime = aState.getLeaseTime();
        doVisible = aState.getJustVisible();
    }

    VisibilityImpl(MangledEntry[] aTemplates,
                   MarshalledObject anObject,
                   RemoteEventListener aDest, long aSeqNum,
                   long aLeaseTime, boolean justVisible)
        throws RemoteException {
        this(aTemplates, anObject, aDest, aSeqNum,
             aLeaseTime, null, justVisible);
    }

    VisibilityImpl(MangledEntry[] aTemplates,
                   MarshalledObject anObject,
                   RemoteEventListener aDest, long aSeqNum,
                   long aLeaseTime, TxnId aTxnId, boolean justVisible)
        throws RemoteException {

        theTemplates = aTemplates;
        theHandback = anObject;
        theSeqNum = aSeqNum;
        theStartSeqNum = aSeqNum;
        theTxnId = aTxnId;
        theLeaseTime = aLeaseTime;
        theListener = (RemoteEventListener)
            GeneratorConfig.getPreparer().prepareProxy(aDest);
        doVisible = justVisible;

        try {
            theMarshalledListener = new MarshalledObject(aDest);
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to marshall listener", anIOE);
        }

        isPrepared = true;
    }

    public void assign(OID anOID) {
        theOID = anOID;

        if (theTxnId == null)
            theSourceId = theOID.getId();
        else
            theSourceId = theTxnId.getId();
    }

    /**
       Return the first sequence number that this event generator ever
       produced.
     */
    public long getStartSeqNum() {
        return theStartSeqNum;
    }

    public boolean isPersistent() {
        return (theTxnId == null);
    }

    public long getSourceId() {
        return theSourceId;
    }

    /**
       Once an Visibility becomes tainted it will generate no more events
       and schedule cleanup.
     */
    public void taint() {
        if (!isTainted.compareAndSet(false, true))
            return;

        try {
            Tasks.queue(new CleanTask(getId()));
        } catch (InterruptedException anIE) {
            EventQueue.theLogger.log(Level.SEVERE,
                "Failed to lodge cleanup for: " + getId(), anIE);
        }
    }

    /* ********************************************************************
       Event filtering starts here
     **********************************************************************/

    /**
       Determines whether the passed QueueEvent is "visible" to this
       Visibility.  This is determind by checking transaction id's, expiry
       and that the generator isn't "tainted"
     */
    public boolean canSee(QueueEvent anEvent, long aTime) {
        if (isTainted.get())
            return false;

        synchronized(this) {
            if (aTime > theLeaseTime) {
                taint();
                return false;
            }

            // If it's a visibility event, it's the result of something
            // that was previously written and committed (and generated events
            // previously) which has now been marked taken and now is not
            // taken
            if ((anEvent.getType() == QueueEvent.ENTRY_VISIBLE) ||
                // Only post this one if user has said they're interested
                // in visible operations
                ((anEvent.getType() == QueueEvent.ENTRY_NOT_CONFLICTED) &&
                 (!doVisible)))
                return true;

            // If we're associated with a transaction.....
            if (theTxnId != null) {

                if ((anEvent.getType() == QueueEvent.TRANSACTION_ENDED) &&
                        (theTxnId.equals(anEvent.getTxn().getId()))) {
                    taint();
                    return false;
                }
                
                // We see all Written's including those generated by us
                if (anEvent.getType() == QueueEvent.ENTRY_WRITTEN)
                    return true;

                // If it's not a written we can only see it if we originated it
                return (anEvent.getTxn().getId().equals(theTxnId));
            } else {
                // Only see ENTRY_WRITTENS
                return (anEvent.getType() == QueueEvent.ENTRY_WRITTEN);
            }
        }
    }

    public boolean matches(MangledEntry anEntry) {
        for (int i = 0; i < theTemplates.length; i++) {
            MangledEntry myTemplate = theTemplates[i];

            if (Types.isSubtype(myTemplate.getType(), anEntry.getType())) {
                if (myTemplate.match(anEntry))
                    return true;
            }
        }
        
        return false;
    }

    /* ********************************************************************
       Lease management starts here
     **********************************************************************/

    public boolean renew(long aTime) {
        if (isTainted.get())
            return false;

        synchronized(this) {
            if (System.currentTimeMillis() > theLeaseTime)
                return false;

            theLeaseTime = aTime;
            return true;
        }
    }

    /* ********************************************************************
       Recovery starts here
     **********************************************************************/

    public void recover(long aSeqNum) {
        synchronized(this) {
            if (aSeqNum > theSeqNum) {
                theSeqNum = aSeqNum;
            }
        }
    }

    /**
       Jumps the sequence number by the RESTART_JUMP
     */
    public long jumpSequenceNumber() {
        synchronized(this) {
            theSeqNum += (GeneratorConfig.getRestartJump() +
                          GeneratorConfig.getSaveInterval());

            return theSeqNum;
        }
    }

    /**
       Jumps the sequence number if it's not already at this minimum.
     */
    public long jumpSequenceNumber(long aMin) {
        synchronized(this) {
            if (theSeqNum < aMin) {
                theSeqNum = aMin + GeneratorConfig.getSaveInterval();
            }

            return theSeqNum;
        }
    }

    /* ********************************************************************
       Event Generation starts here
     **********************************************************************/

    /**
       Dispatches a remote event to a client.
     */
    public void ping(QueueEvent anEvent, JavaSpace aSource) {
        SeqNumInterval mySnapshot = null;

        if (isTainted.get())
            return;

        synchronized(this) {
            RemoteEventListener myTarget = null;

            try {
                RemoteEvent myEvent =
                    newEvent(aSource, anEvent.getContext().getEntry(),
                             (anEvent.getType() != QueueEvent.ENTRY_NOT_CONFLICTED));

                myTarget = getDest();

                myTarget.notify(myEvent);

                mySnapshot = shouldLog();

            } catch (UnknownEventException aUEE) {
                RemoteEventDispatcher.theLogger.log(Level.SEVERE,
                                                    "Couldn't send event [Trash]" +
                                                    myTarget, aUEE);
                taint();
            } catch (NoSuchObjectException anNSOE) {
                RemoteEventDispatcher.theLogger.log(Level.SEVERE,
                                                    "Couldn't send event [Trash]" + 
                                                    myTarget, anNSOE);
                taint();
            } catch (RemoteException anRE) {
                RemoteEventDispatcher.theLogger.log(Level.SEVERE,
                                                    "Couldn't send event " +
                                                    myTarget, anRE);
            }
        }

        try {
            /*
             See recover() for details on why we can log this outside of the
             lock
            */
            if (mySnapshot != null)
                TxnManager.get().log(mySnapshot);
        } catch (TransactionException aTE) {
            RemoteEventDispatcher.theLogger.log(Level.SEVERE,
                "Couldn't update EventGenerator", aTE);
        }
    }

    private RemoteEvent newEvent(JavaSpace aSource, MangledEntry anEntry,
                                 boolean isVisible) {
        ++thePingCount;

        return new AvailabilityEventImpl(aSource, theSourceId, theSeqNum++,
                                         theHandback, anEntry, isVisible);
    }

    private RemoteEventListener getDest()
        throws RemoteException {

        // This flag is only set in the constructor or by us.
        if (!isPrepared) {
            try {
                Object myListener = theMarshalledListener.get();

                theListener = (RemoteEventListener)
                    GeneratorConfig.getRecoveryPreparer().prepareProxy(myListener);
                isPrepared = true;
            } catch (IOException anIOE) {
                throw new RemoteException("Failed to unmarshall listener",
                                          anIOE);
            } catch (ClassNotFoundException aCNFE) {
                throw new RemoteException("Failed to load class for listener",
                                          aCNFE);
            }
        }
        
        return theListener;
    }

    /* ********************************************************************
       State save/recovery starts here
     **********************************************************************/

    public EventGeneratorState getMemento() {
        synchronized(this) {
            return new VisibilityImplState(theOID, theTemplates,
                                           theHandback,
                                           theMarshalledListener,
                                           theSourceId,
                                           theSeqNum, theLeaseTime,
                                           theTxnId, doVisible);
        }
    }

    /**
     * Call this with synchronized asserted on the Generator
     */
    private SeqNumInterval shouldLog() {
        // Never log transactional notify registrations
        //
        if (theTxnId != null) {
            thePingCount = 0;
            return null;
        }

        if (thePingCount == GeneratorConfig.getSaveInterval()) {
            thePingCount = 0;
            return new SeqNumInterval(theOID, theSeqNum);
        } else
            return null;
    }
}
