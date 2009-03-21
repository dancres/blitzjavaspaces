package org.dancres.blitz;

import org.dancres.blitz.entry.SearchOffer;
import org.dancres.blitz.entry.SearchVisitor;
import org.dancres.blitz.mangler.MangledEntry;

/**
 * Responsible for sourcing matching Entry's from the EntryRepositories to
 * go into the UIDSet
 */
class DiskView implements SearchVisitor {
    private UIDSet theUIDs;
    private MangledEntry theTemplate;

    DiskView(MangledEntry aTemplate, UIDSet aSet) {
        theTemplate = aTemplate;
        theUIDs = aSet;
    }

    public int offer(SearchOffer anOffer) {
        MangledEntry myTarget = anOffer.getEntry();

        if (theTemplate.match(myTarget)) {
            // Need to record a globally unique id for later
            theUIDs.add(new SpaceEntryUID(anOffer.getEntry().getType(),
                                            anOffer.getInfo().getOID()));
        }

        if (theUIDs.isFull())
            return STOP;
        else
            return TRY_AGAIN;
    }

    /**
       @return <code>true</code> if this Visitor wishes to perform a take.
     */
    public boolean isDeleter() {
        return false;
    }
}