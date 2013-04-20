package org.dancres.blitz.notify;

import net.jini.space.JavaSpace;

import org.dancres.blitz.txn.TxnId;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.oid.OID;

/**
   <p> Represents a single registration via notify.  If it's transactional,
   it will never be saved as it's considered temporary.  If it's transactional
   it will be saved to disk.  In addition, it's state will be saved to the
   log periodically (on a number of allocations basis) to preserve data
   in case of a crash. <p>

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
 */
public interface EventGenerator extends Comparable {
    public void assign(OID anOID);
    
    public long getStartSeqNum();

    public OID getId();

    public boolean isPersistent();
    
    public long getSourceId();

    /**
       Once an EventGenerator becomes tainted it will generate no more events
       and schedule cleanup.
     */
    public void taint();

    /* ********************************************************************
       Event filtering starts here
     **********************************************************************/

    /**
       Determines whether the passed QueueEvent is "visible" to this
       EventGenerator.  This is determind by checking transaction id's, expiry
       and that the generator isn't "tainted"
     */
    public boolean canSee(QueueEvent anEvent, long aTime);

    /**
     * @return a handback to use in the send event or null indicating no match
     */
    public boolean matches(MangledEntry anEntry);

    /* ********************************************************************
       Lease management starts here
     **********************************************************************/

    public boolean renew(long aTime);

    /* ********************************************************************
       Recovery starts here
     **********************************************************************/

    public void recover(long aSeqNum);

    /**
       Jumps the sequence number by the RESTART_JUMP
     */
    public long jumpSequenceNumber();

    /**
       Jumps the sequence number if it's not already at this minimum.
     */
    public long jumpSequenceNumber(long aMin);


    /* ********************************************************************
       Event Generation starts here
     **********************************************************************/

    /**
       Dispatches a remote event to a client using the passed source.
     */
    public void ping(QueueEvent anEvent, JavaSpace aSource);

    /* ********************************************************************
       State save/recovery starts here
     **********************************************************************/

    public EventGeneratorState getMemento();
}
