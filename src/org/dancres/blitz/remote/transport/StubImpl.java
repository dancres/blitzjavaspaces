package org.dancres.blitz.remote.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.List;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.space.JavaSpace;

import org.dancres.blitz.EntryChit;
import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.remote.BlitzServer;
import org.dancres.blitz.remote.LeaseImpl;
import org.dancres.blitz.remote.LeaseResults;
import org.dancres.blitz.remote.ViewResult;
import org.dancres.blitz.remote.view.EntryViewUID;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.Switch;
import org.apache.mina.common.*;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

/**
 */
public class StubImpl implements BlitzServer {
    private static final String HOSTNAME = "192.168.0.54";
    private static final int PORT = 8080;
    private static final int CONNECT_TIMEOUT = 30; // seconds

    private IoSession _session;
    private ClientSessionHandler _handler;

    private Object _lock = new Object();
    private int _nextConversationId;

    private int nextConversationId() {
        synchronized (_lock) {
            return _nextConversationId++;
        }
    }

    private synchronized IoSession getSession() throws RemoteException {
        if (_session == null) {
            SocketConnector connector = new SocketConnector();
                // new SocketConnector(2, Executors.newFixedThreadPool(2));

            // Change the worker timeout to 1 second to make the I/O thread quit soon
            // when there's no connection to manage.
            connector.setWorkerTimeout(1);

            // Configure the service.
            SocketConnectorConfig cfg = new SocketConnectorConfig();
            cfg.setConnectTimeout(CONNECT_TIMEOUT);
            cfg.getFilterChain().addLast(
                "codec",
                new ProtocolCodecFilter(new MessageCodecFactory()));

            SocketSessionConfig mySConfig = (SocketSessionConfig)
                cfg.getSessionConfig();
            mySConfig.setTcpNoDelay(true);

            // cfg.getFilterChain().addLast(
            //     "codec",
            //     new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
            // cfg.getFilterChain().addLast("logger", new LoggingFilter());

            _handler = new ClientSessionHandler();

            for (; ;) {
                try {
                    ConnectFuture future = connector.connect(
                        new InetSocketAddress(HOSTNAME, PORT),
                        _handler, cfg);

                    future.join();
                    _session = future.getSession();
                    break;
                }
                catch (RuntimeIOException anRIOE) {
                    throw new RemoteException("Failed to connect", anRIOE);
                }
            }
        }

        return _session;
    }

    private synchronized ClientSessionHandler getHandler()
        throws RemoteException {

        if (_handler == null) {
            getSession();
        }

        return _handler;
    }

    public void ping() throws RemoteException {
        Message myMessage = MarshallUtil.marshall(new Ping(), 0);

        getSession().write(myMessage);
    }

    public LeaseImpl write(MangledEntry anEntry, Transaction aTxn,
                           long aLeaseTime) throws RemoteException,
        TransactionException {

        int myConversationId = nextConversationId();

        Message myMessage =
            MarshallUtil.marshall(new Write(anEntry, aTxn, aLeaseTime),
                myConversationId);

        ClientSessionHandler.Ticket myTicket =
            getHandler().getTicket(myConversationId);

        getSession().write(myMessage);

        myMessage = myTicket.getResponse(Long.MAX_VALUE);

        Object myResult = MarshallUtil.unmarshall(myMessage);

        if (myResult instanceof Exception) {
            if (myResult instanceof RemoteException)
                throw (RemoteException) myResult;
            else
                throw (TransactionException) myResult;
        } else {
            return (LeaseImpl) myResult;
        }
    }

