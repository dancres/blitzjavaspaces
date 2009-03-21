package org.dancres.blitz.remote.nio;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.WriteTicket;
import org.dancres.blitz.remote.LeaseImpl;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;

/**
 * Responsible for decoding a serialized space operation as provided by
 * Invoker.  It then dispatches the relevant operation and marshals up the
 * return (or exception) befor epassing it back to the ControlBlock for
 * transmission.
 */
public class MessageDecoder implements Runnable {
    private static CommandFactory _commandFactory = new CommandFactory();

    private int _reqId;
    private byte[] _operation;
    private ControlBlock _block;
    private SpaceImpl _space;

    MessageDecoder(int aReqId,SpaceImpl aSpace, ControlBlock aBlock,
                   byte[] anOpBlock) {
        _reqId = aReqId;
        _operation = anOpBlock;
        _block = aBlock;
        _space = aSpace;
    }

    /**
     * @todo Put the execution code into the op itself which would save
     * the switch etc and allow us to drop the test for GenericSpaceOp etc.
     * It'll also be easier to add say TransactionOp so we can extend the
     * interface to TransactionParticipant
     */
    public void run() {
        Object myResult = null;
        try {
            // System.err.println("Received op: " + _reqId + ", " + _operation.length);
            Operation myOp = _commandFactory.unpack(_operation);

            if (myOp instanceof GenericSpaceOp) {
                GenericSpaceOp mySpaceOp = (GenericSpaceOp) myOp;

                switch (mySpaceOp.getOperation()) {
                    case GenericSpaceOp.WRITE : {
                        WriteTicket myTicket =
                                _space.write(mySpaceOp.getEntry(),
                                        mySpaceOp.getTxn(),
                                        mySpaceOp.getLease());

                        myResult = new LeaseImpl(null, null, myTicket.getUID(),
                                myTicket.getExpirationTime());
                        break;
                    }

                    case GenericSpaceOp.TAKE : {
                        myResult =
                                _space.take(mySpaceOp.getEntry(),
                                        mySpaceOp.getTxn(),
                                        mySpaceOp.getLease());
                        break;
                    }

                    case GenericSpaceOp.TAKE_EXISTS : {
                        myResult =
                                _space.takeIfExists(mySpaceOp.getEntry(),
                                        mySpaceOp.getTxn(),
                                        mySpaceOp.getLease());
                        break;
                    }

                    case GenericSpaceOp.READ : {
                        myResult =
                                _space.read(mySpaceOp.getEntry(),
                                        mySpaceOp.getTxn(),
                                        mySpaceOp.getLease());
                        break;
                    }

                    case GenericSpaceOp.READ_EXISTS : {
                        myResult =
                                _space.readIfExists(mySpaceOp.getEntry(),
                                        mySpaceOp.getTxn(),
                                        mySpaceOp.getLease());
                        break;
                    }

                    default :
                        myResult = new RemoteException("Unrecognised space op");
                }
            } else if (myOp instanceof TransactionOp) {
                TransactionOp myTxnOp = (TransactionOp) myOp;

                switch (myTxnOp.getOperation()) {
                    case TransactionOp.PREPARE : {
                        int myCode =
                            _space.getTxnControl().prepare(myTxnOp.getMgr(),
                                myTxnOp.getId());

                        myResult = new Integer(myCode);
                        break;
                    }

                    case TransactionOp.COMMIT : {
                        _space.getTxnControl().commit(myTxnOp.getMgr(),
                            myTxnOp.getId());
                        break;
                    }

                    case TransactionOp.ABORT : {
                        _space.getTxnControl().abort(myTxnOp.getMgr(),
                            myTxnOp.getId());
                        break;
                    }

                    case TransactionOp.PREPARE_COMMIT : {
                        int myCode =
                            _space.getTxnControl().prepareAndCommit(myTxnOp.getMgr(),
                                myTxnOp.getId());

                        myResult = new Integer(myCode);
                        break;
                    }

                    default :
                        myResult = new RemoteException("Unrecognised txn op");
                }
            }
        } catch (Throwable aT) {
            System.err.println("Failed to dispatch request");
            aT.printStackTrace(System.err);

            myResult = aT;
        }

        ByteArrayOutputStream myBAOS = new ByteArrayOutputStream();

        try {
            ObjectOutputStream myOOS = new ObjectOutputStream(myBAOS);
            myOOS.writeObject(myResult);
            myOOS.close();
        } catch (Exception anE) {
            System.err.println("Failed to marshall response");
            anE.printStackTrace(System.err);
        }

        ByteBuffer myBuffer = ByteBuffer.allocate(myBAOS.size() + 8);

        // System.err.println("Sending: " + _reqId + ", " + myBAOS.size());
        myBuffer.putInt(_reqId);
        myBuffer.putInt(myBAOS.size());
        myBuffer.put(myBAOS.toByteArray());

        myBuffer.flip();
        _block.send(new ByteBuffer[] {myBuffer});
    }
}
