package org.dancres.blitz.arc;

import java.util.logging.Level;

import java.io.Serializable;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

import org.dancres.struct.LinkedInstance;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;
import org.dancres.blitz.cache.CacheListenerSet;

/**
   Each object currently in cache is referenced via a CacbeBlockDescriptor
 */
public class CacheBlockDescriptor implements LinkedInstance {
    private Identifier theIdentifier;
    private Identifiable theContent;

    private LinkedInstance thePrev;
    private LinkedInstance theNext;

    private int isWhere;

    private Mutex theLock = new Mutex();

    CacheBlockDescriptor() {
    }

    void acquire() throws InterruptedException {
        theLock.acquire();
    }

    public void release() {
        theLock.release();
    }

    public void setId(Identifier anIdentifier) {
        theIdentifier = anIdentifier;
    }

    public Identifier getId() {
        return theIdentifier;
    }

    public Identifiable getContent() {
        return theContent;
    }
 
    void setContent(Identifiable aContent) {
        theContent = aContent;
    }

    public void setNext(LinkedInstance aLinkedInstance) {
        theNext = aLinkedInstance;
    }

    public LinkedInstance getNext() {
        return theNext;
    }

    public void setPrev(LinkedInstance aLinkedInstance) {
        thePrev = aLinkedInstance;
    }

    public LinkedInstance getPrev() {
        return thePrev;
    }

    boolean isEmpty() {
        return (theContent == null);
    }

    /**
       Marks the CBD as being on a particular list, T1, T2, B1 or B2.

       @see org.dancres.blitz.arc.Lru
     */
    void setWhere(int aWhere) {
        isWhere = aWhere;
    }

    /**
       Returns an indication of which list this CBD is on, T1, T2, B1 or B2.

       @see org.dancres.blitz.arc.Lru
     */
    int getWhere() {
        return isWhere;
    }
}
