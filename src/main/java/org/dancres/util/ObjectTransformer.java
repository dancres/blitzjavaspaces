package org.dancres.util;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
   Converts an object to a byte-array or vice versa
 */
public class ObjectTransformer {
    public static byte[] toByte(Serializable anObject) throws IOException {
        ByteArrayOutputStream myBAOS = new ByteArrayOutputStream();
        ObjectOutputStream myOOS = new ObjectOutputStream(myBAOS);

        myOOS.writeObject(anObject);
        myOOS.close();

        return myBAOS.toByteArray();
    }

    public static Serializable toObject(byte[] aFlattenedObject) 
        throws IOException {

        try {
            ByteArrayInputStream myBAIS =
                new ByteArrayInputStream(aFlattenedObject);
            ObjectInputStream myOIS = new ObjectInputStream(myBAIS);

            Serializable myObject = (Serializable) myOIS.readObject();
            myOIS.close();

            return myObject;
        } catch (ClassNotFoundException aCNFE) {
            throw new IOException("ClassNotFoundException: " +
                                  aCNFE.toString());
        }
    }
}
