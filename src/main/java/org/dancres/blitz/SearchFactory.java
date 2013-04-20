package org.dancres.blitz;

import java.lang.ref.WeakReference;
import java.io.IOException;

import org.dancres.blitz.txnlock.BaulkedParty;
import org.dancres.blitz.entry.SearchVisitor;
import org.dancres.blitz.entry.EntryRepository;
import org.dancres.blitz.entry.EntryRepositoryFactory;

/**
 * Use this class for standard blocking searches, single or bulk.  For these
 * kinds of searches we care not how many conflicts we get, just when they
 * are resolved at which point we pass them over to our associated MatchTask
 * which can choose to act on them or not.
 */
public class SearchFactory implements VisitorBaulkedPartyFactory {
    public BaulkedParty newParty(SingleMatchTask aMatchTask) {
        return new BaulkedPartyImpl(aMatchTask);
    }

    public BaulkedParty newParty(BulkTakeVisitor aMatchTask) {
        return new BaulkedPartyImpl(aMatchTask);
    }

    public void enableResolutionSignal() {
        // Nothing to do for the standard search case
    }

    private static class BaulkedPartyImpl implements BaulkedParty {
        private WeakReference _task;

        public BaulkedPartyImpl(SingleMatchTask aMatchTask) {
            _task = new WeakReference(aMatchTask);
        }

        public BaulkedPartyImpl(BulkMatchTask aMatchTask) {
            _task = new WeakReference(aMatchTask);
        }

        public void blocked(Object aHandback) {
            // Doesn't matter
        }

        public void unblocked(Object aHandback) {
            Handback myHandback = (Handback) aHandback;

            try {
                SearchVisitor myVisitor = getVisitor();

                if (myVisitor == null)
                    return;

                EntryRepository myRepos =
                    EntryRepositoryFactory.get().get(myHandback.getType());

                myRepos.find(myVisitor, myHandback.getOID(),
                    myHandback.getEntry());

            } catch (IOException aDbe) {
            }
        }

        private SearchVisitor getVisitor() {
            MatchTask myTask = (MatchTask) _task.get();

            if (myTask == null)
                return null;
            else
                return myTask.getVisitor();
        }
    }
}
