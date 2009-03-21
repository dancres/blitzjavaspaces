package org.dancres.blitz.remote.nio;

import java.rmi.RemoteException;

/**
 * An instance of this will receive a response as the result of a message
 * sent via Txer.  Txer will have been given an id for the message and
 * the instance should be registered with Rxer prior to message transmission
 * against the same id.  When a response arrives for that id, the instance
 * will have it's deliver method invoked with the raw payload (a byte[]).
 */
public interface ResultProcessor {
    void deliver(byte[] aPayload);
    void deliver(RemoteException aT);
}
