package org.dancres.blitz.remote.transport.task;

import java.io.IOException;
import java.rmi.RemoteException;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.WriteTicket;
import org.dancres.blitz.remote.ProxyFactory;
import org.dancres.blitz.remote.transport.MarshallUtil;
import org.dancres.blitz.remote.transport.Message;
import org.dancres.blitz.remote.transport.Write;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;

/**
 */
public class WriteTask implements Runnable {
    private Write _arguments;
    private IoSession _session;
    private SpaceImpl _space;
    private int _conversationId;

    public WriteTask(int aConversationId, SpaceImpl aSpace, Write aWrite,
                     IoSession aSession) {
        _arguments = aWrite;
        _session = aSession;
        _space = aSpace;
        _conversationId = aConversationId;
    }

    public void run() {
        try {
            WriteTicket myTicket =
                _space.write(_arguments.getEntry(), _arguments.getTxn(),
                    _arguments.getLeaseTime());

            _session.write(MarshallUtil.marshall(
                ProxyFactory.newEntryLeaseImpl(null,
                    null,
                    myTicket.getUID(),
                    myTicket.getExpirationTime()), _conversationId));
        } catch (IOException anIOE) {
            try {
                _session.write(MarshallUtil.marshall(
                    new RemoteException("Server has issue", anIOE),
                    _conversationId));
            } catch (RemoteException anRE) {
                System.err.println("Couldn't post error response to client");
                anRE.printStackTrace(System.err);
            }
        } catch (TransactionException aTE) {
            try {
                _session.write(MarshallUtil.marshall(aTE, _conversationId));
            } catch (RemoteException anRE) {
                System.err.println("Couldn't post error response to client");
                anRE.printStackTrace(System.err);
            }
        }
    }
}
