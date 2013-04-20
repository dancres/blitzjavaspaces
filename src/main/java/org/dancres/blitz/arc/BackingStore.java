package org.dancres.blitz.arc;

import java.io.IOException;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;

/**
   Implementers of this class will be able to store and recover entities based
   on an Identifier.
 */
public interface BackingStore {
    /**
       @return Identifiable associated with Identifier or <code>null</code>
       if it cannot be found.
     */
    public Identifiable load(Identifier anId) throws IOException;

    /**
       Must deal with handling of delete, update and write.  All saves
       MUST be done asynchronously.  i.e.  ArcCache does not expect this
       method to block.  This is required to avoid the risk of deadlock under
       various circumstances.
     */
    public void save(Identifiable anIdentifiable) throws IOException;

    public String getName();
}
