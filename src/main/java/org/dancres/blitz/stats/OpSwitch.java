package org.dancres.blitz.stats;

/**
   Controls the tracking of a particular operation type against a particular
   type.
 */
public class OpSwitch implements Switch {
    public static final int TAKE_OPS = 1;
    public static final int WRITE_OPS = 2;
    public static final int READ_OPS = 4;

    public static final String ALL_TYPES = "*";

    private String theType;
    private int theOp;
    private boolean isEnabled;
    /**
       @param aType specifies a particular entry classname or the wildcard
       ALL_TYPES
       @param anOp should be an OR'd combination of TAKE_OPS, WRITE_OPS and
       READ_OPS
     */
    public OpSwitch(String aType, int anOp, boolean enable) {
        theType = aType;
        theOp = anOp;
        isEnabled = enable;
    }

    public String getType() {
        return theType;
    }

    public int getOp() {
        return theOp;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isWildcard() {
        return theType.equals(ALL_TYPES);
    }

    /**
       Equal if the switch is an OpSwitch and it has the same type and
       operation.
     */
    public boolean equals(Object anObject) {
        if (anObject instanceof OpSwitch) {
            OpSwitch myOther = (OpSwitch) anObject;

            if (myOther.theType.equals(theType)) {
                return (myOther.theOp == theOp);
            }
        }

        return false;
    }
}
