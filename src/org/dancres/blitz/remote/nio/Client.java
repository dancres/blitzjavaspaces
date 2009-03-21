package org.dancres.blitz.remote.nio;

import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.mangler.EntryMangler;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;

import net.jini.core.lease.Lease;

/**
 */
public class Client implements Runnable {
    private Invoker _invoker;

    Client(InetSocketAddress anAddr) throws IOException {
        _invoker = new Invoker(anAddr, true);
        new Thread(this).start();
    }

    public void run() {
        // while(true) {
            try {

                _invoker.write(EntryMangler.getMangler().mangle(new DummyEntry("555555")),
                        null, Lease.FOREVER);

            } catch (Exception anE) {
                System.err.println("Rdv error");
                anE.printStackTrace(System.err);
                // break;
            }
        // }
    }

    public static void main(String args[]) {
        try {
            new Client(new InetSocketAddress(args[0],
                    Integer.parseInt(args[1])));

        } catch (Exception anE) {
            System.err.println("Client error");
            anE.printStackTrace(System.err);
        }
    }
}
