package org.dancres.blitz;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.SearchVisitor;

public interface MatchTask {
    public SearchVisitor getVisitor();

    public boolean wouldBlock();
}