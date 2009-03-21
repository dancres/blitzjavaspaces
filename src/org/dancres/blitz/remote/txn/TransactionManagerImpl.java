package org.dancres.blitz.remote.txn;

import java.rmi.RemoteException;
import java.io.IOException;

import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.server.TransactionParticipant;
import net.jini.core.transaction.server.CrashCountException;
import net.jini.core.transaction.*;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.discovery.LookupLocator;
import net.jini.config.ConfigurationException;
import net.jini.config.NoSuchEntryException;
import net.jini.export.Exporter;
import net.jini.lookup.JoinManager;
import net.jini.lookup.ServiceIDListener;
import net.jini.lookup.entry.ServiceInfo;
import net.jini.lookup.entry.Name;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.id.Uuid;

import com.sun.jini.lookup.entry.BasicServiceType;

import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.VersionInfo;
import org.dancres.blitz.remote.BlitzServer;
import org.dancres.blitz.remote.ProxyFactory;

/**
 */
public class TransactionManagerImpl implements TransactionManager,
    ServiceIDListener {

    private JoinManager theJoinManager;
    private Exporter theExporter;
    private TransactionManager theStub;
    private TransactionManager theProxy;

    private String[] theGroups;
    private Entry[] theAttributes;
    private LookupLocator[] theLocators;

    private BlitzServer theLandlord;
    private Uuid theLandlordUuid;

    public TransactionManagerImpl(BlitzServer aLandlord, Uuid aLandlordUuid)
        throws ConfigurationException, IOException {
        theExporter =
            ((Exporter) ConfigurationFactory.getEntry("loopbackTxnExporter",
                Exporter.class,
                null));

        theGroups = (String[])
            ConfigurationFactory.getEntry("initialGroups", String[].class,
                null);

        theLocators =
            (LookupLocator[])
                ConfigurationFactory.getEntry("initialLocators",
                    LookupLocator[].class,
                    new LookupLocator[0]);

        String myName = null;

        try {
            myName =
                (String)
                    ConfigurationFactory.getEntry("name", String.class);
        } catch (NoSuchEntryException aNSEE) {
            // Doesn't matter
        }

        theAttributes = getDefaultAttrs(myName);

        if (theExporter == null) {
            // No exporter means we're not active
            //
            return;
        }

        theStub = (TransactionManager) theExporter.export(this);

        LookupDiscoveryManager myLDM =
            new LookupDiscoveryManager(theGroups, theLocators, null);

        theLandlord = aLandlord;
        theLandlordUuid = aLandlordUuid;

        theProxy = new TxnMgrProxy(theStub, theLandlordUuid);

        LoopBackMgr.init(theProxy);
        
        theJoinManager = new JoinManager(theProxy, theAttributes, this,
            myLDM, null, ConfigurationFactory.getConfig());
    }

    public void terminate() {
        if (theJoinManager != null) {
            theJoinManager.terminate();
            theExporter.unexport(true);
        }
    }

    public Created create(long leaseTime) throws LeaseDeniedException,
        RemoteException {

        TxnTicket myTicket = LoopBackMgr.get().create(leaseTime);

        Lease myLease =
            ProxyFactory.newLeaseImpl(theLandlord, theLandlordUuid,
                myTicket.getUID(), myTicket.getLeaseTime());

        return new TransactionManager.Created(myTicket.getUID().getId(), myLease);
    }

    public void join(long id, TransactionParticipant transactionParticipant, long l1)
        throws UnknownTransactionException, CannotJoinException,
            CrashCountException, RemoteException {
        throw new RemoteException("Remote participants are not supported - use Mahalo");
    }

    public int getState(long id) throws UnknownTransactionException, RemoteException {
        throw new RemoteException("Remote participants are not supported - use Mahalo");
    }

    public void commit(long id) throws UnknownTransactionException,
        CannotCommitException, RemoteException {
        LoopBackMgr.get().commit(id);
    }

    public void commit(long id, long timeout) throws UnknownTransactionException,
        CannotCommitException, TimeoutExpiredException, RemoteException {
        LoopBackMgr.get().commit(id, timeout);
    }

    public void abort(long id) throws UnknownTransactionException,
        CannotAbortException, RemoteException {
        LoopBackMgr.get().abort(id);
    }

    public void abort(long id, long timeout) throws UnknownTransactionException,
        CannotAbortException, TimeoutExpiredException, RemoteException {
        LoopBackMgr.get().abort(id, timeout);
    }

    public void serviceIDNotify(ServiceID serviceID) {
        // Don't care
    }

    static Entry[] getDefaultAttrs(String aName) {
        Entry myInfo =
            new ServiceInfo("Blitz JavaSpaces Loopback TxnMgr",
                VersionInfo.EMAIL_CONTACT,
                VersionInfo.SUPPLIER_NAME,
                VersionInfo.VERSION, "", "");

        Entry myType = new BasicServiceType("TransactionManager");

        if (aName != null) {
            return new Entry[]{myInfo, myType, new Name(aName)};
        } else {
            return new Entry[]{myInfo, myType};
        }
    }
}
