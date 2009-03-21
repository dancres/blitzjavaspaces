package org.dancres.blitz.entry;

import org.dancres.blitz.mangler.MangledEntry;

/**
   @see org.dancres.blitz.disk.SearchOffer
 */
class SearchOfferImpl implements SearchOffer {
    private OpInfo theInfo;
    private MangledEntry theEntry;
    
    SearchOfferImpl(MangledEntry anEntry, OpInfo anInfo) {
        theInfo = anInfo;
        theEntry = anEntry;
    }

    public MangledEntry getEntry() {
        return theEntry;
    }

    public OpInfo getInfo() {
        return theInfo;
    }
}
