package org.dancres.blitz.stats;

/**
   <p>Tracks the number of instance of a particular type within a Blitz
   instance.  The instance count accounts for writes and takes but does not
   account for lease expired entries because the cleanup of lease expired
   entries is done lazily.  Thus the instance counts should be treated as
   a guide not an exact number.</p>
 */
public class InstanceCount implements Stat, StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private String theType;
    private int theCount;

    public InstanceCount(String aType, int aCount) {
        theType = aType;
        theCount = aCount;
    }

    private InstanceCount(long anId, String aType, int aCount) {
        theId = anId;
        theType = aType;
        theCount = aCount;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public synchronized Stat generate() {
        InstanceCount myStat = new InstanceCount(theId, theType, theCount);
        return myStat;
    }

    public String getType() {
        return theType;
    }

    public int getCount() {
        return theCount;
    }

    public synchronized void wrote() {
        ++theCount;
    }

    public synchronized void took() {
        --theCount;
    }

    public String toString() {
        return "Total instance of type " + theType + " is " + theCount;
    }
}
