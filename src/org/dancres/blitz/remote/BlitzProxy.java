package org.dancres.blitz.remote;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.lease.Lease;

import net.jini.entry.UnusableEntriesException;

import net.jini.lookup.entry.ServiceInfo;
import net.jini.lookup.entry.Name;

import com.sun.jini.lookup.entry.BasicServiceType;

import net.jini.admin.Administrable;

import net.jini.id.Uuid;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;

import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.VersionInfo;
import org.dancres.blitz.remote.nio.FastSpace;

/**
   The Blitz front-end proxy responsible for implementing the JavaSpace
   interface.
 */
class BlitzProxy implements Serializable, JavaSpace, JavaSpace05,
                            Administrable, ReferentUuid {

    FastSpace theFast;
    BlitzServer theStub;
    Uuid theUuid;

    BlitzProxy(BlitzServer aServer, Uuid aUuid) {
        theStub = aServer;
        theUuid = aUuid;
    }

    void enableFastIO(FastSpace aSpace) {
        theFast = aSpace;
    }

    synchronized FastSpace getFastChannel() {
        if (theFast != null) {
            if (! theFast.isInited()) {
                try {
                    theFast.init();
                } catch (IOException anIOE) {
                    throw new Error("Panic: fast io didn't init", anIOE);
                }
            }
        }

        return theFast;
    }

    public Uuid getReferentUuid() {
        return theUuid;
    }

    private EntryMangler getMangler() {
        return EntryMangler.getMangler();
    }

    private MangledEntry packEntry(Entry anEntry, boolean isWrite) {
        if ((isWrite) && (anEntry == null))
            throw new IllegalArgumentException("You cannot write a null Entry");

        if (anEntry == null)
            return MangledEntry.NULL_TEMPLATE;
        // Is it a snapshot?
        else if (anEntry instanceof MangledEntry)
            return (MangledEntry) anEntry;
        else
            return getMangler().mangle(anEntry, false);
    }

    public Lease write(Entry entry, Transaction txn, long lease)
        throws TransactionException, RemoteException {

        LeaseImpl myLease;

        if (getFastChannel() != null)
            myLease = getFastChannel().write(packEntry(entry, true), txn, lease);
        else
            myLease = theStub.write(packEntry(entry, true), txn, lease);

        myLease.setLandlord(theStub, theUuid);
        return myLease;
    }

    public Entry read(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException,
               InterruptedException, RemoteException {

        MangledEntry myResult;

        if (getFastChannel() != null)
            myResult = getFastChannel().read(packEntry(tmpl, false), txn, timeout);
        else
            myResult = theStub.read(packEntry(tmpl, false), txn, timeout);

        return (myResult != null) ?
            getMangler().unMangle(myResult) : null;
    }

    public Entry readIfExists(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException,
               InterruptedException, RemoteException {

        MangledEntry myResult;

        if (getFastChannel() != null)
            myResult = getFastChannel().readIfExists(packEntry(tmpl, false), txn, timeout);
        else
            myResult = theStub.readIfExists(packEntry(tmpl, false), txn, timeout);

        return (myResult != null) ?
            getMangler().unMangle(myResult) : null;
    }

    public Entry take(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException,
               InterruptedException, RemoteException {

        MangledEntry myResult;

        if (getFastChannel() != null)
            myResult = getFastChannel().take(packEntry(tmpl, false), txn, timeout);
        else
            myResult = theStub.take(packEntry(tmpl, false), txn, timeout);

        return (myResult != null) ?
            getMangler().unMangle(myResult) : null;
    }

    public Entry takeIfExists(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException,
               InterruptedException, RemoteException {

        MangledEntry myResult;

        if (getFastChannel() != null)
            myResult = getFastChannel().takeIfExists(packEntry(tmpl, false), txn, timeout);
        else
            myResult = theStub.takeIfExists(packEntry(tmpl, false), txn, timeout);

        return (myResult != null) ?
            getMangler().unMangle(myResult) : null;
    }

    public EventRegistration
        notify(Entry tmpl, Transaction txn, RemoteEventListener listener,
               long lease, MarshalledObject handback)
        throws TransactionException, RemoteException {

        return theStub.notify(packEntry(tmpl, false), txn, listener, lease,
                                handback);
    }

    public Entry snapshot(Entry e) throws RemoteException {
        if (e instanceof MangledEntry)
            return e;
        else
            return getMangler().mangle(e, true);
    }

    public Object getAdmin() throws RemoteException {
        return theStub.getAdmin();
    }

    /* *******************************************************************
     * JavaSpace05
     * *******************************************************************/

    public List write(List anEntries, Transaction aTxn, List aLeaseTimes)
        throws RemoteException, TransactionException {

        if ((anEntries.size() == 0) || (aLeaseTimes.size() == 0))
            throw new IllegalArgumentException("Empty lists are not allowed!");

        if (anEntries.size() != aLeaseTimes.size())
            throw new IllegalArgumentException("Entry list different size from lease list");

        ArrayList myMangledEntries = new ArrayList();

        for (int i = 0; i < anEntries.size(); i++) {
            Entry myEntry = (Entry) anEntries.get(i);
            Long myLeaseTime = (Long) aLeaseTimes.get(i);

            if (myEntry == null)
                throw new NullPointerException("Whoops, null Entry in list");
            if (myLeaseTime.longValue() <= 0)
                throw new IllegalArgumentException("Non-positive lease times are not allowed");

            myMangledEntries.add(packEntry(myEntry, true));
        }

        List myLeases = theStub.write(myMangledEntries, aTxn, aLeaseTimes);

        Iterator myFixups = myLeases.iterator();

        while (myFixups.hasNext()) {
            LeaseImpl myLease = (LeaseImpl) myFixups.next();
            myLease.setLandlord(theStub, theUuid);
        }

        return Collections.unmodifiableList(myLeases);
    }

    public Collection take(Collection aTemplates, Transaction aTxn,
                           long aWaitTime, long aLimit)
        throws RemoteException, TransactionException, UnusableEntriesException {

        if (aLimit <= 0)
            throw new IllegalArgumentException("Limit needs to be a positive number");

        if (aTemplates.size() == 0)
            throw new IllegalArgumentException("Templates must be non-zero length");

        MangledEntry[] myPackedTemplates = new MangledEntry[aTemplates.size()];
        Iterator myTemplates = aTemplates.iterator();

        int myIndex = 0;

        while(myTemplates.hasNext()) {
            Entry myEntry = (Entry) myTemplates.next();
            myPackedTemplates[myIndex++] = packEntry(myEntry, false);
        }

        List myMatches = theStub.take(myPackedTemplates, aTxn, aWaitTime,
                                      aLimit);


        Iterator myMangledEntrys = myMatches.iterator();

        List myExceptions = new ArrayList();
        List myEntrys = new ArrayList();

        while (myMangledEntrys.hasNext()) {
            MangledEntry myEntry = (MangledEntry) myMangledEntrys.next();

            try {
                Entry myUnpacked = getMangler().unMangle(myEntry);
                myEntrys.add(myUnpacked);
            } catch (Exception anE) {
                myExceptions.add(anE);
            }
        }

        if (myExceptions.size() == 0)
            return Collections.unmodifiableList(myEntrys);
        else
            throw new UnusableEntriesException("Couldn't unpack all Entrys",
                                               myEntrys, myExceptions);
    }

    public EventRegistration
        registerForAvailabilityEvent(Collection aTemplates, Transaction aTxn,
                                     boolean visibilityOnly,
                                     RemoteEventListener aListener,
                                     long aLeaseTime,
                                     MarshalledObject aHandback)
        throws RemoteException, TransactionException {

        MangledEntry[] myPackedTemplates = new MangledEntry[aTemplates.size()];
        Iterator myTemplates = aTemplates.iterator();

        int myIndex = 0;

        while(myTemplates.hasNext()) {
            Entry myEntry = (Entry) myTemplates.next();
            myPackedTemplates[myIndex++] = packEntry(myEntry, false);
        }

        return theStub.registerForVisibility(myPackedTemplates, aTxn,
                                             aListener, aLeaseTime, aHandback,
                                             visibilityOnly);
    }

    public net.jini.space.MatchSet contents(Collection aTemplates,
                                            Transaction aTxn,
                                            long aLeaseTime,
                                            long aLimit)
        throws RemoteException, TransactionException {

        if (aTemplates.size() == 0)
            throw new IllegalArgumentException("No template entry's");

        if (aLeaseTime == 0)
            throw new IllegalArgumentException("Single bulk read via zero length lease time is no longer spec'd");

        MangledEntry[] myMangledTemplates = new MangledEntry[aTemplates.size()];

        Iterator myTemplates = aTemplates.iterator();

        int myIndex = 0;

        while (myTemplates.hasNext()) {
            myMangledTemplates[myIndex++] =
                packEntry((Entry) myTemplates.next(), false);
        }

        ViewResult myResult = theStub.newView(myMangledTemplates, aTxn,
                                             aLeaseTime, true, aLimit, MatchSetImpl.CHUNK_SIZE);

        return new MatchSetImpl(theStub, myResult.getLease(), aLimit,
                myResult.getInitialBatch());
    }

    /* *******************************************************************
     * End of JavaSpace05
     * ******************************************************************/

    public boolean equals(Object anObject) {
        return ReferentUuids.compare(this, anObject);
    }

    public int hashCode() {
        return theUuid.hashCode();
    }

    /**
       As we put these default attributes on the proxy when we register it
       and we need to include them in the dependencies, it makes sense to
       have a method on this class to return those attributes.  Thus, we
       ensure the dependencies are accounted for and they're in the place
       that is most closely related to them (the proxy to which they'll
       be attached).
     */
    static Entry[] getDefaultAttrs(String aName) {
        Entry myInfo =
            new ServiceInfo(VersionInfo.PRODUCT_NAME,
                            VersionInfo.EMAIL_CONTACT,
                            VersionInfo.SUPPLIER_NAME,
                            VersionInfo.VERSION, "", "");

        Entry myType = new BasicServiceType("JavaSpace/JavaSpace05");

        if (aName != null) {
            return new Entry[]{myInfo, myType, new Name(aName)};
        } else {
            return new Entry[]{myInfo, myType};
        }
    }
}
