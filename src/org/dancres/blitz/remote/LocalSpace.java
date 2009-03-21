package org.dancres.blitz.remote;

import java.io.IOException;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.id.UuidFactory;
import net.jini.id.Uuid;

import org.dancres.blitz.*;
import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.remote.view.ViewRegistration;
import org.dancres.blitz.remote.view.EntryViewFactory;
import org.dancres.blitz.remote.view.EntryViewUID;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.Switch;
import org.dancres.blitz.stats.SwitchSettings;
import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseBounds;
import org.dancres.blitz.notify.RemoteEventDispatcher;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.StoragePersonalityFactory;

/**
   <p>An entirely local implementation of a JavaSpace using Blitz.  Useful for
   debugging where one would like client and server co-located.  An instance
   of this class, when configured with an appropriate
   <code>TxnGateway</code> instance, can provide an embedded space
   implementation. For an example, see <code>TxnStress</code></p>

   @see org.dancres.blitz.txn.TxnGateway
   @see org.dancres.blitz.test.TxnStress

   @todo Add support for lease renewal.
 */
public class LocalSpace implements BlitzServer {
    static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote.LocalSpace", Level.INFO);

    private Uuid theUuid;

    private BlitzProxy theSpaceProxy;

    private TxnGateway theTxnGate;

    private AdminProxy theAdminProxy;

    private TxnParticipantProxy theTxnProxy;

    private boolean isStopped;

    private SpaceImpl theSpace;

    public LocalSpace(TxnGateway aGateway) throws Exception {
        init(aGateway);
    }

    public LocalSpace() throws Exception {
        init(null);
    }

    public JavaSpace05 getProxy() {
        return theSpaceProxy;
    }

    private void init(TxnGateway aGateway)
            throws RemoteException, ConfigurationException {

        theUuid = UuidFactory.generate();

        StoragePersonalityFactory.getPersonality();

        theAdminProxy =
            ProxyFactory.newAdminProxy(this, theUuid);

        theSpaceProxy =
            ProxyFactory.newBlitzProxy(this, theUuid);

        theTxnProxy =
            ProxyFactory.newTxnParticipantProxy(this, theUuid);
        theTxnGate = aGateway;

        RemoteEventDispatcher.setSource(theSpaceProxy);

        try {
            theSpace = new SpaceImpl(theTxnGate);

        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to start space", anE);
            throw new RemoteException("Failed to start space", anE);
        }
    }

    public LeaseImpl write(MangledEntry anEntry, Transaction aTxn,
                       long aLeaseTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        try {
            WriteTicket myTicket = theSpace.write(anEntry, aTxn, aLeaseTime);

            return
                ProxyFactory.newEntryLeaseImpl(this, theUuid,
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
                ProxyFactory.newLeaseImpl(this, theUuid,
                                          myTicket.getUID(),
                                          myTicket.getExpirationTime());

            return new EventRegistration(myTicket.getSourceId(), theSpaceProxy,
                                         myLease, myTicket.getSeqNum());
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "SpaceImpl has disk problems", anIOE);
            throw new RemoteException("Space has disk problems", anIOE);
        }
    }

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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
    }

    private DiscoveryLocatorManagement getDLM() {
        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
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

        throw new UnsupportedOperationException();
    }

    public void destroy() throws RemoteException {
        stopBarrier();

        blockCalls();

        try {
        theSpace.stop();
        } catch (Exception anE) {
            throw new RemoteException("Failed to destroy", anE);
        }
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

    public TxnControl getTxnControl() {
        return theSpace.getTxnControl();
    }

    public void stop() throws RemoteException {
        destroy();
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
                    ProxyFactory.newLeaseImpl(this, theUuid,
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
                    ProxyFactory.newEntryLeaseImpl(this, theUuid,
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
                ProxyFactory.newLeaseImpl(this, theUuid,
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
        // stopBarrier is done in destroyImpl
        destroy();
    }

    public void backup(String aDir) throws RemoteException, IOException {
        stopBarrier();
        theSpace.getTxnControl().backup(aDir);
    }

    public void clean() throws RemoteException, IOException {
        stopBarrier();

        blockCalls();

        theSpace.empty();

        unblockCalls();
    }

    public void reap() throws RemoteException {
        theSpace.reap();
    }

    /* *********************************************************************
     * ServiceProxyAccessor
     ******************************************************************** */

    public Object getServiceProxy() throws RemoteException {
        stopBarrier();

        return theSpaceProxy;
    }
}
