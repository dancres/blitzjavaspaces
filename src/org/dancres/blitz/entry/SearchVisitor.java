package org.dancres.blitz.entry;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.mangler.MangledEntry;

/**
   When an EntryRepository is asked to perform a search based on a template
   it offers matches to a SearchVisitor until it either exhausts all
   possibilities or is told to stop by the SearchVisitor.  Note searches
   should lock the state of the associated EntrySleeve whilst
   <code>offer</code> is invoked to prevent acceptance by SearchVisitorImpl
   which must later be cancelled due to the underlying EntrySleeve having
   gone into an unreachable state.
 */
public interface SearchVisitor {
    /**
       Returned from offer when the SearchVisitor wishes to exit the search
       and doesn't wish to accept the offer.
     */
    public static final int STOP = 1;

    /**
       Returned from offer when the SearchVisitor wishes to accept the offer.
       The FS should make good on the offer before stopping the search.
     */
    public static final int ACCEPTED = 2;

    /**
       Indicates the SearchVisitor cannot accept the offer and wishes search
       to continue with more offers
     */
    public static final int TRY_AGAIN = 3;

    /**
       This method can also be used by the asynchronous recent write code.

       @return STOP if the SearchVisitor wishes to halt the search and
       doesn't want to act on the offer.
     */
    public int offer(SearchOffer anOffer);

    /**
       @return <code>true</code> if this Visitor wishes to perform a take.
     */
    public boolean isDeleter();    
}
