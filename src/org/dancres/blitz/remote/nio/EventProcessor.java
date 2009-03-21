package org.dancres.blitz.remote.nio;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectableChannel;
import java.io.IOException;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.net.Socket;

/**
 * Server-side skeleton which manages a Selector across a collection of socket
 * connections supplied by some external entity.  Events are actually
 * handled by Dispatcher instances obtained from DispatcherFactory instances.
 *
 */
public class EventProcessor extends Thread {
    private List _socketsToAdd;
    private Selector _selector;
    private DispatcherFactory _dispatcherFactory;

    EventProcessor(Selector aSelector, DispatcherFactory aFactory) {
        _selector = aSelector;
        _socketsToAdd = new ArrayList();
        _dispatcherFactory = aFactory;
    }

    void add(SocketChannel aSocket) {
        try {
            // System.err.println("Posting socket to add");

            aSocket.configureBlocking(false);
            aSocket.socket().setTcpNoDelay(true);
            
            synchronized(_socketsToAdd) {
                _socketsToAdd.add(aSocket);
            }

            _selector.wakeup();
        } catch (Exception anE) {
            System.err.println("Failed to add");
            anE.printStackTrace(System.err);
        }
    }

    public void run() {
        while (true) {
            try {
                _selector.select();

                // See if we have some more sockets to add
                synchronized(_socketsToAdd) {
                    if (_socketsToAdd.size() > 0) {
                        // System.err.println("Got sockets to add");

                        _selector.selectNow();

                        Iterator myNewChannels = _socketsToAdd.iterator();
                        while(myNewChannels.hasNext()) {
                            SelectableChannel myChannel =
                                    (SelectableChannel) myNewChannels.next();
                            SelectionKey myKey =
                                    myChannel.register(_selector,
                                            SelectionKey.OP_READ);
                            myKey.attach(new ControlBlock(myKey,
                                    _dispatcherFactory.newDispatcher()));
                        }

                        _socketsToAdd.clear();
                    }
                }

                Iterator myKeys = _selector.selectedKeys().iterator();

                while (myKeys.hasNext()) {
                    SelectionKey myKey = (SelectionKey) myKeys.next();

                    if (myKey.isValid()) {
                        ControlBlock myBlock =
                                (ControlBlock) myKey.attachment();

                        try {
                            myBlock.process();
                        } catch (IOException anIOE) {
                            // System.err.println("Channel is dead - dumping it");
                            myKey.cancel();
                            ((SocketChannel) myKey.channel()).close();
                        }

                    } else {
                        // System.err.println("Channel is dead - dumping it");
                        myKey.cancel();
                        ((SocketChannel) myKey.channel()).close();
                    }

                    myKeys.remove();
                }
            } catch (IOException anIOE) {
                System.err.println("EventProcessor error'd");
                anIOE.printStackTrace(System.err);
            }
        }
    }
}
