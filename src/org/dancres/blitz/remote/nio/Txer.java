package org.dancres.blitz.remote.nio;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-side message transmitter.  Accepts a unique id for the message
 * and the message itself.  This message is then queued and sent asynchronously.
 */
class Txer {
    private ExecutorService _sender;
    private DataOutputStream _socketTx;

    Txer(OutputStream anOutgoing) {
        _socketTx = new DataOutputStream(anOutgoing);
        _sender = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }

    void send(int anId, byte[] anOp) throws InterruptedException {
        _sender.execute(new SendTask(anId, anOp));
    }

    void halt() {
        _sender.shutdownNow();
    }

    class SendTask implements Runnable {
        private int _id;
        private byte[] _op;

        SendTask(int anId, byte[] anOp) {
            _id = anId;
            _op = anOp;
        }

        public void run() {
            try {
                // System.err.println("Sending: " + _id + ", " + _op.length);
                _socketTx.writeInt(_id);
                _socketTx.writeInt(_op.length);
                _socketTx.write(_op);
                _socketTx.flush();
                // System.err.println("Done: " + _id);
            } catch (IOException anIOE) {
                System.err.println("Failed to send request:" + _id);
                anIOE.printStackTrace(System.err);
            }
        }
    }
}
