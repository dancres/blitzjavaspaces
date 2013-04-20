package org.dancres.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.ObjectStreamClass;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.loader.ClassLoading;

import net.jini.io.ObjectStreamContext;
import net.jini.io.context.IntegrityEnforcement;

/**
   Takes a stream of codebase annotations and a second stream of object data
   and combines them to produce de-serialized objects.  Object verification
   can be optionally enabled at construction time.
 */
public class AnnotatingObjectInputStream
    extends ObjectInputStream implements ObjectStreamContext {

    private static final Logger theLogger =
        Logger.getLogger("org.dancres.io.AnnotatingObjectInputStream");

    private ObjectInputStream theAnnotations;
    private ClassLoader theLoader;

    private Collection theContext;

    private boolean requireIntegrityCheck;

    private static final Map thePrimitiveClasses = new HashMap();
    static {
        thePrimitiveClasses.put("boolean", boolean.class);
        thePrimitiveClasses.put("byte", byte.class);
        thePrimitiveClasses.put("char", char.class);
        thePrimitiveClasses.put("short", short.class);
        thePrimitiveClasses.put("int", int.class);
        thePrimitiveClasses.put("long", long.class);
        thePrimitiveClasses.put("float", float.class);
        thePrimitiveClasses.put("double", double.class);
        thePrimitiveClasses.put("void", void.class);
    }

    public AnnotatingObjectInputStream(ClassLoader aLoader,
                                       InputStream anObjStream,
                                       InputStream anAnnStream,
                                       final boolean checkIntegrity)
        throws IOException {

        super(anObjStream);
        theAnnotations = new ObjectInputStream(anAnnStream);
        theLoader = aLoader;
        requireIntegrityCheck = checkIntegrity;

        theContext =
            Collections.singleton(
                                  new IntegrityEnforcement() {
                                      public boolean integrityEnforced() {
                                          return checkIntegrity;
                                      }
                                  }
                                  );
    }

    public Collection getObjectStreamContext() {
        return theContext;
    }

    private String getAnnotation() throws IOException, ClassNotFoundException {
        return (String) theAnnotations.readObject();
    }

    protected Class resolveClass(ObjectStreamClass aClassDesc)
        throws IOException, ClassNotFoundException {

        String myAnnotation = getAnnotation();
        String myClassName = aClassDesc.getName();

        Class myClass = (Class) thePrimitiveClasses.get(myClassName);

        if (myClass != null)
            return myClass;

        if (theLogger.isLoggable(Level.FINEST)) {
            theLogger.log(Level.FINEST, "Attempting to load " + myClassName +
                ".class from " + myAnnotation);
        }
        
        return ClassLoading.loadClass(myAnnotation, myClassName, theLoader,
                                      requireIntegrityCheck, null);
    }

    protected Class resolveProxyClass(String[] aListOfInterfaces)
        throws IOException, ClassNotFoundException {

        String myAnnotation = getAnnotation();

        return ClassLoading.loadProxyClass(myAnnotation, aListOfInterfaces,
                                           theLoader, requireIntegrityCheck,
                                           null);
    }

    public void close() throws IOException {
        super.close();
        theAnnotations.close();
    }
}
