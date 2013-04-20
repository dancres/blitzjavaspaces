package org.dancres.blitz;

import java.io.IOException;

import org.dancres.struct.LinkedInstance;

import org.dancres.blitz.entry.SearchVisitor;
import org.dancres.blitz.entry.LongtermOffer;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.mangler.MangledEntry;

/**
   Every search carried out by Blitz is represented by an instance of this
   class. <P>

   The search requirement for such a task can be satisfied by a disk/cache
   based search or the arrival of a new Entry written by a client.

   @see org.dancres.blitz.SearchTasks
 */
public interface SearchTask extends LinkedInstance {
    public SearchVisitor getVisitor();

    public void destroy();

    /**
       Called to indicate that a new Entry has been written to cache.
       Details of the Entry's identification and type are passed in to
       allow the task to perform matching or other appropriate actions.

       @return <code>true</code> if processing occurred.  If the Entry
       being received was no longer available, searching should stop.
       If in doubt, the implementer should return <code>true</code>.
     */
    public boolean writeReceived(LongtermOffer anOffer) throws IOException;
}
