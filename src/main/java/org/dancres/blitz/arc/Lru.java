package org.dancres.blitz.arc;

import java.io.IOException;

import java.util.logging.*;

import org.dancres.struct.LinkedInstances;

class Lru {
    static final int T1 = 0;
    static final int T2 = 1;
    static final int B1 = 2;
    static final int B2 = 3;

    private static final String[] NAMES = {"T1", "T2", "B1", "B2"};

    private LinkedInstances theEntries = new LinkedInstances();

    private int theId;

    Lru(int anId) {
        theId = anId;
    }

    synchronized void remove(CacheBlockDescriptor aDesc) {
        if (aDesc.getWhere() != theId)
            throw new RuntimeException("I don't own this descriptor");

        theEntries.remove(aDesc);
    }

    synchronized void mruInsert(CacheBlockDescriptor aDesc) {
        theEntries.insert(aDesc);
        aDesc.setWhere(theId);
    }

    synchronized void lruInsert(CacheBlockDescriptor aDesc) {
        theEntries.add(aDesc);
        aDesc.setWhere(theId);
    }

    synchronized CacheBlockDescriptor lruRemove() {
        return (CacheBlockDescriptor) theEntries.removeLast();
    }

    synchronized int length() {
        return theEntries.getSize();
    }

    synchronized void dump() {
        Logger myLogger = ArcCache.theLogger;

        myLogger.log(Level.FINE, "AC: " + NAMES[theId]);

        CacheBlockDescriptor myCBD = 
            (CacheBlockDescriptor) theEntries.getHead();

        while (myCBD != null) {

            try {
                myCBD.acquire();

                myLogger.log(Level.FINE, myCBD.getId() + ", " +
                             myCBD.getId().hashCode() + ", ");

                myCBD.release();
            } catch (InterruptedException anIE) {
                myLogger.log(Level.FINE, "Interrupted locking CBD");
            }

            myCBD = (CacheBlockDescriptor) myCBD.getNext();
        }
    }

    synchronized void sync(BackingStore aStore) throws IOException {
        CacheBlockDescriptor myCBD = 
            (CacheBlockDescriptor) theEntries.getHead();

        while (myCBD != null) {

            try {
                myCBD.acquire();

                aStore.save(myCBD.getContent());

                myCBD.release();
            } catch (InterruptedException anIE) {
                throw new IOException();
            }

            myCBD = (CacheBlockDescriptor) myCBD.getNext();
        }
    }
}
