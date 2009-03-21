package org.dancres.blitz.stats;

/**
   Provides information about the total number of blocking reads and takes
   active within a Blitz instance.
 */
public class BlockingOpsStat implements Stat {
    private int theReadCount;
    private int theTakeCount;
    private long theId;

    public BlockingOpsStat(long anId, int aReadCount, int aTakeCount) {
        theId = anId;
        theReadCount = aReadCount;
        theTakeCount = aTakeCount;
    }

    public long getId() {
        return theId;
    }

    public int getReaders() {
        return theReadCount;
    }

    public int getTakers() {
        return theTakeCount;
    }

    public String toString() {
        return "Blocking reads: " + theReadCount + ", takes: " + theTakeCount;
    }
}
