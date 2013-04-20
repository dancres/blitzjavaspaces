package org.dancres.blitz.stats;

/**
   <p>Tracks memory usage statistics.  This stat is permanently enabled and
   cannot be switched on or off.  It's maintenance costs are entirely absorbed
   by the caller recovering stats from StatsBoard.</p>
 */
public class MemoryStat implements Stat, StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private long theMaxMemory;
    private long theCurrentMemory;

    MemoryStat() {
    }

    MemoryStat(long anId, long aMaxMem, long aCurrentMem) {
        theId = anId;
        theMaxMemory = aMaxMem;
        theCurrentMemory = aCurrentMem;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public synchronized Stat generate() {
        MemoryStat myStat = new MemoryStat(theId, 
                                           Runtime.getRuntime().maxMemory(),
                                           Runtime.getRuntime().totalMemory());
        return myStat;
    }

    public long getMaxMemory() {
        return theMaxMemory;
    }

    public long getCurrentMemory() {
        return theCurrentMemory;
    }

    public String toString() {
        return "Memory: " + theCurrentMemory + " of: " + theMaxMemory;
    }
}
