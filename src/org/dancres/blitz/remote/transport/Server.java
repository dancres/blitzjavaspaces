package org.dancres.blitz.remote.transport;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.Iterator;

import net.jini.core.transaction.CannotJoinException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.CrashCountException;
import net.jini.core.transaction.server.TransactionParticipant;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

/**
 */
public class Server {
    private static final int SERVER_PORT = 8080;
    // Set this to false to use object serialization instead of custom codec.
    private static final boolean USE_CUSTOM_CODEC = true;

    public static void main(String[] args) throws Throwable {
        SpaceImpl mySpace = new SpaceImpl(new TxnGatewayImpl(null));

        IoAcceptor acceptor = new SocketAcceptor();
            // new SocketAcceptor(2, Executors.newFixedThreadPool(2));

        // Prepare the service configuration.
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.setReuseAddress(true);
        cfg.getFilterChain().addLast(
            "codec",
            new ProtocolCodecFilter(new MessageCodecFactory()));

        SocketSessionConfig mySConfig = (SocketSessionConfig)
            cfg.getSessionConfig();
        mySConfig.setTcpNoDelay(true);

        // cfg.getFilterChain().addLast(
            //     "codec",
            //     new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));

        // cfg.getFilterChain().addLast("logger", new LoggingFilter());

        acceptor.bind(
            new InetSocketAddress(SERVER_PORT),
            new ServerSessionHandler(mySpace), cfg);

        System.out.println("Listening on port " + SERVER_PORT);
    }

    static class TxnGatewayImpl implements TxnGateway {
        private long theCrashCount = System.currentTimeMillis();
        private TransactionParticipant theParticipantStub;

        TxnGatewayImpl(TransactionParticipant aStub) {
            theParticipantStub = aStub;
        }

        public void join(TxnId anId)
            throws UnknownTransactionException, CannotJoinException,
            CrashCountException, RemoteException {

            anId.getManager().join(anId.getId(), theParticipantStub,
                theCrashCount);
        }

        public int getState(TxnId anId)
            throws UnknownTransactionException, RemoteException {

            return anId.getManager().getState(anId.getId());
        }
    }
}
