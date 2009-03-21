package org.dancres.blitz.test;

import net.jini.core.entry.Entry;

public class TestEntry implements Entry {
    private String theField;

    public TestEntry(String aValue) {
        theField = aValue;
    }
}
