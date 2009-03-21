package org.dancres.blitz.mangler;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

import org.dancres.io.AnnotatingObjectOutputStream;
import org.dancres.io.AnnotatingObjectInputStream;

/**
   Each field within an Entry is held in a MangledField.  We require the
   field's name for indexing purposes.  We also use our own hashing
   algorithm which is known not to "funnel" in the vast majority of cases.
 */
public class MangledField implements Serializable {
    static final long serialVersionUID = -6256197142057463399L;

    private String theName;
    private byte[] theObjectBytes;
    private byte[] theAnnotationBytes;
    private int theHashCode;

    MangledField(String aName) {
        theName = aName;
        theObjectBytes = new byte[0];
        theAnnotationBytes = new byte[0];
        theHashCode = 0;
    }

    MangledField(String aName, Object anObject)
        throws IOException {
        theName = aName;
        mangle(anObject);
    }

    MangledField(String aName, byte[] anObject, byte[] aAnnotation, int aHash) {
        theName = aName;
        theObjectBytes = anObject;
        theAnnotationBytes = aAnnotation;
        theHashCode = aHash;
    }

    public int sizeOf() {
        if (isNull())
            return 0;
        else
            return theName.length() + theObjectBytes.length +
                theAnnotationBytes.length + 4 + 4;
    }

    public Object unMangle(ClassLoader aDefault, boolean checkIntegrity)
        throws IOException, ClassNotFoundException {

        ByteArrayInputStream myObjStream =
            new ByteArrayInputStream(theObjectBytes);
        ByteArrayInputStream myAnnoStream =
            new ByteArrayInputStream(theAnnotationBytes);

        AnnotatingObjectInputStream myStream =
            new AnnotatingObjectInputStream(aDefault, myObjStream,
                                            myAnnoStream, checkIntegrity);

        Object myResult = myStream.readObject();

        myStream.close();

        return myResult;
    }
  
    public boolean isNull() {
        // System.err.println("isNull: " + theName);
        // System.err.println("OLength: " + theObjectBytes.length);
        // System.err.println("ALength: " + theAnnotationBytes.length);
        return (theObjectBytes.length == 0);
    }

    private void mangle(Object anObject) throws IOException {
        ByteArrayOutputStream myObjectBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream myAnnotBytes = new ByteArrayOutputStream();
        
        AnnotatingObjectOutputStream myStream = 
            new AnnotatingObjectOutputStream(myObjectBytes, myAnnotBytes);

        myStream.writeObject(anObject);
        myStream.close();

        theObjectBytes = myObjectBytes.toByteArray();
        theAnnotationBytes = myAnnotBytes.toByteArray();

        theHashCode = buildHash(theObjectBytes);
    }

    /**
       This algorithm is based on the "One at a time" hash published in
       Dr Dobbs Journal '97 and authored by Bob Jenkins.  A copy of the
       article can be found at
       <a href="http://burtleburtle.net/bob/hash/doobs.html">
       Burtleburtle.net</a>
    */
    private int buildHash(byte[] aBytes) {
        int myHash = 0;

        for (int i = 0; i < aBytes.length; i++) {
            myHash += aBytes[i];
            myHash += (myHash << 10);
            myHash ^= (myHash >> 6);
        }

        myHash += (myHash << 3);
        myHash ^= (myHash >> 11);
        myHash += (myHash << 15);

        return myHash;
    }

    public boolean matches(MangledField aField) {
        if (aField.theHashCode == theHashCode) {
            if (aField.theObjectBytes.length == theObjectBytes.length) {
                for (int i = 0; i < theObjectBytes.length; i++) {
                    if (aField.theObjectBytes[i] != theObjectBytes[i])
                        return false;
                }

                return true;
            }
        }

        return false;
    }

    public String getName() {
        return theName;
    }

    byte[] getContent() {
        return theObjectBytes;
    }

    byte[] getAnnotations() {
        return theAnnotationBytes;
    }

    public int hashCode() {
        return theHashCode;
    }
}
