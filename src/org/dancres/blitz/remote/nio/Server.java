package org.dancres.blitz.remote.nio;

import org.dancres.blitz.SpaceImpl;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;

/**
 * To make this scale we would have multiple selectors each serviced by a
 * separate event processor thread.  That's way easier than trying to handle
 * multiple threads doing selects on the same selector.  Each processor
 * thread would handle IO transfer into ControlBlock which would then figure
 * out when to dispatch a request into the thread pool for execution.
 * We'd also need to pass over the SocketChannel or whatever so the worker
 * knows where to post the response to.  Probably the best thing is to pass
 * the selection key so that we can set it for write and post the data to write
 * into the control block.  When the control block sees no more data to write
 * it clears down that setting on the selection key.
 */
public class Server implements Runnable {
    private InetSocketAddress _address;
    private ServerSocketChannel _rootSocket;
    private Thread _acceptor;
    private Selector _selector;
    private EventProcessor _processor;

    /**
     * Warning: Do not use more than one selector, not thread safe yet
     */
    public Server(InetSocketAddress anAddr,
                  DispatcherFactory aFactory) throws IOException {
        _address = anAddr;
        _rootSocket = ServerSocketChannel.open();

        _rootSocket.socket().bind(anAddr);

        _acceptor = new Thread(this, "Selector");
        _selector = Selector.open();
        _processor = new EventProcessor(_selector, aFactory);
        _processor.start();
        _acceptor.start();
    }

    public FastSpace getEndpoint() {
        return new Invoker(new InetSocketAddress(_address.getAddress(),
                _rootSocket.socket().getLocalPort()));
    }

    public void run() {
        while (true) {
            try {
                SocketChannel mySocket = _rootSocket.accept();

                // System.err.println("Connection received: " + mySocket);
                _processor.add(mySocket);
            } catch (IOException anIOE) {
                System.err.println("Error during accept");
                anIOE.printStackTrace(System.err);
            }
        }
    }

    public static void main(String args[]) {
        try {

            SpaceImpl mySpace = new SpaceImpl(null);

            new Server(new InetSocketAddress(12345),
                    new DispatcherFactoryImpl(mySpace));

        } catch (Exception anE) {
            System.err.println("Server error");
            anE.printStackTrace(System.err);
        }
    }
}
