package org.dancres.blitz.remote.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;

import org.dancres.io.AnnotatingObjectInputStream;
import org.dancres.io.AnnotatingObjectOutputStream;
import org.apache.mina.common.ByteBuffer;

/**
 */
public class MarshallUtil {
    public static Message marshall(Serializable aMessage,
                            int aConversationId) throws RemoteException {
        try {
            ByteArrayOutputStream myBAOS =
                new ByteArrayOutputStream();

            AnnotatingObjectOutputStream myOOS =
                new AnnotatingObjectOutputStream(myBAOS, myBAOS);

            myOOS.writeObject(aMessage);
            myOOS.close();

            return new Message(aConversationId, myBAOS.toByteArray());
        } catch (IOException anIOE) {
            throw new RemoteException("Failed to marhsall", anIOE);
        }
    }

    public static Object unmarshall(Message aMessage) throws RemoteException {
        ByteArrayInputStream myBAIS =
            new ByteArrayInputStream(aMessage.getPayload());

        AnnotatingObjectInputStream myOIS = null;

        try {
            myOIS =
                new AnnotatingObjectInputStream(null, myBAIS, myBAIS, false);

            Object myResult = myOIS.readObject();

            myOIS.close();

            return myResult;

        } catch (Exception anE) {
            if (myOIS != null) {
                try {
                    myOIS.close();
                } catch (IOException anIOE) {
                }
            }

            throw new RemoteException("Failed to unmarshall", anE);
        }
    }
}
