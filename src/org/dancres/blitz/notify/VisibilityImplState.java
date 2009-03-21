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
class VisibilityImplState implements EventGeneratorState {

    private OID theOID;
    private MangledEntry[] theTemplates;
    private MarshalledObject theHandback;
    private MarshalledObject theListener;
    private long theSourceId;
    private long theSeqNum;
    private long theLeaseTime;
    private TxnId theTxnId;
    private boolean doVisible;

    VisibilityImplState(OID aOID, MangledEntry[] aTemplates,
                        MarshalledObject aHandback,
                        MarshalledObject aListener,
                        long aSourceId, long aSeqNum, long aLeaseTime,
                        TxnId anId, boolean justVisible) {
        theOID = aOID;
        theTemplates = aTemplates;
        theHandback = aHandback;
        theListener = aListener;
        theSourceId = aSourceId;
        theSeqNum = aSeqNum;
        theLeaseTime = aLeaseTime;
        theTxnId = anId;
        doVisible = justVisible;
    }

    public EventGenerator getGenerator() {
        return VisibilityImpl.restoreGenerator(this);
    }

    public boolean isPersistent() {
        return (theTxnId == null);
    }

    public OID getOID() {
        return theOID;
    }

    boolean getJustVisible() {
        return doVisible;
    }

    MangledEntry[] getTemplates() {
        return theTemplates;
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
