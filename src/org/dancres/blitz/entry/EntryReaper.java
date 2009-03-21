package org.dancres.blitz.entry;

import java.io.IOException;

/**
   Invoked on by a LeaseTracker in response to a call to bringOutTheDead

   @see org.dancres.blitz.entry.LeaseTracker
 */
public interface EntryReaper {
    /**
       @param aLocator TupleLocator which will return a sequence of
       <code>UIDImpl</code>s for <code>EntrySleeve</code>s which are
       <em>likely</em> (not guarenteed, so the user should check explicitly
       using an appropriate timestamp
     */
    public void clean(TupleLocator aLocator) throws IOException;
}
