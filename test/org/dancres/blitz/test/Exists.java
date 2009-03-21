package org.dancres.blitz.test;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.io.IOException;

import net.jini.space.JavaSpace;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.entry.UnusableEntryException;
import net.jini.lease.LeaseRenewalManager;

import org.dancres.blitz.remote.LocalSpace;

/**
 */
public class Exists {
    private static Logger log_ = Logger.getLogger(Exists.class.getName());

    private static final long SLEEP_TIME_ = 5 * 1000;

    private JavaSpace space_ = null;
    private TxnMgr txnMgr_ = null;

    private Thread threadOne_ = null;
    private Thread threadTwo_ = null;

    public static void main(String[] args) throws Exception {
        LocalSpace mySpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace space = mySpace.getProxy();
        TxnMgr txnMgr = new TxnMgr(1, mySpace);

        log_.info("Space: " + space);
        log_.info("TxnMgr: " + txnMgr);

        Exists stb = new Exists(space, txnMgr);
        log_.info("Beginning SpaceTxnBlock test case");
        stb.runTest();
        mySpace.stop();
    }

    public Exists(JavaSpace space, TxnMgr txnMgr) {
        space_ = space;
        txnMgr_ = txnMgr;
    }

    public void runTest() {
        ServerTransaction txn = null;

        try {
            // Write two new entries to test on
            log_.info("Initialising test case");
            txn = getTransaction();

            // Cleanup from last time
            while (space_.takeIfExists(new BlockEntry(null),
                txn, Lease.FOREVER) != null) {
                log_.log(Level.INFO, "Removed old block entry");
            }

            // A single entry to test on
            BlockEntry entryTest = new BlockEntry("Test");
            log_.log(Level.INFO, "Writing new entry: " + entryTest);
            space_.write(entryTest, txn, Lease.FOREVER);

            txn.commit();
            txn = null;

            log_.log(Level.INFO, "Template used: " + entryTest);
            threadOne_ = new TestThreadOne(entryTest);
            threadTwo_ = new TestThreadTwo(entryTest);

            log_.info("Completed initialisation, beginning test");

            threadOne_.start();
            threadTwo_.start();

            threadOne_.join();
            threadTwo_.join();

            log_.info("Test completed, both threads have exited");
        }
        catch (RemoteException re) {
            log_.log(Level.WARNING, "Remote Exception", re);
        }
        catch (TransactionException te) {
            log_.log(Level.WARNING, "Transaction Exception", te);
        }
        catch (InterruptedException te) {
            log_.log(Level.WARNING, "Interrupted Exception", te);
        }
        catch (UnusableEntryException uee) {
            log_.log(Level.WARNING, "Unusable Entry: " + uee.partialEntry, uee);
            Throwable[] nested_exception = uee.nestedExceptions;

            for (int i = 0; i < nested_exception.length; i++) {
                log_.log(Level.WARNING, "Nested exception " + i, nested_exception[i]);
            }
        }
        finally {
            if (txn != null) {
                try {
                    txn.abort();
                }
                catch (Exception e) {
                    log_.log(Level.WARNING, "Cannot abort transaction", e);
                }
            }
        }
    }

    private ServerTransaction getTransaction() throws RemoteException {
        return txnMgr_.newTxn();
    }

    /**
     * Implements the first thread that should attempt to take the object and
     * gets permanently blocked.
     *
     * @author dominicc
     */
    private class TestThreadOne extends Thread {
        private ServerTransaction txn_ = null;

        private BlockEntry entryTestTmpl_ = null;

        public TestThreadOne(BlockEntry entryTestTmpl) {
            entryTestTmpl_ = entryTestTmpl;
        }