    public MangledEntry take(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime) throws RemoteException,
        TransactionException {

        int myConversationId = nextConversationId();

        Message myMessage =
            MarshallUtil.marshall(new Take(anEntry, aTxn, aWaitTime),
                myConversationId);

        ClientSessionHandler.Ticket myTicket =
            getHandler().getTicket(myConversationId);

        getSession().write(myMessage);

        myMessage = myTicket.getResponse(aWaitTime);

        if (myMessage == null)
            return null;

        Object myResult = MarshallUtil.unmarshall(myMessage);

        if (myResult instanceof Exception) {
            if (myResult instanceof RemoteException)
                throw (RemoteException) myResult;
            else
                throw (TransactionException) myResult;
        } else {
            return (MangledEntry) myResult;
        }
    }

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime) throws RemoteException,
        TransactionException {

        int myConversationId = nextConversationId();

        Message myMessage =
            MarshallUtil.marshall(new Read(anEntry, aTxn, aWaitTime),
                myConversationId);

        ClientSessionHandler.Ticket myTicket =
            getHandler().getTicket(myConversationId);

        getSession().write(myMessage);

        myMessage = myTicket.getResponse(Long.MAX_VALUE);

        if (myMessage == null)
            return null;

        Object myResult = MarshallUtil.unmarshall(myMessage);

        if (myResult instanceof Exception) {
            if (myResult instanceof RemoteException)
                throw (RemoteException) myResult;
            else
                throw (TransactionException) myResult;
        } else {
            return (MangledEntry) myResult;
        }
    }

    public MangledEntry takeIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime) throws RemoteException,
        TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MangledEntry readIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime) throws RemoteException,
        TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public EventRegistration notify(MangledEntry anEntry, Transaction aTxn,
                                    RemoteEventListener aListener,
                                    long aLeaseTime,
                                    MarshalledObject aHandback) throws
        RemoteException, TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Entry[] getLookupAttributes() throws RemoteException {
        return new Entry[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addLookupAttributes(Entry[] entries) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modifyLookupAttributes(Entry[] entries, Entry[] entries1) throws
        RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getLookupGroups() throws RemoteException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addLookupGroups(String[] strings) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeLookupGroups(String[] strings) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLookupGroups(String[] strings) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public LookupLocator[] getLookupLocators() throws RemoteException {
        return new LookupLocator[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addLookupLocators(LookupLocator[] lookupLocators) throws
        RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeLookupLocators(LookupLocator[] lookupLocators) throws
        RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLookupLocators(LookupLocator[] lookupLocators) throws
        RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Stat[] getStats() throws RemoteException {
        return new Stat[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSwitches(Switch[] aListOfSwitches) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void backup(String aBackupDir) throws RemoteException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void requestSnapshot() throws RemoteException, TransactionException,
        IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void shutdown() throws RemoteException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clean() throws RemoteException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void reap() throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void destroy() throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public JavaSpace getJavaSpaceProxy() throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }/*
    * @param isJavaSpace05 if <code>true</code> enforces any defined lease bounds
    * and asserts locks when performing the scan/acquire internally.
    * This is used internally to differentiate between old and new contents
    * methods as JavaSpaceAdmin::contents does not do leases.
    */
    public ViewResult newView(MangledEntry[] aTemplates, Transaction aTxn,
                              long aLeaseDuration, boolean isJavaSpace05,
                              long aLimit, int anInitialChunk) throws
        RemoteException, TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public EntryChit[] getNext(EntryViewUID aEntryViewUID,
                               int aChunkSize) throws RemoteException {
        return new EntryChit[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void delete(Object aCookie) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close(EntryViewUID aEntryViewUID) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getServiceProxy() throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int prepare(TransactionManager transactionManager, long l) throws
        UnknownTransactionException, RemoteException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void commit(TransactionManager transactionManager, long l) throws
        UnknownTransactionException, RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void abort(TransactionManager transactionManager, long l) throws
        UnknownTransactionException, RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int prepareAndCommit(TransactionManager transactionManager,
                                long l) throws UnknownTransactionException,
        RemoteException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List write(List aMangledEntries, Transaction aTxn,
                      List aLeaseTimes) throws RemoteException,
        TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List take(MangledEntry[] aTemplates, Transaction aTxn,
                     long aWaitTime, long aLimit) throws RemoteException,
        TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public EventRegistration registerForVisibility(MangledEntry[] aTemplates,
                                                   Transaction aTxn,
                                                   RemoteEventListener aListener,
                                                   long aLeaseTime,
                                                   MarshalledObject aHandback,
                                                   boolean visibilityOnly) throws
        RemoteException, TransactionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public LeaseResults renew(SpaceUID[] aLeases, long[] aDurations) throws
        RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public LeaseResults cancel(SpaceUID[] aLeases) throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long renew(SpaceUID aUID, long aDuration) throws
        UnknownLeaseException, LeaseDeniedException, RemoteException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cancel(SpaceUID aUID) throws UnknownLeaseException,
        RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getAdmin() throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
