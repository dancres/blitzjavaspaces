package org.dancres.blitz.stats;

public class MissedOpsStat implements Stat {
    private long theReadMiss;
    private long theTakeMiss;
    private long theId;

    public MissedOpsStat(long anId, long aReadMiss, long aTakeMiss) {
        theId = anId;
        theReadMiss = aReadMiss;
        theTakeMiss = aTakeMiss;
    }

    public long getId() {
        return theId;
    }

    public long getMissedReads() {
        return theReadMiss;
    }

    public long getMissedTakes() {
        return theTakeMiss;
    }

    public String toString() {
        return "Missed reads: " + theReadMiss + ", takes: " + theTakeMiss;
    }
}
