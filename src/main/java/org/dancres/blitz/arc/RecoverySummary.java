package org.dancres.blitz.arc;

/**
   When ArcCache is asked to recover an instance it returns an instance of
   this class which reports the CacheBlockDescriptor of the instance and
   whether or not the instance was present on disk
 */
public class RecoverySummary {
    private boolean wasOnDisk;
    private CacheBlockDescriptor theCBD;

    RecoverySummary(CacheBlockDescriptor aCBD, boolean onDisk) {
        theCBD = aCBD;
        wasOnDisk = onDisk;
    }

    public CacheBlockDescriptor getCBD() {
        return theCBD;
    }

    public boolean wasOnDisk() {
        return wasOnDisk;
    }
}
