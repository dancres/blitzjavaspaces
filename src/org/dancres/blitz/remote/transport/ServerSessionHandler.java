package org.dancres.blitz.remote.transport;

import java.rmi.RemoteException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.remote.transport.task.ReadTask;
import org.dancres.blitz.remote.transport.task.TakeTask;
import org.dancres.blitz.remote.transport.task.WriteTask;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;

/**
 */
public class ServerSessionHandler extends IoHandlerAdapter {
    private static final boolean STRAIGHT_THROUGH = true;

    private Executor _executor =
        new ThreadPoolExecutor(1, 16, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue());

    private SpaceImpl _space;

    ServerSessionHandler(SpaceImpl aSpace) {
        _space = aSpace;
    }

    public void sessionOpened(IoSession session) {
        // set idle time to 60 seconds
        session.setIdleTime(IdleStatus.BOTH_IDLE, 60);
    }

    public void messageReceived(IoSession session, Object message) {
        Message myMessage = (Message) message;

        int myConversationId = myMessage.getConversationId();

        // System.err.println("Received request with: " + myConversationId);
        
        try {
            Object myRequest = MarshallUtil.unmarshall(myMessage);

            // System.err.println("Request type: " + myRequest.getClass());

            if (STRAIGHT_THROUGH) {
                if (myRequest instanceof Write) {
                    new WriteTask(myConversationId, _space,
                        (Write) myRequest, session).run();
                } else if (myRequest instanceof Take) {
                    new TakeTask(myConversationId, _space,
                        (Take) myRequest, session).run();
                } else if (myRequest instanceof Read) {
                    new ReadTask(myConversationId, _space,
                        (Read) myRequest, session).run();
                } else if (myRequest instanceof Ping) {
                    // System.err.println("ping");
                }
            } else {
                if (myRequest instanceof Write) {
                    _executor.execute(
                        new WriteTask(myConversationId, _space,
                            (Write) myRequest, session));
                } else if (myRequest instanceof Take) {
                    _executor.execute(
                        new TakeTask(myConversationId, _space,
                            (Take) myRequest, session));
                } else if (myRequest instanceof Read) {
                    _executor.execute(
                        new ReadTask(myConversationId, _space,
                            (Read) myRequest, session));
                } else if (myRequest instanceof Ping) {
                    // System.err.println("ping");
                }
            }

        } catch (RemoteException anRE) {
            try {
                Message myResponse =
                    MarshallUtil.marshall(
                        new RemoteException("Server problem", anRE),
                        myConversationId);
                session.write(myResponse);
            } catch (RemoteException anREE) {
                System.err.println("Couldn't post error response to client");
                anREE.printStackTrace(System.err);
            }
        }
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
        SessionLog.info(session, "Disconnecting the idle.");
        // disconnect an idle client
        session.close();
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        // close the connection on exceptional situation
        session.close();
    }
}
