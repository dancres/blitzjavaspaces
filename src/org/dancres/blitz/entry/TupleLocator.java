package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.oid.OID;

/**
   All searches, whether they are performed in-memory or on-disk, result
   in TupleLocators which are then used to iterate over the results.
   Returned results are always UIDImpls which are then used to load
   an appropriate EntrySleeve via SleeveCache (this ensures correct state is
   maintained as the request to load a sleeve will make it's way into
   an ArcCache then the WriteBuffer and then disk storage).
 */
public interface TupleLocator {
    /**
       Invoke this to load the next matching Tuple.

       @return <code>true</code> if there was a tuple, <code>false</code>
       otherwise
     */
    boolean fetchNext() throws IOException;

    /**
       @return the OID of the tuple just fetched with
       <code>fetchNext</code>
     */
    OID getOID();

    /**
       When you've finished with the TupleLocator instance, call release.
     */
    void release() throws IOException;
}