        public void run() {
            try {
                txn_ = getTransaction();

                // t=1
                log_.info("Currently t=1");
                Thread.sleep(SLEEP_TIME_);

                // t=2
                log_.info("Currently t=2");
                log_.log(Level.INFO, "Attempting to read test entry: " + entryTestTmpl_);
                BlockEntry entryTestRead =
                    (BlockEntry) space_.takeIfExists(entryTestTmpl_,
                        txn_,
                        Lease.FOREVER);

                // Shouldn't ever get to here, unless the other thread writes
                // the entry back, as the above call will block
                log_.log(Level.INFO, "Read test entry from txn one: " + entryTestRead);
                Thread.sleep(SLEEP_TIME_);

                // t=3
                log_.info("Currently t=3");
                Thread.sleep(SLEEP_TIME_);

                // t=4
                log_.info("Currently t=4");
                txn_.commit();
                txn_ = null;
                log_.log(Level.INFO, "Committed transaction");
                Thread.sleep(SLEEP_TIME_);

                // t=end
                log_.info("Thread has finished");
            }
            catch (RemoteException re) {
                log_.log(Level.WARNING, "Remote Exception", re);
            }
            catch (TransactionException te) {
                log_.log(Level.WARNING, "Transaction Exception", te);
            }
            catch (InterruptedException te) {
                log_.log(Level.WARNING, "Interrupted Exception", te);
            }
            catch (UnusableEntryException uee) {
                log_.log(Level.WARNING, "Unusable Entry: " + uee.partialEntry, uee);
                Throwable[] nested_exception = uee.nestedExceptions;

                for (int i = 0; i < nested_exception.length; i++) {
                    log_.log(Level.WARNING, "Nested exception " + i, nested_exception[i]);
                }
            }
            finally {
                if (txn_ != null) {
                    try {
                        txn_.abort();
                    }
                    catch (Exception e) {
                        log_.log(Level.WARNING, "Cannot abort transaction 1", e);
                    }
                }
            }
        }
    }

    /**
     * Implements the second thread that should take the object, sleep for a bit
     * and then commit the transaction.
     *
     * @author dominicc
     */
    private class TestThreadTwo extends Thread {
        private ServerTransaction txn_ = null;

        private BlockEntry entryTestTmpl_ = null;

        public TestThreadTwo(BlockEntry entryTestTmpl) {
            entryTestTmpl_ = entryTestTmpl;
        }

        public void run() {
            try {
                txn_ = getTransaction();

                // t=1
                log_.info("Currently t=1");
                BlockEntry entrySpace = (BlockEntry) space_.take(entryTestTmpl_,
                    txn_,
                    Lease.FOREVER);
                log_.log(Level.INFO, "Taken test entry: " + entrySpace);
                Thread.sleep(SLEEP_TIME_);

                // t=2
                log_.info("Currently t=2");
                Thread.sleep(SLEEP_TIME_);

                // t=3
                log_.info("Currently t=3");

                // Enable to write the object back to the space, which the first
                // thread then picks up and stops blocking.
                // space_.write(entrySpace, txn_, Lease.FOREVER);
                // log_.log(Level.INFO, "Written object back to space :" + entrySpace);

                txn_.commit();
                txn_ = null;
                log_.log(Level.INFO, "Committed transaction");
                Thread.sleep(SLEEP_TIME_);

                // t=4
                log_.info("Currently t=4");
                Thread.sleep(SLEEP_TIME_);

                // t=end
                log_.info("Thread has finished");
            }
            catch (RemoteException re) {
                log_.log(Level.WARNING, "Remote Exception", re);
            }
            catch (TransactionException te) {
                log_.log(Level.WARNING, "Transaction Exception", te);
            }
            catch (InterruptedException te) {
                log_.log(Level.WARNING, "Interrupted Exception", te);
            }
            catch (UnusableEntryException uee) {
                log_.log(Level.WARNING, "Unusable Entry: " + uee.partialEntry, uee);
                Throwable[] nested_exception = uee.nestedExceptions;

                for (int i = 0; i < nested_exception.length; i++) {
                    log_.log(Level.WARNING, "Nested exception " + i, nested_exception[i]);
                }
            }
            finally {
                if (txn_ != null) {
                    try {
                        txn_.abort();
                    }
                    catch (Exception e) {
                        log_.log(Level.WARNING, "Cannot abort transaction 1", e);
                    }
                }
            }
        }
    }
}
