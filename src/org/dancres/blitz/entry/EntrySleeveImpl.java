package org.dancres.blitz.entry;

import java.util.logging.Logger;

import net.jini.core.entry.Entry;
import org.dancres.struct.LinkedInstance;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.mangler.EntryMangler;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.oid.OIDFactory;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;

import org.dancres.blitz.Logging;
import org.dancres.blitz.entry.ci.CacheIndexer;

/**
   <p> The in-memory representation of an Entry (as in record sleeve) -
   includes transient caching state as well as persistent information. </p>

   <p> Call flatten to render an instance into a byte[] suitable for disk
   storage and invoke the constructor EntrySleeveImpl(byte[]) to reconstitute
   the flattened form from disk. </p>

   <p>We track dirty state explicitly on the sleeve and it's managed in the
   layers above the WriteBuffer which only concerns itself with PersistentEntry
   instances and getting them to disk.</p>
 */
final class EntrySleeveImpl implements EntrySleeve, Identifiable,
                                       LinkedInstance {

    static final Logger theLogger =
            Logging.newLogger("org.dancres.blitz.entry.EntrySleeveImpl");

    private transient SleeveState theState = new SleeveState();

    private transient LinkedInstance theNext;
    private transient LinkedInstance thePrev;

    private transient PersistentEntry theEntry;

    private transient boolean isDirty;

    private EntrySleeveImpl() {
    }

    /**
       Use this constructor ONLY for EntrySleeveImpls that have never been
       written to disk.  Typically this constructor is called for
       space::write() or recovery processing.
     */
    EntrySleeveImpl(OID anId, MangledEntry anEntry, long anExpiry) {
        theEntry = new PersistentEntry(anId, anEntry, anExpiry);
        getState().set(SleeveState.NOT_ON_DISK | SleeveState.PINNED);
    }

    /**
       Take a byte[] as returned by flatten and re-constitute an
       EntrySleeveImpl from it.  Use this constructor for rebuilding state
       saved to disk.
     */
    EntrySleeveImpl(byte[] aBytes) {
        theEntry = new PersistentEntry(aBytes);
    }

    EntrySleeveImpl(PersistentEntry anEntry) {
        theEntry = anEntry;
    }
    
    /**
       @return a deep duplicate of this EntrySleeveImpl, including current
       SleeveState
     */
    EntrySleeveImpl duplicate() {
        EntrySleeveImpl myDupe = new EntrySleeveImpl();

        // Copy status flags
        myDupe.getState().set(getState().get());

        myDupe.theEntry = new PersistentEntry(theEntry.flatten());

        return myDupe;
    }

    byte[] flatten() {
        return theEntry.flatten();
    }

    SleeveState getState() {
        synchronized(this) {
            if (theState == null)
                theState = new SleeveState();
        }

        return theState;
    }

    public boolean isDeleted() {
        return getState().test(SleeveState.DELETED);
    }

    boolean hasExpired(long aTime) {
        return theEntry.hasExpired(aTime);
    }

    void setExpiry(long anExpiry) {
        theEntry.setExpiry(anExpiry);
    }

    long getExpiry() {
        return theEntry.getExpiry();
    }

    public int getHashCodeForField(int anOffset) {
        return theEntry.getHashCodeForField(anOffset);
    }

    byte[] getKey() {
        return theEntry.getKey();
    }

    public void setNext(LinkedInstance anInstance) {
        theNext = anInstance;
    }

    public void setPrev(LinkedInstance anInstance) {
        thePrev = anInstance;
    }

    public LinkedInstance getNext() {
        return theNext;
    }

    public LinkedInstance getPrev() {
        return thePrev;
    }

    /**
       Ensure we only ever hand out one ID whilst we are cached so, should
       someone wish to WeakRef the ID, it can.  Id will be re-constituted
       from key only when we've been reloaded from disk causing theId to
       be null and flatten to have converted theId to a key made of bytes held
       in theKey.
     */
    public OID getOID() {
        return theEntry.getOID();
    }

    public Identifier getId() {
        return getOID();
    }

    public String getType() {
        return getEntry().getType();
    }

    public MangledEntry getEntry() {
        return theEntry.getEntry();
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof EntrySleeveImpl) {
            EntrySleeveImpl myOther = (EntrySleeveImpl) anObject;

            return myOther.getOID().equals(getOID());
        }

        return false;
    }

    public String toString() {
        return "ES: " + getOID() + " - state: " + getState().toString();

    }

    void markDirty() {
        isDirty = true;

        if (isDeleted()) {
            CacheIndexer.getIndexer(getType()).flushed(this);
        }
    }

    void clearDirty() {
        isDirty = false;
    }

    boolean isDirty() {
        return isDirty;
    }

    public static class DummyEntry implements Entry {
        public String theName;
        public String anotherField;

        public DummyEntry() {
        }

        public DummyEntry(String aName) {
            theName = aName;
        }

        public String toString() {
            return theName;
        }

        public boolean equals(Object anObject) {
            if ((anObject != null) && (anObject instanceof DummyEntry)) {

                DummyEntry myEntry = (DummyEntry) anObject;

                if (myEntry.theName == null)
                    return (myEntry.theName == theName);
                else
                    return ((DummyEntry) anObject).theName.equals(theName);
            }

            return false;
        }
    }

    public static void main(String args[]) {
        try {
            EntryMangler myMangler = EntryMangler.getMangler();

            DummyEntry myEntry = new DummyEntry("rhubarb");

            MangledEntry myMangled = myMangler.mangle(myEntry);

            EntrySleeveImpl mySleeve =
                new EntrySleeveImpl(OIDFactory.newOID(10, 21), myMangled,
                                    Long.MAX_VALUE);

            byte[] myFlatten = mySleeve.flatten();

            mySleeve = new EntrySleeveImpl(myFlatten);

            System.out.println(mySleeve.getEntry());

            System.out.println(mySleeve.getOID());

            mySleeve = new EntrySleeveImpl(myFlatten);

            mySleeve = mySleeve.duplicate();

            myFlatten = mySleeve.flatten();

            mySleeve = new EntrySleeveImpl(myFlatten);

            System.out.println(mySleeve.getEntry());

            System.out.println(mySleeve.getOID());
            
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    PersistentEntry getPersistentRep() {
        return theEntry;
    }
}
