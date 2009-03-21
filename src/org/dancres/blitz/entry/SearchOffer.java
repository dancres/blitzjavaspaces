package org.dancres.blitz.entry;

import org.dancres.blitz.mangler.MangledEntry;

/**
   Whenever the filesystem locates an Entry suitable for a specified search,
   it offers it to the specified SearchVisitor using an instance of this
   object. <P>

   The SearchVisitor can use the available information to determine whether
   the Entry is a suitable match from it's perspective which includes doing
   a deep match, checking expiry, txn locks etc.  If the SearchVisitor is
   happy, it accepts the offer and saves the OpInfo into the log and commits
   or aborts later. <P>
 */
public interface SearchOffer {
    public MangledEntry getEntry();

    public OpInfo getInfo();
}
