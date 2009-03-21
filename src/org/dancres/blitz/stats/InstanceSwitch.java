package org.dancres.blitz.stats;

/**
   Used to enable/disable generation of instance counts
 */
public class InstanceSwitch implements Switch {
    public static final String ALL_TYPES = "*";

    private boolean isOnOff;
    private String theType;

    public InstanceSwitch(String aType, boolean onOff) {
        theType = aType;
        isOnOff = onOff;
    }

    public String getType() {
        return theType;
    }

    public boolean isOn() {
        return isOnOff;
    }

    public boolean isWildcard() {
        return (theType.equals(ALL_TYPES));
    }
}
