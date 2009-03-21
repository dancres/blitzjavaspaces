package org.dancres.blitz.entry;

import java.io.IOException;

/**
   Interface through which the WriteBuffer updates on-disk state - thus the
   WriteBuffer knows nothing of the underlying storage method.
 */
public interface EntryEditor {
    public String getType();
    public void delete(PersistentEntry anEntry) throws IOException;
    public void update(PersistentEntry anEntry) throws IOException;
    public void write(PersistentEntry anEntry) throws IOException;
}
