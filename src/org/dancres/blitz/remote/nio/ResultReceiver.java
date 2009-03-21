package org.dancres.blitz.remote.nio;

import java.rmi.RemoteException;

/**
 */
public class ResultReceiver implements ResultProcessor {
    private byte[] _payload;
    private RemoteException _throwable;

    public synchronized void deliver(byte[] aPayload) {
        // System.err.println("Deliver");
        _payload = aPayload;
        notify();
    }

    public synchronized void deliver(RemoteException aT) {
        _throwable = aT;
        notify();
    }

    synchronized byte[] getPayload() throws RemoteException {
        while ((_payload == null) && (_throwable == null)) {
            try {
                wait();
            } catch (InterruptedException anIE) {
            }
        }

        if (_throwable != null)
                throw _throwable;

        // System.err.println("Got payload: " + _payload.length);
        return _payload;
    }
}
