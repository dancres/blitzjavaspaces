package org.dancres.blitz.meta;

import java.io.IOException;

/**
   Note instances of this class are not MT-safe
 */
public interface MetaIterator {
    public MetaEntry fetch() throws IOException;

    public void release() throws IOException;
}
