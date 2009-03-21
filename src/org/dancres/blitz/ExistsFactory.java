package org.dancres.blitz;

import java.lang.ref.WeakReference;
import java.io.IOException;

import org.dancres.blitz.txnlock.BaulkedParty;
import org.dancres.blitz.entry.SearchVisitor;
import org.dancres.blitz.entry.EntryRepository;
import org.dancres.blitz.entry.EntryRepositoryFactory;
import org.dancres.blitz.mangler.MangledEntry;

/**
 * Use this class for *Exists searches return single results, bulk operations
 * are <b>not</b> currently supported.
 *
 * This class maintains a count of transaction conflicts that have occurred
 * and when it reaches zero, signals the associated <code>MatchTask</code> via
 * it's <code>SearchVisitor</code>.  Note the signal is only permitted after
 * <code>enableResolutionSignal</code> has been called.  This provides a
 * barrier such that a full disk-search can be completed without false
 * terminations from resulting conflicts.
 */
public class ExistsFactory implements VisitorBaulkedPartyFactory {
    private int _conflicts;
    private boolean _resolvable = false;
    private WeakReference _task;

    public BaulkedParty newParty(SingleMatchTask aMatchTask) {
        _task = new WeakReference(aMatchTask);
        return new BaulkedPartyImpl();
    }

    public BaulkedParty newParty(BulkTakeVisitor aMatchTask) {
        throw new RuntimeException
            ("Exists not currently supported on bulk operations");
    }

    public void enableResolutionSignal() {
        synchronized (this) {
            _resolvable = true;
        }

        testAndSignal();
    }

    private SearchVisitor getVisitor() {
        MatchTask myTask = (MatchTask) _task.get();

        if (myTask == null)
            return null;
        else
            return myTask.getVisitor();
    }

    private void testAndSignal() {
        int myConflicts;

        synchronized(this) {
            if (!_resolvable)
                return;

            myConflicts = _conflicts;
        }

        if (myConflicts == 0) {
            SingleMatchTask myTask = (SingleMatchTask) _task.get();

            if (myTask != null) {
                myTask.sendEvent(CompletionEvent.COMPLETED);
            }
        }
    }

    private class BaulkedPartyImpl implements BaulkedParty {

        public BaulkedPartyImpl() {
        }

        public void blocked(Object aHandback) {
            synchronized(ExistsFactory.this) {
                ++_conflicts;
            }
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
            } finally {
                synchronized(ExistsFactory.this) {
                    --_conflicts;
                }

                testAndSignal();
            }
        }
    }
}
