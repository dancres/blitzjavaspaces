package org.dancres.blitz.meta;

import java.io.IOException;

import org.dancres.blitz.disk.DiskTxn;

public interface Initializer {
    /**
       @param aRegistry the registry instance being initialized
     */
    public void execute(RegistryAccessor anAccessor) throws IOException;
}
