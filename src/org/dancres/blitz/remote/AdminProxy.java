package org.dancres.blitz.remote;

import java.io.Serializable;
import java.io.IOException;

import java.rmi.RemoteException;

import net.jini.core.entry.Entry;

import net.jini.core.discovery.LookupLocator;

import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.Transaction;

import net.jini.core.lease.Lease;

import net.jini.space.JavaSpace;

import net.jini.admin.JoinAdmin;

import net.jini.id.Uuid;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;

import com.sun.jini.admin.DestroyAdmin;

import com.sun.jini.outrigger.JavaSpaceAdmin;
import com.sun.jini.outrigger.AdminIterator;

import org.dancres.blitz.stats.Switch;
import org.dancres.blitz.stats.Stat;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.mangler.EntryMangler;

import org.dancres.blitz.remote.view.EntryViewUID;

/**
   Handles all admin-related functions for Blitz.
 */
public class AdminProxy implements DestroyAdmin, JoinAdmin, Serializable,
                                   ReferentUuid, StatsAdmin,
                                   BlitzAdmin, JavaSpaceAdmin {
    AdminServer theStub;
    Uuid theUuid;

    AdminProxy(AdminServer anAdminStub, Uuid aUuid) {
        theStub = anAdminStub;
        theUuid = aUuid;
    }

    private synchronized EntryMangler getMangler() {
        return EntryMangler.getMangler();
    }

    public Uuid getReferentUuid() {
        return theUuid;
    }

    /** 
     * Get the current attribute sets for the service. 
     * 
     * @return the current attribute sets for the service
     * @throws java.rmi.RemoteException
     */
    public Entry[] getLookupAttributes() throws RemoteException {
        return theStub.getLookupAttributes();
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
        theStub.addLookupAttributes(attrSets);
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
        theStub.modifyLookupAttributes(attrSetTemplates, attrSets);
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
        return theStub.getLookupGroups();
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
        theStub.addLookupGroups(groups);
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
        theStub.removeLookupGroups(groups);
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
        theStub.setLookupGroups(groups);
    }

    /** 
     *Get the list of locators of specific lookup services to join. 
     *
     * @return the list of locators of specific lookup services to join
     * @throws java.rmi.RemoteException
     * @see #setLookupLocators
     */
    public LookupLocator[] getLookupLocators() throws RemoteException {
        return theStub.getLookupLocators();
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
        theStub.addLookupLocators(locators);
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
        theStub.removeLookupLocators(locators);
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
        theStub.setLookupLocators(locators);
    }    

    public void destroy() throws RemoteException {
        theStub.destroy();
    }

    public boolean equals(Object anObject) {
        return ReferentUuids.compare(this, anObject);
    }

    public Stat[] getStats() throws RemoteException {
        return theStub.getStats();
    }

    public void setSwitches(Switch[] aListOfSwitches) throws RemoteException {
        theStub.setSwitches(aListOfSwitches);
    }

    public void requestSnapshot() throws RemoteException,
                                         TransactionException, IOException {
        theStub.requestSnapshot();
    }

    public void shutdown() throws RemoteException, IOException {
        theStub.shutdown();
    }

    public void backup(String aDir) throws RemoteException, IOException {
        theStub.backup(aDir);
    }

    public void clean() throws RemoteException, IOException {
        theStub.clean();
    }

    public void reap() throws RemoteException {
        theStub.reap();
    }

   /************************************************************************
     * JavaSpaceAdmin
     ***********************************************************************/

    public JavaSpace space() throws RemoteException {
        return theStub.getJavaSpaceProxy();
    }

    public AdminIterator contents(Entry aTmpl, Transaction aTxn)
        throws TransactionException, RemoteException {

        ViewResult myResult =
            theStub.newView(new MangledEntry[] {packEntry(aTmpl)},
                aTxn, Lease.FOREVER, false, Long.MAX_VALUE,
                AdminIteratorImpl.CHUNK_SIZE);

        // We only want the UID from this lease
        EntryViewUID myViewId = (EntryViewUID) myResult.getLease().getUID();

        return new AdminIteratorImpl(getMangler(), myViewId, theStub,
                myResult.getInitialBatch());
    }

    public AdminIterator contents(Entry aTmpl, Transaction aTxn, int aFetchSize)
        throws TransactionException, RemoteException {

        ViewResult myResult =
            theStub.newView(new MangledEntry[] {packEntry(aTmpl)},
                aTxn, Lease.FOREVER, false, Long.MAX_VALUE,
                AdminIteratorImpl.CHUNK_SIZE);

        // We only want the UID from this lease
        EntryViewUID myViewId = (EntryViewUID) myResult.getLease().getUID();

        return new AdminIteratorImpl(getMangler(), myViewId,
                                     aFetchSize, theStub, myResult.getInitialBatch());
    }

    private MangledEntry packEntry(Entry anEntry) {
        if (anEntry == null)
            return MangledEntry.NULL_TEMPLATE;
        // Is it a snapshot?
        else if (anEntry instanceof MangledEntry)
            return (MangledEntry) anEntry;
        else
            return getMangler().mangle(anEntry);
    }
}
