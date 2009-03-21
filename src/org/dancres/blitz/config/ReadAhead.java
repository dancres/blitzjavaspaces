package org.dancres.blitz.config;

/**
 * <p>When Blitz encounters a template Entry created via snapshot() it will
 * attempt a readahead from disk when there are no matches obtained from the
 * cache. One can specify a global default readahead for all types using <code>
 * entryReposReadahead</code> which can be overidden with individual
 * <code>ReadAhead</code> <code>EntryConstraint</code>s.</p>
 *
 * <p>Specifying a readahead size of 0 disables readahead and is an enforced
 * default for those Entry types tagged for FIFO operation.  Specifying any
 * other positive value causes Blitz to fault in up to <code>readahead</code>
 * Entry's from disk which are likely to match the <code>snapshot</code>'d
 * template.</p>
 *
 * <p>Readahead is useful in cold start situations where the cache will be
 * empty and also in cases where swapping occurs but Entry's are accessed in
 * groups via a snapshot template.</p>
 */
public class ReadAhead implements EntryConstraint {
    private int theSize;

    public ReadAhead(int aSize) {
        theSize = aSize;
    }

    public int getSize() {
        return theSize;
    }
}
