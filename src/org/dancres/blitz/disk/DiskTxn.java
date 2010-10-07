package org.dancres.blitz.disk;

import java.io.IOException;

import java.util.logging.Level;

import com.sleepycat.je.Transaction;
import com.sleepycat.je.DatabaseException;

/**
   Disk operations are encapsulated in transactions for the purposes of
   recovery etc.  DiskTxn represents such transactions.  No DiskTxn means
   no disk update.
 */
public class DiskTxn {
    private static ThreadLocal ACTIVE_TXN = new ThreadLocal();

    private Transaction theTxn;

    private boolean amStandalone = false;

    private DiskTxn(Transaction aTxn) {
        theTxn = aTxn;
    }

    private DiskTxn(Transaction aTxn, boolean isStandalone) {
        theTxn = aTxn;
        amStandalone = isStandalone;
    }

    /**
       Create a new txn and make it part of the current thread's context.
       <B>Warning</B> if there's another Txn already associated with this
       thread (i.e. a non-standalone txn) then it will be overwritten by
       this one with the resultant loss of the old transaction which will
       remain open forever - this would be bad!  Under circumstances where
       it's necessary to nest txns, use newStandlone().
     */
    public static DiskTxn newTxn() throws IOException {
        Transaction myDbTxn;

        try {
            myDbTxn = Disk.newTxn();
        } catch (DatabaseException aDbe) {
            Disk.theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("DatabaseException");
        }

        DiskTxn myTxn = new DiskTxn(myDbTxn);

        ACTIVE_TXN.set(myTxn);

        return myTxn;
    }

    /**
       Create a new txn but don't make it a part of the current thread's
       context.
     */
    public static DiskTxn newStandalone() throws IOException {
        Transaction myDbTxn;
        
        try {
            myDbTxn = Disk.newTxn();
        } catch (DatabaseException aDbe) {
            Disk.theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("DatabaseException");
        }

        DiskTxn myTxn = new DiskTxn(myDbTxn, true);

        return myTxn;
    }

    /**
       If you choose to use a non-blocking transaction, you must be prepared
       to handle (for Db) <code>DatabaseException</code> with an errno of
       <code>Db.DB_LOCK_NOTGRANTED</code> during any operations performed under
       that transaction.  This breaks encapsulation of Db a little.
       Note that Db documentation for 4.1.25 would lead a programmer to
       believe they need to handle DbLockNotGrantedException - unfortunately,
       that never seems to be thrown - sigh.

       @todo Fix API leakage.
     */
    public static DiskTxn newNonBlockingStandalone() throws IOException {
        Transaction myDbTxn;
        
        try {
            myDbTxn = Disk.newNonBlockingTxn();
        } catch (DatabaseException aDbe) {
            Disk.theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("DatabaseException");
        }

        DiskTxn myTxn = new DiskTxn(myDbTxn, true);

        return myTxn;
    }

    public static DiskTxn getActiveTxn() {
        return (DiskTxn) ACTIVE_TXN.get();
    }

    public static Transaction getActiveDbTxn() {
        return getActiveTxn().getDbTxn();
    }

    public Transaction getDbTxn() {
        return theTxn;
    }

    /**
       Commit the transaction with no forced logging.  If you want forced
       logging invoke <code>commit(true)</code>.
     */
    public void commit() throws IOException {
        commit(false);
    }

    public void commit(boolean shouldSync) throws IOException {
        try {
            if (shouldSync)
                theTxn.commitSync();
            else 
                theTxn.commitNoSync();
        } catch (DatabaseException aDbe) {
            Disk.theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("DatabaseException");
        }
            
        release();

        if (!amStandalone)
            ACTIVE_TXN.set(null);
    }

    public void abort() throws IOException {
        try {
            theTxn.abort();
        } catch (DatabaseException aDbe) {
            Disk.theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("DatabaseException");
        }
            
        release();

        if (!amStandalone)
            ACTIVE_TXN.set(null);
    }

    private void release() {
    }
}
