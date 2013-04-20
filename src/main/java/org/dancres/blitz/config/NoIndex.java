package org.dancres.blitz.config;

public class NoIndex implements EntryConstraint {
    private static final String TYPE = "NOINDEX";

    public NoIndex() {
    }

    public int hashCode() {
        return TYPE.hashCode();
    }

    public boolean equals(Object anObject) {
        return (anObject instanceof NoIndex);
    }
}
