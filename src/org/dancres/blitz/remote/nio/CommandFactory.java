package org.dancres.blitz.remote.nio;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.io.AnnotatingObjectOutputStream;
import org.dancres.io.AnnotatingObjectInputStream;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.server.TransactionManager;

import java.io.*;

/**
 * Yields a space operation that can be rendered into a byte array for
 * passing across to the server over a socket.  Also performs the conversion
 * in the other direction
 */
public class CommandFactory {
    public Operation newWrite(MangledEntry anEntry, Transaction aTxn,
                              long aLease) {
        return new GenericSpaceOp(GenericSpaceOp.WRITE, anEntry, aTxn, aLease);
    }

    public Operation newRead(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime) {
        return new GenericSpaceOp(GenericSpaceOp.READ, anEntry, aTxn, aWaitTime);
    }

    public Operation newTake(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime) {
        return new GenericSpaceOp(GenericSpaceOp.TAKE, anEntry, aTxn, aWaitTime);
    }

    public Operation newReadExists(MangledEntry anEntry, Transaction aTxn,
                                   long aWaitTime) {
        return new GenericSpaceOp(GenericSpaceOp.READ_EXISTS, anEntry, aTxn, aWaitTime);
    }

    public Operation newTakeExists(MangledEntry anEntry, Transaction aTxn,
                                   long aWaitTime) {
        return new GenericSpaceOp(GenericSpaceOp.TAKE_EXISTS, anEntry, aTxn, aWaitTime);
    }

    public Operation newPrepare(TransactionManager aMgr, long anId) {
        return new TransactionOp(TransactionOp.PREPARE, aMgr, anId);
    }

    public Operation newAbort(TransactionManager aMgr, long anId) {
        return new TransactionOp(TransactionOp.ABORT, aMgr, anId);
    }

    public Operation newCommit(TransactionManager aMgr, long anId) {
        return new TransactionOp(TransactionOp.COMMIT, aMgr, anId);
    }

    public Operation newPrepareCommit(TransactionManager aMgr, long anId) {
        return new TransactionOp(TransactionOp.PREPARE_COMMIT, aMgr, anId);
    }

    public byte[] pack(Operation anOp) throws IOException {
        ByteArrayOutputStream myBAOS = new ByteArrayOutputStream();

        if (anOp instanceof GenericSpaceOp) {
            GenericSpaceOp myOp = (GenericSpaceOp) anOp;

            myBAOS.write(myOp.getOperation());

            AnnotatingObjectOutputStream myOOS =
                    new AnnotatingObjectOutputStream(myBAOS, myBAOS);

            myOOS.writeObject(myOp.getEntry());
            myOOS.writeObject(myOp.getTxn());
            myOOS.writeLong(myOp.getLease());

            myOOS.close();
        } else if (anOp instanceof TransactionOp) {
            TransactionOp myOp = (TransactionOp) anOp;

            myBAOS.write(myOp.getOperation());

            AnnotatingObjectOutputStream myOOS =
                new AnnotatingObjectOutputStream(myBAOS, myBAOS);

            myOOS.writeObject(myOp.getMgr());
            myOOS.writeLong(myOp.getId());

            myOOS.close();
        } else
            throw new IOException("Don't know how to marshal: " +
                    anOp.getClass());

        byte[] myResult = myBAOS.toByteArray();

        // System.err.println("Pack hash: " + hash(myResult));

        return myResult;
    }

    private int hash(byte[] anArray) {
        int myHash = 0;

        for (int i = 0; i < anArray.length; i++) {
            myHash += anArray[i];
            myHash += (myHash << 10);
            myHash ^= (myHash >> 6);
        }

        myHash += (myHash << 3);
        myHash ^= (myHash >> 11);
        myHash += (myHash << 15);

        return myHash;
    }

    public Operation unpack(byte[] aFlattenedOp) throws IOException {

        // System.err.println("Unpack hash: " + hash(aFlattenedOp));

        ByteArrayInputStream myBAIS = new ByteArrayInputStream(aFlattenedOp);

        int myOpCode = myBAIS.read();

        switch (myOpCode) {
            case GenericSpaceOp.WRITE :
            case GenericSpaceOp.TAKE_EXISTS :
            case GenericSpaceOp.READ_EXISTS :
            case GenericSpaceOp.TAKE :
            case GenericSpaceOp.READ : {

                try {
                    AnnotatingObjectInputStream myOIS =
                            new AnnotatingObjectInputStream(null, myBAIS,
                                    myBAIS, false);

                    MangledEntry myEntry = (MangledEntry) myOIS.readObject();
                    Transaction myTxn = (Transaction) myOIS.readObject();
                    long myLease = myOIS.readLong();

                    myOIS.close();

                    return new GenericSpaceOp(myOpCode, myEntry,
                            myTxn, myLease);

                } catch (ClassNotFoundException aCNFE) {
                    IOException myIOE =
                            new IOException("Failed to unpack operation");

                    myIOE.initCause(aCNFE);
                    throw myIOE;
                }
            }

            case TransactionOp.ABORT :
            case TransactionOp.COMMIT :
            case TransactionOp.PREPARE :
            case TransactionOp.PREPARE_COMMIT : {

                try {
                    AnnotatingObjectInputStream myOIS =
                        new AnnotatingObjectInputStream(null, myBAIS,
                            myBAIS, false);

                    TransactionManager myMgr =
                        (TransactionManager) myOIS.readObject();
                    long myId = myOIS.readLong();

                    myOIS.close();

                    return new TransactionOp(myOpCode, myMgr, myId);

                } catch (ClassNotFoundException aCNFE) {
                    IOException myIOE =
                        new IOException("Failed to unpack operation");

                    myIOE.initCause(aCNFE);
                    throw myIOE;
                }
            }

            default : throw new IOException("Bad Op: " + myOpCode);
        }
    }
}
