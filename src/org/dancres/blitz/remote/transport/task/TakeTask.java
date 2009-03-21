package org.dancres.blitz.remote.transport.task;

import java.io.IOException;
import java.rmi.RemoteException;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.remote.transport.MarshallUtil;
import org.dancres.blitz.remote.transport.Message;
import org.dancres.blitz.remote.transport.Take;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;

/**
 */
public class TakeTask implements Runnable {
    private Take _arguments;
    private IoSession _session;
    private SpaceImpl _space;
    private int _conversationId;

    public TakeTask(int aConversationId, SpaceImpl aSpace, Take aTake,
                    IoSession aSession) {
        _arguments = aTake;
        _session = aSession;
        _space = aSpace;
        _conversationId = aConversationId;
    }

    public void run() {
        try {
            MangledEntry myEntry =
                _space.take(_arguments.getEntry(), _arguments.getTxn(),
                    _arguments.getWaitTime());

            _session.write(MarshallUtil.marshall(
                myEntry, _conversationId));

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
