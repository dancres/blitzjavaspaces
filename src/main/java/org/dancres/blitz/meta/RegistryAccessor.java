package org.dancres.blitz.meta;

import java.io.IOException;
import java.io.Serializable;

/**
   @see org.dancres.blitz.meta.Registry
 */
public interface RegistryAccessor {
    public byte[] loadRaw(byte[] aKey) throws IOException;

    public Serializable load(byte[] aKey) throws IOException;

    public void save(byte[] aKey, Serializable anObject) throws IOException;

    public void delete(byte[] aKey) throws IOException;

    public MetaIterator readAll() throws IOException;
}
