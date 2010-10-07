package org.dancres.blitz.remote.transport;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;

/**
 */
public class ClientSessionHandler extends IoHandlerAdapter {
    private ConcurrentHashMap _requests = new ConcurrentHashMap();

    public void messageReceived(IoSession session, Object aMessage) {
        Message myMessage = (Message) aMessage;

        int myConversationId = myMessage.getConversationId();

        // System.err.println("Got response: " + myConversationId);

        Ticket myTicket = (Ticket)
            _requests.get(new Integer(myConversationId));

        if (myTicket != null)
            myTicket.postResponse(myMessage);
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        SessionLog.warn(session, "Server error, disconnecting...", cause);
        session.close();
    }

    Ticket getTicket(int aConversationId) {
        Ticket myTicket = new Ticket(aConversationId);

        _requests.put(new Integer(aConversationId), myTicket);
        return myTicket;
    }

    class Ticket {
        private int _conversationId;
        private Message _message;

        Ticket(int aConversationId) {
            _conversationId = aConversationId;
        }

        void postResponse(Message aMessage) {
            synchronized(this) {
                _message = aMessage;
                notify();
            }
        }

        Message getResponse(long aWait) throws RemoteException {
            try {
                synchronized(this) {
                    if (_message == null) {
                        wait(aWait);
                    }

                    return _message;
                }
            } catch(InterruptedException anIE) {
                throw new RemoteException("interrupted", anIE);
            } finally {
                _requests.remove(new Long(_conversationId));
            }
        }
    }
}
