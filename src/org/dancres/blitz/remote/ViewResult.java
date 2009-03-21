package org.dancres.blitz.remote;

import java.io.Serializable;

import org.dancres.blitz.EntryChit;

/**
 * Used to contain a lease and a first chunk of EntryChits destined for a MatchSetImpl or
 * an AdminIteratorImpl.  The first batch is piggy-backed onto the initial request to create
 * a view as an optimization for saving a further roundtrip which would otherwise be
 * required to get the first batch of EntryChits (which might also be the last of course
 * if the set is less than the chunk size).
 */
public class ViewResult implements Serializable {
    private LeaseImpl theLease;
    private EntryChit[] theInitialBatch;

    ViewResult(LeaseImpl aLease, EntryChit[] anInitialBatch) {
        theLease = aLease;
        theInitialBatch = anInitialBatch;
    }

    LeaseImpl getLease() {
        return theLease;
    }

    EntryChit[] getInitialBatch() {
        return theInitialBatch;
    }
}
