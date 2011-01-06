package org.dancres.blitz.txn;

import java.io.Serializable;
import java.io.IOException;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.jini.security.ProxyPreparer;

import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.TransactionException;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.entry.OpInfo;

import org.dancres.blitz.config.ConfigurationFactory;

/**
   <p> A reference to transactional state being held within the transaction
   manager. </p>

   <p> This class uses txnPreparer to do initial preparation of the
   passed RemoteEventListener when constructed from scratch (i.e. it's a new
   registration).  It uses recoveredTxnPreparer in those cases where it
   has been de-serialized from storage and is about to be used for the first
   time post a restart </p>
 */
public final class TxnId implements Serializable {
    private static final ProxyPreparer RECOVERY_PREPARER;
    private static final ProxyPreparer PREPARER;

    static {
        try {
            RECOVERY_PREPARER =
                ConfigurationFactory.getPreparer("recoveredTxnPreparer");

            PREPARER =
                ConfigurationFactory.getPreparer("txnPreparer");
        } catch (ConfigurationException aCE) {
            throw new RuntimeException("TxnId has problems with preparers",
                                       aCE);
        }
    }

    static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            theMarshalledTxnMgrCache.clear();
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }


    private MarshalledObject theMarshalledMgr;
    private long theId;

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        /*
          If the manager is a remote manager we must marshall it up for serialization otherwise leave null behind
          as a marker for getManager
         */
        if (theManager != LOCAL_TXN_MGR)
            theMarshalledMgr = getMarshalledMgr(theManager);

        stream.defaultWriteObject();
    }

    private transient TransactionManager theManager;
    private transient boolean isPrepared;

    private static final LocalTxnManager LOCAL_TXN_MGR =
        new LocalTxnManager();

    private static final Map theMarshalledTxnMgrCache = new ConcurrentHashMap();

    /**
       Repeatedly marshalling the same txn mgr for writing to disk and
       such like is costly so we marshall them once and then cache them
       forever on the basis that we'll never see enough transaction manager
       refs to make the memory consumption substantial

       @todo Broken references may be a problem....
     */
    private static MarshalledObject getMarshalledMgr(TransactionManager aMgr)
        throws RemoteException {

        try {
            MarshalledObject myMarshalledMgr =
                    (MarshalledObject) theMarshalledTxnMgrCache.get(aMgr);

            if (myMarshalledMgr == null) {
                myMarshalledMgr = new MarshalledObject(aMgr);
                theMarshalledTxnMgrCache.put(aMgr, myMarshalledMgr);
            }

            return myMarshalledMgr;
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to marshall txnmgr", anIOE);
        }

    }

    static TxnId newNullTxn() throws RemoteException {
        return new TxnId(LOCAL_TXN_MGR.nextId());
    }

    /**
       Only to be used for local(null) transactions.  That's because we do
       no preparation on the txn manager reference (there's no point).
     */
    private TxnId(long anId) throws RemoteException {
        theManager = LOCAL_TXN_MGR;
        isPrepared = true;
        theId = anId;
    }

    /**
       Use this for transactions which have a remote transaction manager
     */
    TxnId(TransactionManager aMgr, long anId) throws RemoteException {
        theId = anId;
        theManager = (TransactionManager) PREPARER.prepareProxy(aMgr);
        isPrepared = true;
    }

    boolean isNull() {
        try {
            return (getManager() instanceof LocalTxnManager);
        } catch (RemoteException anRE) {
            // This will only happen if we've tried to unpack a remote
            // object reference which implicitly means it's not null
            //
            return false;
        }
    }

    public synchronized TransactionManager getManager()
        throws RemoteException {

        /*
          This flag is only set in the constructor for non-null txns or by us. It will also be unset should this
          TxnId have been de-serialized back from disk. In such a case we need to unmarshall and re-verify.
         */
        if (!isPrepared) {
            Object myManager = null;

            try {
                // Only null if it was a local txn mgr.....
                //
                if (theMarshalledMgr == null)
                    myManager = LOCAL_TXN_MGR;
                else {
                    myManager = theMarshalledMgr.get();
                    myManager = RECOVERY_PREPARER.prepareProxy(myManager);
                }
            } catch (IOException anIOE) {
                throw new RemoteException("Failed to unmarshall txnmgr",
                                          anIOE);
            } catch (ClassNotFoundException aCNFE) {
                throw new RemoteException("Failed to unmarshall txnmgr",
                                          aCNFE);
            }

            theManager = (TransactionManager) myManager;
            isPrepared = true;
        }

        return theManager;
    }

    public long getId() {
        return theId;
    }

    public String toString() {
        try {
            return getManager() + "->" + theId;
        } catch (RemoteException anRE) {
            return "Broken_Codebase->" + theId;
        }
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof TxnId) {
            TxnId myOther = (TxnId) anObject;

            if (myOther.theId != theId)
                return false;

            try {
                TransactionManager myMgr = getManager();
                TransactionManager myOtherMgr = myOther.getManager();

                return myMgr.equals(myOtherMgr);
            } catch (RemoteException anRE) {
                // Likely to happen if codebase of txnmgr ref is broken
                return false;
            }
        }

        return false;
    }

    public int hashCode() {
        return (int) (theId ^ (theId >>> 32));
    }
}

