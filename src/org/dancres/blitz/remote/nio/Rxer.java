package org.dancres.blitz.remote.nio;

import com.go.trove.util.IntHashMap;

import java.io.InputStream;
import java.io.DataInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.lang.ref.WeakReference;

/**
 * Txer's twin - waits for a response id and a payload which is then passed
 * to the ResultProcessor registered against the id.  Obviously the
 * ResultProcessor should be registered against the id prior to transmission
 * to avoid race conditions.
 *
 * @todo That means if there's an exception during transmission, the
 * ResultProcessor should be removed (might be a good plan to do this all with
 * weak references automatically).
 */
public class Rxer extends Thread {
    private DataInputStream _socketRx;
    private final IntHashMap _listeners = new IntHashMap();
    private WeakReference _listener;

    Rxer(InputStream anInput, TransportListener aListener) {
        _socketRx = new DataInputStream(anInput);
        _listener = new WeakReference(aListener);
        setDaemon(true);
        start();
    }

    void waitFor(int anId, ResultProcessor aProcessor) {
        synchronized(_listeners) {
            _listeners.put(anId, aProcessor);
        }
    }

    void cancel(int anId) {
        synchronized(_listeners) {
            _listeners.remove(anId);
        }
    }

    public void run() {
        while (true) {
            try {
                int myReqId = _socketRx.readInt();

                // System.err.println("Receiving: " + myReqId);

                int myResponseSize = _socketRx.readInt();

                byte[] myBuffer = new byte[myResponseSize];

                _socketRx.readFully(myBuffer);

                ResultProcessor myProcessor;

                synchronized(_listeners) {
                    myProcessor = (ResultProcessor) _listeners.remove(myReqId);
                }

                if (myProcessor != null)
                    myProcessor.deliver(myBuffer);

            } catch (Exception anE) {
                // System.err.println("Error receiving response");
                // anE.printStackTrace(System.err);

                // Tell all listeners to abort
                Collection myListeners;

                synchronized(_listeners) {
                    myListeners = _listeners.values();
                }

                Iterator myRecipients = myListeners.iterator();

                while (myRecipients.hasNext()) {
                    ResultProcessor myProcessor =
                            (ResultProcessor) myRecipients.next();

                    myProcessor.deliver(new RemoteException("Connection problem", anE));
                }

                pingListener();

                return;
            }
        }
    }

    private void pingListener() {
        TransportListener myListener = (TransportListener) _listener.get();

        if (myListener != null)
            myListener.dead();
    }
}
