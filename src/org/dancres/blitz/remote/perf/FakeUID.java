package org.dancres.blitz.remote.perf;

import org.dancres.blitz.lease.SpaceUID;

final class FakeUID implements SpaceUID {
    private static long theFakeMagic = System.currentTimeMillis();
    private static long theFakeId = System.currentTimeMillis();

    private long theMagic = theFakeMagic;
    private long theId = theFakeId;

    FakeUID() {
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof FakeUID) {
            FakeUID myUID = (FakeUID) anObject;

            return ((theId == myUID.theId) && (theMagic == myUID.theMagic));
        }

        return false;
    }

    public int hashCode() {
        return (int) ((theId >>> 32) ^ theId);
    }

    public String toString() {
        return "FakeUID: " + theMagic + "->" + theId;
    }
}