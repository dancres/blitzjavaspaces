package org.dancres.blitz.meta;

import java.io.IOException;
import java.io.Serializable;

import org.dancres.blitz.disk.DiskTxn;

class RegistryAccessorImpl implements RegistryAccessor {
    private DiskTxn theTxn;
    private RegistryImpl theRegistry;

    RegistryAccessorImpl(RegistryImpl aRegistry) {
        this(aRegistry, DiskTxn.getActiveTxn());
    }

    RegistryAccessorImpl(RegistryImpl aRegistry, DiskTxn aTxn) {
        theTxn = aTxn;
        theRegistry = aRegistry;
    }

    public byte[] loadRaw(byte[] aKey) throws IOException {
        return theRegistry.loadRaw(theTxn, aKey);
    }

    public Serializable load(byte[] aKey) throws IOException {
        return theRegistry.load(theTxn, aKey);
    }

    public void save(byte[] aKey, Serializable anObject) throws IOException {
        theRegistry.save(theTxn, aKey, anObject);
    }

    public void delete(byte[] aKey) throws IOException {
        theRegistry.delete(theTxn, aKey);
    }

    public MetaIterator readAll() throws IOException {
        return theRegistry.readAll(theTxn);
    }
}
