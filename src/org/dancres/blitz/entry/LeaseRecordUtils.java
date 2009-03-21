package org.dancres.blitz.entry;

import org.dancres.util.BytePacker;
import org.dancres.blitz.oid.OIDFactory;

class LeaseRecordUtils {
    static boolean isKey(byte[] anEntry, byte[] aPackedOid) {
        for (int i = 0; i < aPackedOid.length; i++) {
            if (anEntry[8 + i] != aPackedOid[i])
                return false;
        }

        return true;
    }

    static byte[] getLeaseEntry(PersistentEntry anEntry) {
        byte[] myEntry = new byte[8 + OIDFactory.KEY_SIZE];
        BytePacker myPacker = BytePacker.getMSBPacker(myEntry);

        myPacker.putLong(anEntry.getExpiry(), 0);
        myPacker.putArray(anEntry.getKey(), 8);

        // dumpHex("leaseentry", myEntry);

        return myEntry;
    }

    static byte[] getId(PersistentEntry anEntry) {
        // dumpHex("getSleeveKey", aSleeve.getKey());

        return anEntry.getKey();
    }

    static byte[] getBucketKey(PersistentEntry anEntry) {
        return getBucketKey(anEntry.getOID().getZoneId());
    }

    static byte[] getBucketKey(int anId) {
        byte[] myKey = new byte[4];
        BytePacker myPacker = BytePacker.getMSBPacker(myKey);
        myPacker.putInt(anId, 0);

        // dumpHex("bucketKey", myKey);

        return myKey;
    }

    static long unpackExpiry(byte[] aLeaseRecord) {
        BytePacker myPacker = BytePacker.getMSBPacker(aLeaseRecord);

        // System.err.println("Unpack expiry: " + myPacker.getLong(0));

        return myPacker.getLong(0);
    }

    static byte[] unpackId(byte[] aLeaseRecord) {
        BytePacker myPacker = BytePacker.getMSBPacker(aLeaseRecord);

        // System.err.println("Unpack id: " + myPacker.getLong(8));

        return myPacker.getArray(8, OIDFactory.KEY_SIZE);
    }

    private static void dumpHex(String aTitle, byte[] anArray) {
        System.err.println();
        System.err.println(aTitle);
        for (int i = 0; i < anArray.length; i++) {
            System.err.print(Integer.toHexString(anArray[i]) + " ");
        }

        System.err.println();
    }
}
