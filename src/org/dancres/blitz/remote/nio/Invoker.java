package org.dancres.blitz.remote.nio;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.*;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.remote.LeaseImpl;

/**
 * Encapsulates all the logic for invoking a space method - one of take, read and write.
 * Manages the socket and wraps it in an asynchronous dispatch framework to allow queueing
 * etc for max throughput.
 */
public class Invoker implements FastSpace, Serializable, TransportListener {
    private transient Socket _socket;
    private transient Rxer _rxer;
    private transient Txer _txer;
    private transient CommandFactory _commandFactory;

    private transient int _nextRequestId;

    private transient boolean isDown;

    private InetSocketAddress _addr;

    public Invoker(InetSocketAddress anAddr) {
        _addr = anAddr;
    }

    public Invoker(InetSocketAddress anAddr, boolean doOpen) throws IOException {
        _addr = anAddr;

        if (doOpen)
            init();
    }

    public void dead() {
        synchronized(this) {
            isDown = true;
        }

        // Transport is down
        _txer.halt();

        try {
            _socket.close();
        } catch (IOException anIOE) {
            // Nothing to do
        }
    }

    public void init() throws IOException {
        /*
            System.err.println("Connecting to: " + _addr.getAddress() +
                    ", " + _addr.getPort());
        */

        _socket = new Socket(_addr.getAddress(), _addr.getPort());
        _socket.setTcpNoDelay(true);
        _socket.setReuseAddress(true);

        /*
            System.err.println("Connect");
        */
        _rxer = new Rxer(_socket.getInputStream(), this);
        _txer = new Txer(_socket.getOutputStream());

        /*
            System.err.println("Socket in buffer: " +
                    _socket.getReceiveBufferSize());
            System.err.println("Socket out buffer: " +
                    _socket.getSendBufferSize());
        */

        _commandFactory = new CommandFactory();
    }

    public boolean isInited() {
        return (_socket != null);
    }

    private int getNextRequestId() {
        synchronized(this) {
            return _nextRequestId++;
        }
    }

    private void downBarrier() throws RemoteException {
        synchronized(this) {
            if (isDown)
                throw new RemoteException("Connection is closed");
        }
    }

    public LeaseImpl write(MangledEntry anEntry, Transaction aTxn, long aLeaseTime)
        throws RemoteException, TransactionException {

        /*
        long myStart = System.currentTimeMillis();
         */

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newWrite(anEntry, aTxn, aLeaseTime);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);
            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult instanceof TransactionException) {
                throw (TransactionException) myResult;
            } else if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                        (Throwable) myResult);
            } else {
                /*
                System.err.println("Wrote: " + (System.currentTimeMillis() -
                        myStart));
                 */

                return (LeaseImpl) myResult;
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                    aCNFE);
        }
    }

    public MangledEntry takeIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws RemoteException, TransactionException {

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp =
                _commandFactory.newTakeExists(anEntry, aTxn, aWaitTime);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);
            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult == null)
                return null;

            if (myResult instanceof TransactionException) {
                throw (TransactionException) myResult;
            } else if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                        (Throwable) myResult);
            } else {
                return (MangledEntry) myResult;
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                    aCNFE);
        }
    }

    public MangledEntry readIfExists(MangledEntry anEntry, Transaction aTxn, long aWaitTime)
        throws RemoteException, TransactionException {

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp =
                _commandFactory.newReadExists(anEntry, aTxn, aWaitTime);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult == null)
                return null;

            if (myResult instanceof TransactionException) {
                throw (TransactionException) myResult;
            } else if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                        (Throwable) myResult);
            } else {
                return (MangledEntry) myResult;
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                    aCNFE);
        }
    }

    public MangledEntry take(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException {

        /*
        long myStart = System.currentTimeMillis();
         */

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newTake(anEntry, aTxn, aWaitTime);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult == null) {
                /*
                System.err.println("Took: " + (System.currentTimeMillis() -
                        myStart));
                        */
                return null;
            }

            if (myResult instanceof TransactionException) {
                throw (TransactionException) myResult;
            } else if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                        (Throwable) myResult);
            } else {
                /*
                System.err.println("Took: " + (System.currentTimeMillis() -
                        myStart));
                 */

                return (MangledEntry) myResult;
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                    aCNFE);
        }
    }

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException {

        /*
        long myStart = System.currentTimeMillis();
         */
        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newRead(anEntry, aTxn, aWaitTime);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult == null) {
                /*
                System.err.println("Read: " + (System.currentTimeMillis() -
                        myStart));
                 */
                return null;
            }

            if (myResult instanceof TransactionException) {
                throw (TransactionException) myResult;
            } else if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                        (Throwable) myResult);
            } else {
                /*
                System.err.println("Read: " + (System.currentTimeMillis() -
                        myStart));
                 */
                return (MangledEntry) myResult;
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                    aCNFE);
        }
    }

    public EventRegistration notify(MangledEntry anEntry, Transaction aTxn,
                                    RemoteEventListener aListener,
                                    long aLeaseTime,
                                    MarshalledObject aHandback)
        throws RemoteException, TransactionException {

        throw new UnsupportedOperationException();
    }

    public Object getAdmin() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    public int prepare(TransactionManager aTxnMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newPrepare(aTxnMgr, anId);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof UnknownTransactionException) {
                throw (UnknownTransactionException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                    (Throwable) myResult);
            } else {
                return ((Integer) myResult).intValue();
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                aCNFE);
        }
    }

    public void commit(TransactionManager aTxnMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newCommit(aTxnMgr, anId);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof UnknownTransactionException) {
                throw (UnknownTransactionException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                    (Throwable) myResult);
            }
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                aCNFE);
        }
    }

    public void abort(TransactionManager aTxnMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newAbort(aTxnMgr, anId);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof UnknownTransactionException) {
                throw (UnknownTransactionException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                    (Throwable) myResult);
            }
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                aCNFE);
        }
    }

    public int prepareAndCommit(TransactionManager aTxnMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        downBarrier();

        int myReqId = getNextRequestId();

        Operation myOp = _commandFactory.newPrepareCommit(aTxnMgr, anId);

        ResultReceiver myReceiver = new ResultReceiver();
        _rxer.waitFor(myReqId, myReceiver);

        try {
            byte[] myFlattenedOp = _commandFactory.pack(myOp);

            _txer.send(myReqId, myFlattenedOp);
        } catch (Exception anE) {
            _rxer.cancel(myReqId);
            throw new RemoteException("Failed to send request", anE);
        }

        byte[] myBuffer = myReceiver.getPayload();

        try {
            ByteArrayInputStream myBAIS = new ByteArrayInputStream(myBuffer);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Object myResult = myOIS.readObject();

            if (myResult instanceof RemoteException) {
                throw (RemoteException) myResult;
            } else if (myResult instanceof UnknownTransactionException) {
                throw (UnknownTransactionException) myResult;
            } else if (myResult instanceof Throwable) {
                throw new RemoteException("Invocation failed",
                    (Throwable) myResult);
            } else {
                return ((Integer) myResult).intValue();
            }

        } catch (IOException anIOE) {
            throw new RemoteException("Failed to unmarshal response: " + myReqId, anIOE);
        } catch (ClassNotFoundException aCNFE) {
            throw new RemoteException("Failed to unmarshal response",
                aCNFE);
        }
    }

    protected void finalize() throws Throwable {
        dead();

        super.finalize();
    }
}
