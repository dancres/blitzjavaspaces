package org.dancres.blitz.oid;

/**
   <p>Provides the means by which to reconstitute an OID from it's constituent
   parts.</p>

   <p>This class will change some when we introduce a node id as will
   the OID interface.  Likely as not, all marshalling and unmarshalling will
   move into this class and need to introduce a type identifier as part of
   the marshalling to make sure we return the appropriate concrete
   implementation class.</p>
 */
public class OIDFactory {
    public static final int KEY_SIZE = UIDImpl.KEY_SIZE;

    public static OID newOID(int aZoneId, long anId) {
        return new UIDImpl(aZoneId, anId);
    }

    public static OID newOID(byte[] aKey) {
        return new UIDImpl(aKey);
    }

    public static byte[] getKey(OID anOID) {
        return ((UIDImpl) anOID).getKey();
    }
}