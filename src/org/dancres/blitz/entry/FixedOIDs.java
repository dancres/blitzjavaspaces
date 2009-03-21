package org.dancres.blitz.entry;

import org.dancres.util.BytePacker;

/**
   Various fixed OIDs are used to store meta data in the registry associated
   with a particular type.  This class encapsulates all the knowledge of what
   OIDs are used to store which bits of information.
 */
class FixedOIDs {
    private static final long INDEXES_OID = 0;
    private static final long SUBTYPES_OID = 1;

    static final byte[] INDEXES_KEY =
        keyFor(INDEXES_OID);

    static final byte[] SUBTYPES_KEY =
        keyFor(SUBTYPES_OID);

    static byte[] keyFor(long anId) {
        byte[] myKey = new byte[8];

        BytePacker myPacker = BytePacker.getMSBPacker(myKey);
        myPacker.putLong(anId, 0);

        return myKey;
    }

}
