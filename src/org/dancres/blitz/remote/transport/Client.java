package org.dancres.blitz.remote.transport;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

/**
 */
public class Client {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;
    private static final int CONNECT_TIMEOUT = 30; // seconds

    public static void main(String[] args) throws Throwable {
        SocketConnector connector = new SocketConnector();

        // Change the worker timeout to 1 second to make the I/O thread quit soon
        // when there's no connection to manage.
        connector.setWorkerTimeout(1);

        // Configure the service.
        SocketConnectorConfig cfg = new SocketConnectorConfig();
        cfg.setConnectTimeout(CONNECT_TIMEOUT);
            cfg.getFilterChain().addLast(
                "codec",
                new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
        // cfg.getFilterChain().addLast("logger", new LoggingFilter());

        IoSession session;

        ClientSessionHandler myHandler = new ClientSessionHandler();

        for (; ;) {
            try {
                ConnectFuture future = connector.connect(
                    new InetSocketAddress(HOSTNAME, PORT),
                    myHandler, cfg);

                future.join();
                session = future.getSession();
                break;
            }
            catch (RuntimeIOException e) {
                System.err.println("Failed to connect.");
                e.printStackTrace();
                Thread.sleep(5000);
            }
        }

        int myPingCount = 0;

        while (true) {
            long myStart = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                session.write(new PingMessage(myPingCount++));

                // myHandler.waitForResponse();
            }

            long myTotal = System.currentTimeMillis() - myStart;
            System.out.println("1000 iterations in: " + myTotal);

            double myTimePerIter = ((double) myTotal) / (double) 1000;
            System.out.println("Time per roundtrip: " + myTimePerIter);
        }
    }
}
