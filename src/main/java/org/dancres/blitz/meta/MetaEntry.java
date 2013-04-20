package org.dancres.blitz.meta;

import java.io.Serializable;

public interface MetaEntry {
    public byte[] getKey();
    public Serializable getData();
}
