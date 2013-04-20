package org.dancres.blitz.entry;

import java.util.logging.Level;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.oid.OIDFactory;
import org.dancres.blitz.oid.OID;
import org.dancres.util.ObjectTransformer;
import org.dancres.util.BytePacker;

/**
 */
public class PersistentEntry {
    private static final int EXPIRY_LENGTH = 8;

    private static final int HASH_COUNT_LENGTH = 4;

    /*
      All disk-structures are kept "simple" - this removes the need for
      costly (in terms of time to do it and space on disk) serialization
     */
    private byte[] theEntryBytes;
    private long theExpiry;
    private byte[] theKey;
    private byte[] theHashCodes;

    private transient MangledEntry theEntry;
    private transient OID theId;

    PersistentEntry(OID anId, MangledEntry anEntry, long anExpiry) {
        theEntry = anEntry;
        theId = anId;
        theExpiry = anExpiry;
    }

    PersistentEntry(byte[] aBytes) {
        BytePacker myPacker = BytePacker.getMSBPacker(aBytes);

        theExpiry = myPacker.getLong(0);
        theKey = myPacker.getArray(EXPIRY_LENGTH, OIDFactory.KEY_SIZE);

        int myHashCodesLength =
            myPacker.getInt(EXPIRY_LENGTH + OIDFactory.KEY_SIZE);

        theHashCodes = myPacker.getArray(EXPIRY_LENGTH + OIDFactory.KEY_SIZE +
            HASH_COUNT_LENGTH, myHashCodesLength);

        int myRemainder = aBytes.length - EXPIRY_LENGTH - OIDFactory.KEY_SIZE -
            myHashCodesLength - HASH_COUNT_LENGTH;

        theEntryBytes = myPacker.getArray(EXPIRY_LENGTH + OIDFactory.KEY_SIZE +
            myHashCodesLength +
            HASH_COUNT_LENGTH,
            myRemainder);
    }

    /**
     * Convert EntrySleeveImpl into a byte representation suitable for
     * saving to disk.
     */
    byte[] flatten() {
        if (theEntryBytes == null) {
            try {
                theEntryBytes = ObjectTransformer.toByte(theEntry);
            } catch (Exception anE) {
                EntrySleeveImpl.theLogger.log(
                    Level.SEVERE, "Couldn't flatten entry!", anE);
            }
        }

        byte[] myArray = new byte[theEntryBytes.length + OIDFactory.KEY_SIZE
            + EXPIRY_LENGTH + HASH_COUNT_LENGTH +
            getHashCodes().length];

        BytePacker myPacker = BytePacker.getMSBPacker(myArray);
        myPacker.putLong(theExpiry, 0);
        myPacker.putArray(getKey(), EXPIRY_LENGTH);
        myPacker.putInt(getHashCodes().length, EXPIRY_LENGTH +
            OIDFactory.KEY_SIZE);
        myPacker.putArray(getHashCodes(), EXPIRY_LENGTH + OIDFactory.KEY_SIZE +
            HASH_COUNT_LENGTH);
        myPacker.putArray(theEntryBytes, EXPIRY_LENGTH + OIDFactory.KEY_SIZE +
            HASH_COUNT_LENGTH + getHashCodes().length);

        return myArray;
    }

    PersistentEntry duplicate() {
        return new PersistentEntry(getOID(), getEntry(), getExpiry());
    }

    private synchronized byte[] getHashCodes() {
        /*
          Should only happen if we've been newly created and never been
          flattened
         */
        if (theHashCodes == null) {
            MangledEntry myEntry = getEntry();

            theHashCodes = new byte[myEntry.getFields().length * 4];

            BytePacker myPacker = BytePacker.getMSBPacker(theHashCodes);

            for (int i = 0; i < myEntry.getFields().length; i++) {
                myPacker.putInt(myEntry.getField(i).hashCode(), i * 4);
            }
        }

        return theHashCodes;
    }

    synchronized int getHashCodeForField(int anOffset) {
        BytePacker myPacker = BytePacker.getMSBPacker(getHashCodes());

        return myPacker.getInt(anOffset * 4);
    }

    synchronized MangledEntry getEntry() {
        if (theEntry == null) {
            try {
                theEntry =
                    (MangledEntry) ObjectTransformer.toObject(theEntryBytes);
            } catch (Exception anE) {
                EntrySleeveImpl.theLogger.log(
                    Level.SEVERE, "Couldn't recover entry", anE);
            }
        }

        return theEntry;
    }

    synchronized byte[] getKey() {

        /*
          Should only happen if we've been newly created and never been
          flattened
         */
        if (theKey == null)
            theKey = OIDFactory.getKey(theId);

        return theKey;
    }

    synchronized OID getOID() {
        if (theId == null) {
            theId = OIDFactory.newOID(theKey);
        }

        return theId;
    }

    synchronized boolean hasExpired(long aTime) {
        return (theExpiry < aTime);
    }

    synchronized void setExpiry(long anExpiry) {
        theExpiry = anExpiry;
    }

    synchronized long getExpiry() {
        return theExpiry;
    }
}
