package org.dancres.blitz.notify;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import org.dancres.blitz.txn.TxnId;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.oid.OID;

/**
   Used to contain a Memento (see GOF) of an EventGenerator which can be
   saved to/loaded from disk.
 */
class EventGeneratorImplState implements EventGeneratorState {

    private OID theOID;
    private MangledEntry theTemplate;
    private MarshalledObject theHandback;
    private MarshalledObject theListener;
    private long theSourceId;
    private long theSeqNum;
    private long theLeaseTime;
    private TxnId theTxnId;

    EventGeneratorImplState(OID aOID, MangledEntry aTemplate,
                            MarshalledObject aHandback,
                            MarshalledObject aListener,
                            long aSourceId, long aSeqNum, long aLeaseTime,
                            TxnId anId) {
        theOID = aOID;
        theTemplate = aTemplate;
        theHandback = aHandback;
        theListener = aListener;
        theSourceId = aSourceId;
        theSeqNum = aSeqNum;
        theLeaseTime = aLeaseTime;
        theTxnId = anId;
    }

    public EventGenerator getGenerator() {
        return EventGeneratorImpl.restoreGenerator(this);
    }

    public boolean isPersistent() {
        return (theTxnId == null);
    }

    public OID getOID() {
        return theOID;
    }

    MangledEntry getTemplate() {
        return theTemplate;
    }

    MarshalledObject getHandback() {
        return theHandback;
    }

    MarshalledObject getListener() {
        return theListener;
    }

    long getSourceId() {
        return theSourceId;
    }

    long getSeqNum() {
        return theSeqNum;
    }

    long getLeaseTime() {
        return theLeaseTime;
    }
}
