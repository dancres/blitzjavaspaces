package org.dancres.blitz.remote;

import java.io.IOException;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;
import java.rmi.NoSuchObjectException;

import java.rmi.activation.ActivationID;
import java.rmi.activation.ActivationException;
import java.rmi.activation.ActivationSystem;
import java.rmi.activation.Activatable;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.net.InetSocketAddress;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import net.jini.security.ProxyPreparer;
import net.jini.security.TrustVerifier;

import net.jini.security.proxytrust.ServerProxyTrust;

import net.jini.lookup.JoinManager;

import net.jini.config.ConfigurationException;
import net.jini.config.Configuration;

import net.jini.export.ProxyAccessor;
import net.jini.export.Exporter;

import net.jini.activation.ActivationExporter;
import net.jini.activation.ActivationGroup;

import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;

import net.jini.jeri.tcp.TcpServerEndpoint;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.TransactionException;

import net.jini.core.transaction.server.TransactionManager;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import net.jini.core.discovery.LookupLocator;

import net.jini.core.entry.Entry;

import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryManagement;

import net.jini.space.JavaSpace;

import com.sun.jini.start.LifeCycle;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseBounds;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.notify.RemoteEventDispatcher;

import org.dancres.blitz.Logging;
import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.RegTicket;
import org.dancres.blitz.WriteTicket;
import org.dancres.blitz.EntryChit;
import org.dancres.blitz.EntryView;

import org.dancres.blitz.txn.StoragePersonalityFactory;
import org.dancres.blitz.txn.StoragePersonality;

import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.Switch;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.SwitchSettings;

import org.dancres.blitz.remote.view.EntryViewFactory;
import org.dancres.blitz.remote.view.ViewRegistration;
import org.dancres.blitz.remote.view.EntryViewUID;
import org.dancres.blitz.remote.txn.TransactionManagerImpl;
import org.dancres.blitz.remote.user.ColocatedAgent;

/**
   <p> The remote layer implementation for Blitz supporting both transient and
   activatable modes of operation.  Needs to implement ProxyAccessor to
   provide a means of accessing it's core remote ref for the purposes of
   converting this remote service implementation to a stub at appropriate
   moments in the activation process </p>.

   <p> We need to publish a proxy for the Javaspace itself.  If we implement
   Administrable, this proxy should call back to the server's getAdmin() to
   recovery an admin proxy which contains a remote stub to the admin methods.
   The final proxy, for transactionparticipant can actually be a naked ref,
   not even a proxy but this would allow immediate access to the other
   interfaces of the stub which could be bad. </p>

   <p> JoinAdmin should be implemented by the admin proxy and the server
   back end.  Storage/manipulation of the JoinManager should be delegated to
   a descendent of the JINIExporter LookupStorage code which should store
   it's data in a Blitz meta registry. This means no configuration required
   and keeps all the meta data in one neat place.</p>
 */
