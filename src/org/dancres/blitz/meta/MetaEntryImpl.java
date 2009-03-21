package org.dancres.blitz.meta;

import java.io.Serializable;

class MetaEntryImpl implements MetaEntry {
    private byte[] theKey;
    private Serializable theData;

    MetaEntryImpl(byte[] aKey, Serializable aData) {
        theKey = aKey;
        theData = aData;
    }

    public byte[] getKey() {
        return theKey;
    }

    public Serializable getData() {
        return theData;
    }
}
