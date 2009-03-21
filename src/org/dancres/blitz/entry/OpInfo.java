package org.dancres.blitz.entry;

import java.io.Serializable;
import java.io.IOException;

import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.mangler.MangledEntry;

/**
   Every operation performed on the disk layer results in an OpInfo being
   returned which will contain details of that operation which can be:

   <OL>
   <LI> Logged to disk for recovery purposes either individually or as part
   of a transaction record.  In these cases, one recovers the OpInfo and
   invokes <code>restore</code> followed by either <code>commit</code> or
   <code>abort</code>. </LI>.
   <LI> Used to finalize an operation either immediately in the case of a
   null transaction or later as part of a full external transaction </LI>
   </OL>

   Basic interaction with write is to invoke on the WriteBuddy just before
   we place the EntrySleeve in the cache so it can set a write lock.  We
   then return the OpInfo which may be put into the list of a transactions
   operations or we commit it and then release the transaction lock asserted
   by the WriteBuddy for null transactions <P>

   Basic interaction for a read or take is to iterate through EntrySleeves
   pinning, offering to buddy and un-pinning.  On success from buddy (we
   keep a pin applied) knowing a transaction lock has been asserted.  We then
   return the OpInfo which will, again, either be commited immediately or
   stored for a later commit. <P>

   Note that, because underlying implementations always pin their respective
   entries in a cache, the Entry will be in cache and will be updated in
   cache (because it can't be flushed).  This means that restore can throw
   a IOException as it reloads state but neither of commit or abort can.
*/
public interface OpInfo extends Serializable {

    /**
       @return <code>true</code> if this operation is a debug operation with
       no state that needs preserving
     */
    public boolean isDebugOp();

    /**
       Restore memory-state required to apply this OpInfo.
     */
    public void restore() throws IOException;

    /**
       Apply the action implied by this OpInfo to the underlying FS.
       
       @return the entry associated with the UID of this OpInfo if it
       still exists (is not deleted).
     */
    public MangledEntry commit(TxnState aState) throws IOException;

    /**
       Abort the action implied by this OpInfo to the underlying FS.
       
       @return the entry associated with the UID of this OpInfo if it
       still exists (is not deleted).
     */
    public MangledEntry abort(TxnState aState) throws IOException;

    public String getType();

    public OID getOID();
}