public class BlitzServiceImpl implements ServerProxyTrust,
                                         ProxyAccessor, BlitzServer {

    static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote", Level.INFO);

    private static BlitzServiceImpl theServiceImpl;

    private ActivationID theActivationID;
    private ActivationSystem theActivationSystem;

    private LookupStorage theLookupStore;

    private Exporter theExporter;

    private BlitzServer theStub;

    private BlitzProxy theSpaceProxy;

    private JoinManager theJoinManager;

    private TxnGatewayImpl theTxnGate;

    private SpaceImpl theSpace;

    private AdminProxy theAdminProxy;

    private TxnParticipantProxy theTxnProxy;

    private LifeCycle theLifecycle;

    private boolean isStopped = false;

    private LoginContext theLoginContext;

    private boolean doCompatDestroy = false;

    private TransactionManagerImpl theLoopbackTxnMgr;

    /**
       This constructor is required to use
       com.sun.jini.start.NonActivatableServiceDescriptor.
     */
    public BlitzServiceImpl(String [] anArgs, LifeCycle aLifeCycle)
        throws RemoteException, ConfigurationException {

        try {
            theLifecycle = aLifeCycle;
            init(anArgs, true);
        } catch (ConfigurationException aCE) {
            destroyImpl(false);
            throw aCE;
        } catch (RemoteException anRE) {
            destroyImpl(false);
            throw anRE;
        }

        /*
           Keep a static reference to ourselves in order to avoid premature
           GC
        */
        theServiceImpl = this;
    }

    /**
       This constructor is required to use
       com.sun.jini.start.SharedActivatableServiceDescriptor which invokes
       on ActivateWrapper which will call this constructor.
       SharedActivatabkeServiceDescriptor's serverConfigArgs are wrapped
       into a MarshalledObject and pass in with the ActivationID.
     */
    public BlitzServiceImpl(ActivationID anActivationID,
                            MarshalledObject aData)
        throws RemoteException, ConfigurationException {

        theActivationID = anActivationID;

        String[] myArgs = null;

        try {
            myArgs = (String[]) aData.get();
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to unpacked marshalled args",
                          anE);
            throw new RemoteException("Failed to unpack marshalled args");
        }

        try {
            init(myArgs, true);
        } catch (ConfigurationException aCE) {
            destroyImpl(false);
            throw aCE;
        } catch (RemoteException anRE) {
            destroyImpl(false);
            throw anRE;
        }

        theServiceImpl = this;
    }

    /**
     * Use this to construct a local instance of blitz.  When you have
     * completed your usage of this local instance invoke either:
     *
     * <ul>
     * <li>Shutdown - which will cleanly shutdown the instance retaining state
     * (assuming you're running blitz in persistent mode).</li>
     * <li>Destroy - which will cleanly shutdown the instance whilst discarding
     * state as defined in the Jini DestroyAdmin specifications (note you can
     * cause destroy to behave similarly to shutdown via appropriate
     * configuration.</li>
     * </ul>
     */
    public BlitzServiceImpl() throws RemoteException, ConfigurationException {
        init(new String[] {}, false);
    }

    private void init(String[] anArgs, boolean doExport)
        throws ConfigurationException, RemoteException {

        if (doExport) {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());
        }

        if ((anArgs != null) && (anArgs.length > 0)) {
            ConfigurationFactory.setup(anArgs);
        }

        try {
            doCompatDestroy =
                ((Boolean) ConfigurationFactory.getEntry("compliantDestroy",
                                                         Boolean.class,
                                                         new Boolean(false))).booleanValue();

            theLoginContext = (LoginContext)
                ConfigurationFactory.getEntry("loginContext",
                                              LoginContext.class, null);

            if (theLoginContext != null) {
                try {
                    theLoginContext.login();
                } catch (LoginException aLE) {
                    theLogger.log(Level.SEVERE, "Couldn't login", aLE);
                    throw new ConfigurationException("Login invalid", aLE);
                }
            }

            doPriv(new PrivilegedInitImpl(doExport));
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Oops privilege problem?", anE);
            throw new ConfigurationException("loginContext has insufficient privileges", anE);
        }
    }

    private Object doPriv(PrivilegedExceptionAction aPAE)
        throws Exception {

        if (theLoginContext != null) {
            return Subject.doAsPrivileged(theLoginContext.getSubject(),
                                          aPAE, null);
        } else
            return aPAE.run();
    }

    private class PrivilegedInitImpl implements PrivilegedExceptionAction {
        private boolean doExport;

        PrivilegedInitImpl(boolean shouldExport) {
            doExport = shouldExport;
        }

        public Object run() throws Exception {
            try {
                initImpl(doExport);
            } catch (Exception anE) {
                theLogger.log(Level.SEVERE, "Could init with subject", anE);
                throw anE;
            }
            return null;
        }
    }

    private void initImpl(boolean doExport)
        throws ConfigurationException, RemoteException {

        /*
          Create space with a TxnGatewayImpl
          Once we've got as far as export'ing, invoke setParticipantStub
          on TxnGatewayImpl so it can bundle things up appropriately.
         */

        // Make sure storage is able to initialize before we do anything
        //
        StoragePersonalityFactory.getPersonality();

        theLookupStore = new LookupStorage();
        theLookupStore.init(ConfigurationFactory.getConfig());

        Configuration myConfig = ConfigurationFactory.getConfig();

        Exporter myDefaultExporter =
            new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                  new BasicILFactory(), false, true);

        if (theActivationID == null) {
            theExporter =
                (Exporter) ConfigurationFactory.getEntry("serverExporter",
                                                         Exporter.class,
                                                         myDefaultExporter);
        } else {
            ProxyPreparer myIDPreparer =
                ConfigurationFactory.getPreparer("activationIdPreparer");

            ProxyPreparer mySysPreparer =
                ConfigurationFactory.getPreparer("activationSysPreparer");

            theActivationID =
                (ActivationID) myIDPreparer.prepareProxy(theActivationID);

            try {
                theActivationSystem =
                    (ActivationSystem) mySysPreparer.prepareProxy(ActivationGroup.getSystem());
            } catch (ActivationException anAE) {
                throw new RemoteException("Unable to locate ActivationSystem");
            }

            ActivationExporter myDefActExp =
                new ActivationExporter(theActivationID, myDefaultExporter);

            theExporter =
                (Exporter) ConfigurationFactory.getEntry("serverExporter",
                                                         Exporter.class,
                                                         myDefActExp,
                                                         theActivationID);
        }

        if (theExporter == null) {
            throw new ConfigurationException("serverExporter must be non-null if it's specified");
        }

        theLogger.log(Level.INFO, "Using exporter: " + theExporter);

        if (doExport)
            theStub = (BlitzServer) theExporter.export(this);
        else {
            // Don't need this - so clear it down....
            //
            theExporter = null;

            theStub = this;
        }

        Class[] myInterfaces = theStub.getClass().getInterfaces();

        for (int i = 0; i < myInterfaces.length; i++) {
            theLogger.log(Level.INFO,
                    "Stub supports: " + myInterfaces[i].getName());
        }

        theAdminProxy =
            ProxyFactory.newAdminProxy(theStub, theLookupStore.getUuid());

        theSpaceProxy =
            ProxyFactory.newBlitzProxy(theStub, theLookupStore.getUuid());

        theTxnProxy =
            ProxyFactory.newTxnParticipantProxy(theStub,
                                                theLookupStore.getUuid());
        theTxnGate =
            new TxnGatewayImpl(theTxnProxy);

        RemoteEventDispatcher.setSource(theSpaceProxy);

        try {
            theSpace = new SpaceImpl(theTxnGate);

            // Now run the user initializers
            //
            ColocatedAgent[] myInitializers =
                    (ColocatedAgent[])
                            ConfigurationFactory.getEntry(
                                    "agents", ColocatedAgent[].class,
                                    new ColocatedAgent[0]);

            for (int i = 0; i < myInitializers.length; i++) {
                myInitializers[i].init(theSpaceProxy);
            }

        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to start space", anE);
            throw new RemoteException("Failed to start space", anE);
        }

        if (doExport) {
            theLogger.log(Level.INFO, "Space core is up, starting JoinManager");
            
            try {
                theJoinManager = new JoinManager(theSpaceProxy,
                    theLookupStore.getAttributes(),
                    theLookupStore.getServiceID(),
                    theLookupStore.getDiscMgt(),
                    null,
                    myConfig);
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE, "Failed to init JoinManager",
                    anIOE);

                throw new RemoteException("Failed to init JoinManager",
                    anIOE);
            }
        }

        try {
            theLoopbackTxnMgr =
                new TransactionManagerImpl(theStub, theLookupStore.getUuid());
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to init loopback txn manager",
                anIOE);
        }
    }

    public JavaSpace getSpaceProxy() {
        return theSpaceProxy;
    }

    public SpaceImpl getSpace() {
        return theSpace;
    }

    public LeaseImpl write(MangledEntry anEntry, Transaction aTxn,
                           long aLeaseTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            WriteTicket myTicket = theSpace.write(anEntry, aTxn, aLeaseTime);

            return
                ProxyFactory.newEntryLeaseImpl(theStub, theLookupStore.getUuid(),
                                               myTicket.getUID(),
                                               myTicket.getExpirationTime());
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public MangledEntry take(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            return theSpace.take(anEntry, aTxn, aWaitTime);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            return theSpace.read(anEntry, aTxn, aWaitTime);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public MangledEntry takeIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            return theSpace.takeIfExists(anEntry, aTxn, aWaitTime);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public MangledEntry readIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            return theSpace.readIfExists(anEntry, aTxn, aWaitTime);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public EventRegistration notify(MangledEntry anEntry, Transaction aTxn,
                                    RemoteEventListener aListener,
                                    long aLeaseTime,
                                    MarshalledObject aHandback)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            RegTicket myTicket =
                theSpace.notify(anEntry, aTxn, aListener, aLeaseTime,
                                aHandback);

            LeaseImpl myLease =
                ProxyFactory.newLeaseImpl(theStub, theLookupStore.getUuid(),
                                          myTicket.getUID(),
                                          myTicket.getExpirationTime());

            return new EventRegistration(myTicket.getSourceId(), theSpaceProxy,
                                         myLease, myTicket.getSeqNum());
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    /* *********************************************************************
     * ProxyAccessor stuff
     ******************************************************************** */

    public Object getProxy() {
        return theStub;
    }

    /* *********************************************************************
     * ServerProxyTrust stuff
     ******************************************************************** */

    /**
       <p> Blitz can be run as a secure service, in which case, it exports
       various ConstrainedProxy instances which support RemoteMethodControl.
       Such proxy instances are exported only when the original stub supports
       RemoteMethodControl. </p>

       <p> As Blitz uses custom proxies as opposed to JERI stubs, we must
       do some additional work to support TrustVerification.  Assuming the
       stub has been generated using ProxyTrustILFactory it supports the
       bootstrap step.  For bootstrap to work, the remote server must support
       ServerProxyTrust which will be invoked as part of the bootstrap process
       supported in the stub. </p>
     */
    public TrustVerifier getProxyVerifier() throws RemoteException {
        stopBarrier();

        return new ProxyVerifier(theStub, theLookupStore.getUuid());
    }

    /* *********************************************************************
     * Transaction participant stuff
     ******************************************************************** */

    public int prepare(TransactionManager mgr, long id)
        throws UnknownTransactionException, RemoteException {

        stopBarrier();

        return theSpace.getTxnControl().prepare(mgr, id);
    }

    public void commit(TransactionManager mgr, long id)
        throws UnknownTransactionException, RemoteException {

        stopBarrier();

        theSpace.getTxnControl().commit(mgr, id);
    }

    public void abort(TransactionManager mgr, long id)
        throws UnknownTransactionException, RemoteException {

        stopBarrier();

        theSpace.getTxnControl().abort(mgr, id);
    }

    public int prepareAndCommit(TransactionManager mgr, long id)
        throws UnknownTransactionException, RemoteException {

        stopBarrier();

        return theSpace.getTxnControl().prepareAndCommit(mgr, id);
    }

    /* *********************************************************************
     * Landlord
     ******************************************************************** */
    public long renew(SpaceUID aUID, long aDuration)
        throws UnknownLeaseException, LeaseDeniedException, RemoteException {

        // System.out.println("Renew: " + aUID + ", " + aDuration);

        stopBarrier();

        try {
            long myResult = theSpace.getLeaseControl().renew(aUID, aDuration);

            // System.out.println("Renew: " + myResult);

            return myResult;
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "Space has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, RemoteException {

        stopBarrier();

        try {
            theSpace.getLeaseControl().cancel(aUID);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "Space has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public LeaseResults renew(SpaceUID[] aLeases, long[] aDurations)
        throws RemoteException {

        stopBarrier();

        long[] myNewDurations = new long[aLeases.length];
        Exception[] myFails = null;

        for (int i = 0; i < aLeases.length; i++) {
            try {
                myNewDurations[i] =
                    theSpace.getLeaseControl().renew(aLeases[i],
                                                     aDurations[i]);
            } catch (LeaseDeniedException anLDE) {
                if (myFails == null)
                    myFails = initFails(aLeases.length, i);

                myNewDurations[i] = -1;
                myFails[i] = anLDE;
            } catch (UnknownLeaseException anULE) {
                if (myFails == null)
                    myFails = initFails(aLeases.length, i);

                myNewDurations[i] = -1;
                myFails[i] = anULE;
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE, "Space has disk problems", anIOE);
                throw new RemoteException("Space has disk problems", anIOE);
            }
        }

        return new LeaseResults(myNewDurations, myFails);
    }

    public LeaseResults cancel(SpaceUID[] aLeases)
        throws RemoteException {

        stopBarrier();

        Exception[] myFails = null;

        for (int i = 0; i < aLeases.length; i++) {
            try {
                theSpace.getLeaseControl().cancel(aLeases[i]);
            } catch (UnknownLeaseException aULE) {
                if (myFails == null)
                    myFails = initFails(aLeases.length, i);

                myFails[i] = aULE;
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE, "Space has disk problems", anIOE);
                throw new RemoteException("Space has disk problems", anIOE);
            }
        }

        if (myFails != null)
            return new LeaseResults(null, myFails);
        else
            return null;
    }

    private Exception[] initFails(int aLength, int anOffset) {
        return new Exception[aLength];
    }

    /* *********************************************************************
     * ServiceProxyAccessor
     ******************************************************************** */

    public Object getServiceProxy() throws RemoteException {
        stopBarrier();

        return theSpaceProxy;
    }

    /* *********************************************************************
     * Administrable::getAdmin support
     ******************************************************************** */

    public Object getAdmin() throws RemoteException {
        stopBarrier();

        return theAdminProxy;
    }

    /* *********************************************************************
     * JoinAdmin
     ******************************************************************** */
    /**
     * Get the current attribute sets for the service. 
     * 
     * @return the current attribute sets for the service
     * @throws java.rmi.RemoteException
     */
    public Entry[] getLookupAttributes() throws RemoteException {
        stopBarrier();

        return theJoinManager.getAttributes();
    }

    /**
     * Add attribute sets for the service.  The resulting set will be used
     * for all future joins.  The attribute sets are also added to all 
     * currently-joined lookup services.
     *
     * @param attrSets the attribute sets to add
     * @throws java.rmi.RemoteException
     */
    public void addLookupAttributes(Entry[] attrSets) throws RemoteException {
        stopBarrier();

        theJoinManager.addAttributes(attrSets);
        syncStore();
    }

    /**
     * Modify the current attribute sets, using the same semantics as
     * ServiceRegistration.modifyAttributes.  The resulting set will be used
     * for all future joins.  The same modifications are also made to all 
     * currently-joined lookup services.
     *
     * @param attrSetTemplates the templates for matching attribute sets
     * @param attrSets the modifications to make to matching sets
     * @throws java.rmi.RemoteException
     *     
     * @see net.jini.core.lookup.ServiceRegistration#modifyAttributes
     */
    public void modifyLookupAttributes(Entry[] attrSetTemplates,
                                       Entry[] attrSets)
        throws RemoteException {
        stopBarrier();

        theJoinManager.modifyAttributes(attrSetTemplates, attrSets);
        syncStore();
    }

    private DiscoveryGroupManagement getDGM() {
        return (DiscoveryGroupManagement) theJoinManager.getDiscoveryManager();
    }

    /**
     * Get the list of groups to join.  An empty array means the service
     * joins no groups (as opposed to "all" groups).
     *
     * @return an array of groups to join. An empty array means the service
     *         joins no groups (as opposed to "all" groups).
     * @throws java.rmi.RemoteException
     * @see #setLookupGroups
     */
    public String[] getLookupGroups() throws RemoteException {
        stopBarrier();

        return getDGM().getGroups();
    }

    /**
     * Add new groups to the set to join.  Lookup services in the new
     * groups will be discovered and joined.
     *
     * @param groups groups to join
     * @throws java.rmi.RemoteException
     * @see #removeLookupGroups
     */
    public void addLookupGroups(String[] groups) throws RemoteException {
        stopBarrier();

        try {
            getDGM().addGroups(groups);
        } catch (IOException anIOE) {
            throw new RemoteException("Got IOE changing groups", anIOE);
        }
        syncStore();
    }

    /**
     * Remove groups from the set to join.  Leases are cancelled at lookup
     * services that are not members of any of the remaining groups.
     *
     * @param groups groups to leave
     * @throws java.rmi.RemoteException
     * @see #addLookupGroups
     */
    public void removeLookupGroups(String[] groups) throws RemoteException {
        stopBarrier();

        getDGM().removeGroups(groups);
        syncStore();
    }

    /**
     * Replace the list of groups to join with a new list.  Leases are
     * cancelled at lookup services that are not members of any of the
     * new groups.  Lookup services in the new groups will be discovered
     * and joined.
     *
     * @param groups groups to join
     * @throws java.rmi.RemoteException
     * @see #getLookupGroups
     */
    public void setLookupGroups(String[] groups) throws RemoteException {
        stopBarrier();

        try {
            getDGM().setGroups(groups);
        } catch (IOException anIOE) {
            throw new RemoteException("Got IOE changing groups", anIOE);
        }
        syncStore();
    }

    private DiscoveryLocatorManagement getDLM() {
        return (DiscoveryLocatorManagement)
            theJoinManager.getDiscoveryManager();
    }

    /**
     *Get the list of locators of specific lookup services to join. 
     *
     * @return the list of locators of specific lookup services to join
     * @throws java.rmi.RemoteException
     * @see #setLookupLocators
     */
    public LookupLocator[] getLookupLocators() throws RemoteException {
        stopBarrier();

        return getDLM().getLocators();
    }

    /**
     * Add locators for specific new lookup services to join.  The new
     * lookup services will be discovered and joined.
     *
     * @param locators locators of specific lookup services to join
     * @throws java.rmi.RemoteException
     * @see #removeLookupLocators
     */
    public void addLookupLocators(LookupLocator[] locators)
        throws RemoteException {
        stopBarrier();

        getDLM().addLocators(locators);
        syncStore();
    }

    /**
     * Remove locators for specific lookup services from the set to join.
     * Any leases held at the lookup services are cancelled.
     *
     * @param locators locators of specific lookup services to leave
     * @throws java.rmi.RemoteException
     * @see #addLookupLocators
     */
    public void removeLookupLocators(LookupLocator[] locators)
        throws RemoteException {
        stopBarrier();

        getDLM().removeLocators(locators);
        syncStore();
    }

    /**
     * Replace the list of locators of specific lookup services to join
     * with a new list.  Leases are cancelled at lookup services that were
     * in the old list but are not in the new list.  Any new lookup services
     * will be discovered and joined.
     *
     * @param locators locators of specific lookup services to join
     * @throws java.rmi.RemoteException
     * @see #getLookupLocators
     */
    public void setLookupLocators(LookupLocator[] locators)
        throws RemoteException {
        stopBarrier();

        getDLM().setLocators(locators);
        syncStore();
    }

    private void syncStore() throws RemoteException {
        try {
            theLookupStore.sync(theJoinManager);
        } catch (IOException anIOE) {
            throw new RemoteException("Couldn't update saved state", anIOE);
        }
    }


    /* *********************************************************************
   * DestroyAdmin
   ******************************************************************** */

    public void destroy() throws RemoteException {
        destroyImpl(doCompatDestroy);
    }

    private void destroyImpl(boolean doCleanup) throws RemoteException {
        stopBarrier();

        blockCalls();

        // Load cleanup variable from config and construct accordingly
        Thread myDestroyer = new DestroyThread(doCleanup);
        myDestroyer.setDaemon(false);
        myDestroyer.start();
    }

    private void blockCalls() {
        synchronized(this) {
            isStopped = true;
        }
    }

    private void unblockCalls() {
        synchronized(this) {
            isStopped = false;
        }
    }

    /**
       Used to check whether calls are blocked and, if they are, throws
       a RemoteException back to the client.
     */
    private void stopBarrier() throws RemoteException {
        synchronized(this) {
            if (isStopped)
                throw new RemoteException("Remote calls not permitted");
        }
    }

    private class DestroyThread extends Thread {
        private boolean doClean;

        private DestroyThread(boolean shouldClean) {
            super("DestroyThread");
            doClean = shouldClean;
        }

        public void run() {
            theLogger.log(Level.SEVERE, "Shutdown in progress");

            if (theLoopbackTxnMgr != null) {
                theLogger.log(Level.SEVERE, "Stop LoopbackTxnMgr");
                theLoopbackTxnMgr.terminate();
            }

            if (theJoinManager != null) {
                theLogger.log(Level.SEVERE, "Stop JoinManager");
                DiscoveryManagement myDGM = theJoinManager.getDiscoveryManager();

                theJoinManager.terminate();
                myDGM.terminate();
            }

            if (theActivationID != null) {
                theLogger.log(Level.SEVERE,
                              "De-registering from ActivationSystem");

                try {
                    theActivationSystem.unregisterObject(theActivationID);
                } catch (Exception anAE) {
                    theLogger.log(Level.SEVERE, "Activation unregister failed",
                                  anAE);
                }
            }

            if ((theStub != null) && (theExporter != null)) {
                theLogger.log(Level.SEVERE, "Unexport - forced");
                theExporter.unexport(true);
            }

            if (theSpace != null) {
                try {
                    theLogger.log(Level.SEVERE, "Stopping space");
                    theSpace.stop();
                } catch (Exception anE) {
                    theLogger.log(Level.SEVERE, "Space failed to shutdown cleanly",
                                  anE);
                }
            }

            if (theActivationID != null) {
                theLogger.log(Level.SEVERE, "Notifying ActivationGroup");

                try {
                    Activatable.inactive(theActivationID);
                } catch (Exception anE) {
                    theLogger.log(Level.SEVERE, "ActivationGroup complained",
                                  anE);
                }
            }

            if (theLifecycle != null) {
                theLogger.log(Level.SEVERE, "Lifecycle::unregister");
                theLifecycle.unregister(theServiceImpl);
            }

            if (theLoginContext != null) {
                try {
                    theLogger.log(Level.SEVERE, "Logout");
                    theLoginContext.logout();
                } catch (LoginException aLE) {
                    theLogger.log(Level.SEVERE, "Couldn't logout", aLE);
                }
            }

            theLogger.log(Level.SEVERE, "Shutdown complete");

            /*
              Check config and optionally invoke Disk::clear on
              appropriate directories
            */
            if (doClean) {
                StoragePersonality myPersonality =
                    StoragePersonalityFactory.getPersonality();

                /*
                  It's possible that config is broken and thus we are
                  performing destroy.  If config *is* broken, we may not
                  be able to create a personality.....
                */
                if (myPersonality != null) {
                    theLogger.log(Level.SEVERE, "Erasing disk state");

                    myPersonality.destroy();
                }
            }
        }
    }

    /* *********************************************************************
     * StatsAdmin
     ******************************************************************** */

    public Stat[] getStats() throws RemoteException {
        stopBarrier();

        return StatsBoard.get().getStats();
    }

    public void setSwitches(Switch[] aListOfSwitches) throws RemoteException {
        stopBarrier();

        SwitchSettings.get().update(aListOfSwitches);
    }

    /* *********************************************************************
     * EntryViewAdmin
     ******************************************************************** */
    public JavaSpace getJavaSpaceProxy() throws RemoteException {
        return theSpaceProxy;
    }

    public ViewResult newView(MangledEntry[] aTemplates, Transaction aTxn,
                              long aLeaseDuration, boolean isJavaSpace05,
                              long aLimit, int anInitialChunk)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            long myDuration = aLeaseDuration;

            /*
              Bound the lease if we're instructed to do so - we may not
              be required to if this is an emulation of the old
              JavaSpaceAdmin::contents call
             */
            if (isJavaSpace05)
                myDuration = LeaseBounds.boundView(aLeaseDuration);

            /*
            * JavaSpace05 holds locks only for a non-null transaction.
            * JavaSpaceAdmin never holds locks but uses the transaction to
            * test visibility
            */
            boolean holdLocks =
                (isJavaSpace05) ? (aTxn != null) : false;

            /*
              If we're being asked to do JavaSpace05, we're performing the new contents
              operation which means we should pass true for holdlocks, false otherwise
             */
            ViewRegistration myReg =
                EntryViewFactory.get().newView(aTemplates, aTxn, holdLocks,
                                               myDuration, aLimit, theSpace);

            /*
                Okay, got a view, now we need an initial batch
             */
            EntryChit[] myInitialBatch = getNext(myReg.getUID(), anInitialChunk);

            return new ViewResult(
                    ProxyFactory.newLeaseImpl(theStub, theLookupStore.getUuid(),
                            myReg.getUID(),
                            myReg.getExpiry()),
                    myInitialBatch);
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to create view", anIOE);
        }
    }

    public EntryChit[] getNext(EntryViewUID aUid, int aChunkSize)
        throws RemoteException {

        stopBarrier();

        ArrayList myNext = new ArrayList();

        EntryView myView = EntryViewFactory.get().getView(aUid);

        /*
         View could have expired or been otherwise lost
        */
        if (myView == null)
            throw new NoSuchObjectException("View has been lost");

        try {
            for (int i = 0; i < aChunkSize; i++) {
                EntryChit myChit = myView.next();

                if (myChit == null)
                    break;
                else
                    myNext.add(myChit);
            }
        } catch (TransactionException aTE) {
            throw new RemoteException("View was prematurely destroyed - was a transaction ended?", aTE);
        } catch (IOException anIOE) {
            throw new RemoteException("Couldn't recover an Entry", anIOE);
        }

        if (myNext.size() == 0)
            return null;

        EntryChit[] myChits = new EntryChit[myNext.size()];
        myChits = (EntryChit[]) myNext.toArray(myChits);

        return myChits;
    }

    /**
       Deletes a specific entity returned from an 
       <code>EntryView</code> via an <code>EntryChit</code>
     */
    public void delete(Object aCookie) throws RemoteException {
        stopBarrier();

        try {
            theSpace.getLeaseControl().cancel((SpaceUID) aCookie);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "Space has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        } catch (UnknownLeaseException aULE) {
            // It's gone already, fail silently
        }
    }

    public void close(EntryViewUID aUid) throws RemoteException {
        stopBarrier();

        EntryViewFactory.get().delete(aUid);
    }

    /* *********************************************************************
   * JavaSpace05
   ******************************************************************** */

    public List write(List aMangledEntries, Transaction aTxn, List aLeaseTimes)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            List myTickets = theSpace.write(aMangledEntries, aTxn, aLeaseTimes);

            for (int i = 0; i < myTickets.size(); i++) {
                WriteTicket myTicket = (WriteTicket) myTickets.get(i);

                Lease myLease =
                    ProxyFactory.newEntryLeaseImpl(theStub, theLookupStore.getUuid(),
                                                   myTicket.getUID(),
                                                   myTicket.getExpirationTime());

                myTickets.set(i, myLease);
            }

            return myTickets;
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public List take(MangledEntry[] aTemplates, Transaction aTxn,
                     long aWaitTime, long aLimit)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            return theSpace.take(aTemplates, aTxn, aWaitTime, aLimit);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    public EventRegistration
        registerForVisibility(MangledEntry[] aTemplates, Transaction aTxn,
                              RemoteEventListener aListener, long aLeaseTime,
                              MarshalledObject aHandback,
                              boolean visibilityOnly)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            RegTicket myTicket =
                theSpace.visibility(aTemplates, aTxn, aListener, aLeaseTime,
                                    aHandback, visibilityOnly);

            LeaseImpl myLease =
                ProxyFactory.newLeaseImpl(theStub, theLookupStore.getUuid(),
                                          myTicket.getUID(),
                                          myTicket.getExpirationTime());

            return new EventRegistration(myTicket.getSourceId(), theSpaceProxy,
                                         myLease, myTicket.getSeqNum());
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

    /* *********************************************************************
     * BlitzAdmin
     ******************************************************************** */
    public void requestSnapshot() throws RemoteException,
                                         TransactionException, IOException {
        stopBarrier();
        theSpace.getTxnControl().requestSnapshot();
    }

    public void shutdown() throws RemoteException {
        // stopBarrier is donw in destroyImpl
        destroyImpl(false);
    }

    public void backup(String aDir) throws RemoteException, IOException {
        stopBarrier();
        theSpace.getTxnControl().backup(aDir);
    }

    public void clean() throws RemoteException, IOException {
        stopBarrier();

        blockCalls();

        try {
            theSpace.empty();
        } finally {
            unblockCalls();
        }
    }

    public void reap() throws RemoteException {
        theSpace.reap();
    }
}
