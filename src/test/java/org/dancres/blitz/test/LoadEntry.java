package org.dancres.blitz.test;

import net.jini.core.entry.Entry;

public class LoadEntry implements Entry {
    public String rhubarb;
    public Integer count;

    public LoadEntry() {
    }

    public LoadEntry(String aString) {
        rhubarb = aString;
        count = new Integer(5);
    }

    public void init() {
        rhubarb = "blah";
        count = new Integer(5);
    }

    public String toString() {
        return super.toString() + ", " + rhubarb + ", " + count;
    }
}
