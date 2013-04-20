package org.dancres.blitz.notify;

import java.io.Serializable;

import org.dancres.blitz.txn.TxnId;

import org.dancres.blitz.oid.OID;

/**
   Used to contain a Memento (see GOF) of an EventGenerator which can be
   saved to/loaded from disk.  This is basically an opaque type with one
   method which can be used to restore an EventGenerator from the state.
 */
public interface EventGeneratorState extends Serializable {
    public EventGenerator getGenerator();
    public boolean isPersistent();
    public OID getOID();
}
