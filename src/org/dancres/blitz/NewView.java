package org.dancres.blitz;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.entry.*;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.oid.OID;
import org.dancres.blitz.notify.EventGenerator;
import org.dancres.blitz.notify.EventQueue;
import org.dancres.blitz.notify.QueueEvent;
import org.dancres.blitz.notify.EventGeneratorState;
import org.dancres.struct.LinkedInstance;

/**
   <p>Reponsible for dispatching the flow of new writes into the UIDSet.</p>
 */
class NewView {
    private static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.NewView");

    private EventGeneratorImpl theEventGenerator;

    private MangledEntry[] theTemplates;
    private UIDSet theUIDs;

    private EntryViewImpl theView;
    
    NewView(EntryViewImpl aView, MangledEntry[] aTemplates, UIDSet aSet) {
        theTemplates = aTemplates;
        theUIDs = aSet;
        theEventGenerator = new EventGeneratorImpl();
        theView = aView;
    }

    EventGenerator getSearchTask() {
        return theEventGenerator;
    }

    /**
       @return <code>true</code> if this Visitor wishes to perform a take.
     */
    public boolean isDeleter() {
        return false;
    }

    /************************************************************************
     *  View stuff
     ************************************************************************/

    private boolean atCapacity() {
        /*
            If we're in merge mode, theMergeTuples will tell us how many
            Entry's we currently have accrued.  Otherwise everything is added
            to theTuples.
        */
        return (theUIDs.isFull());
    }

    private class EventGeneratorImpl implements EventGenerator {
        private boolean isTainted = false;
        private OID theOID;

        EventGeneratorImpl() {
        }

        public void assign(OID anOID) {
            theOID = anOID;
        }

        public long getStartSeqNum() {
            return 0;
        }

        public OID getId() {
            return theOID;
        }

        public boolean isPersistent() {
            return false;
        }

        public long getSourceId() {
            return 0;
        }

        void taint(boolean signal) {

            /*
            try {
                Tasks.queue(new CleanTask(getId()));
            } catch (InterruptedException anIE) {
                theLogger.log(Level.WARNING,
                    "Failed to lodge cleanup for: " + getId(), anIE);
            }
            */
        }

        public void taint() {
            synchronized (this) {
                // Tainting can only be done once
                //
                if (isTainted)
                    return;

                isTainted = true;
            }

            try {
                EventQueue.get().kill(getId());
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE,
                    "Encountered IOException during kill", anIOE);
            }
        }

        public boolean canSee(QueueEvent anEvent, long aTime) {
            synchronized (this) {
                if (isTainted) {
                    return false;
                }
            }

            // Check if it's txn_ended and my txn and call resolved if it is
            if ((anEvent.getType() == QueueEvent.TRANSACTION_ENDED) &&
                    (theView.getTxn().getId().equals(anEvent.getTxn().getId()))) {
                theView.resolved();
                return false;
            }
            
            // We want to see new writes from a transaction
            //
            return (anEvent.getType() == QueueEvent.ENTRY_WRITE);
        }

        public boolean matches(MangledEntry anEntry) {
            synchronized (this) {
                if (isTainted) {
                    return false;
                }
            }

            for (int i = 0; i < theTemplates.length; i++) {
                MangledEntry myTemplate = theTemplates[i];

                if (Types.isSubtype(myTemplate.getType(), anEntry.getType())) {
                    if (myTemplate.match(anEntry)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean renew(long aTime) {
            // Nothing to do as we expire by being tainted by the enclosing
            // class only
            //
            return true;
        }

        public void recover(long aSeqNum) {
            // Nothing to do
        }

        public long jumpSequenceNumber() {
            return 0;
        }

        public long jumpSequenceNumber(long aMin) {
            return 0;
        }

        public void ping(QueueEvent anEvent, JavaSpace aSource) {
            synchronized (this) {
                if (isTainted) {
                    return;
                }
            }

            if (atCapacity()) {
                taint();
                return;
            }

            QueueEvent.Context myContext = anEvent.getContext();

            SpaceEntryUID myUID =
                new SpaceEntryUID(myContext.getEntry().getType(),
                    myContext.getOID());

            theUIDs.add(myUID);
        }

        public EventGeneratorState getMemento() {
            throw new RuntimeException(
                "Shouldn't be happening - we're transient");
        }
    }
}