package org.dancres.blitz.stats;

/**
   A class which tracks the number of times a particular operation has been
   performed against a particular type.
 */
public class OpStat implements Stat, StatGenerator {

    public static final int TAKES = 1;
    public static final int WRITES = 2;
    public static final int READS = 3;

    private String theType;
    private long theCount;
    private int theOp;

    private long theId = StatGenerator.UNSET_ID;

    public OpStat(String aType, int anOp) {
        theType = aType;
        theOp = anOp;
    }

    private OpStat(long anId, String aType, int anOp, long aCount) {
        theId = anId;
        theType = aType;
        theOp = anOp;
        theCount = aCount;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public String getType() {
        return theType;
    }

    public int getOp() {
        return theOp;
    }

    public long getCount() {
        return theCount;
    }

    public synchronized void incCount() {
        ++theCount;
    }

    public synchronized Stat generate() {
        OpStat myStat = new OpStat(theId, theType, theOp, theCount);

        return myStat;
    }

    public String getOpTypeAsString() {
        switch(theOp) {
            case TAKES : {
                return "Takes";
            }

            case WRITES : {
                return "Writes";
            } 

            case READS : {
                return "Reads";
            }

            default : {
                return "Unknown";
            }
        }
    }

    public String toString() {
        String myOp = getOpTypeAsString();

        return myOp + ":" + theType + " = " + theCount;
    }
}
