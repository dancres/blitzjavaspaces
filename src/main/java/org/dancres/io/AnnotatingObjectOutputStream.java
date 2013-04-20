package org.dancres.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.rmi.server.RMIClassLoader;

import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
   Serializes a set of objects, separting annotations into one stream and
   object data into another.  This is useful in various situations including
   equality checking where one may not wish to include the codebase
   annotations.
 */
public class AnnotatingObjectOutputStream extends ObjectOutputStream {
    private static final Logger theLogger =
        Logger.getLogger("org.dancres.io.AnnotatingObjectOutputStream");

    private static final WeakHashMap theCachedCodebases = new WeakHashMap();

    private ObjectOutputStream theAnnotations;

    public AnnotatingObjectOutputStream(OutputStream anObjStream,
                                        OutputStream anAnnStream)
        throws IOException {

        super(anObjStream);
        theAnnotations = new ObjectOutputStream(anAnnStream);
    }

    protected void annotateClass(Class aClass) throws IOException {
        // addAnnotation(RMIClassLoader.getClassAnnotation(aClass));
        if (theLogger.isLoggable(Level.FINEST)) {
            theLogger.log(Level.FINEST, "Annotating " + aClass.getName() +
                ".class with " + getCodebase(aClass));
        }

        addAnnotation(getCodebase(aClass));
    }

    protected void annotateProxyClass(Class aClass) throws IOException {
        // addAnnotation(RMIClassLoader.getClassAnnotation(aClass));
        if (theLogger.isLoggable(Level.FINEST)) {
            theLogger.log(Level.FINEST, "Annotating proxy " + aClass.getName() +
                ".class with " + getCodebase(aClass));
        }

        addAnnotation(getCodebase(aClass));
    }

    private String getCodebase(Class aClass) {
        CodebaseHolder myCodebase;

        synchronized(theCachedCodebases) {
            myCodebase =
                    (CodebaseHolder) theCachedCodebases.get(aClass);

            if (myCodebase == null) {
                myCodebase =
                        new CodebaseHolder(RMIClassLoader.getClassAnnotation(aClass));
                theCachedCodebases.put(aClass, myCodebase);
            }
        }

        return myCodebase.getCodebase();
    }

    private void addAnnotation(String anAnnotation) throws IOException {
        theAnnotations.writeObject(anAnnotation);
    }

    public void flush() throws IOException {
        super.flush();
        theAnnotations.flush();
    }

    public void close() throws IOException {
        super.close();
        theAnnotations.close();
    }

    private static class CodebaseHolder {
        private String theCodebase;

        CodebaseHolder(String aCodebase) {
            theCodebase = aCodebase;
        }

        public String getCodebase() {
            return theCodebase;
        }
    }
}
