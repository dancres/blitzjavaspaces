package org.dancres.blitz;

import java.io.Serializable;

import org.dancres.blitz.mangler.MangledEntry;

/**
   Holds the packaged Entry together with the space-global identifier it's
   associated with.
 */
public class EntryChit implements Serializable {
    private MangledEntry theEntry;
    private Object theCookie;

    EntryChit(MangledEntry anEntry, SpaceEntryUID aCookie) {
        theEntry = anEntry;
        theCookie = aCookie;
    }

    public Object getCookie() {
        return theCookie;
    }

    public MangledEntry getEntry() {
        return theEntry;
    }
}