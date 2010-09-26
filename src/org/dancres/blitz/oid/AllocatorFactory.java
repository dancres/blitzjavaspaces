package org.dancres.blitz.oid;

import java.io.File;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.Registry;

public class AllocatorFactory {

    private static int DEFAULT_MAX_ALLOCATORS;

    static {
        try {
            DEFAULT_MAX_ALLOCATORS =
                ((Integer)
                 ConfigurationFactory.getEntry("maxOidAllocators", 
                                               int.class,
                                               new Integer(4))).intValue();
        } catch (ConfigurationException aCE) {
        }
    }

    private static Map theAllocators = new HashMap();

    private static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            synchronized(theAllocators) {
                Iterator myAllocs = theAllocators.values().iterator();
                while (myAllocs.hasNext()) {
                    ((AllocatorAdmin) myAllocs.next()).discard();
                }

                theAllocators.clear();
            }
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }
    
    public static Allocator get(String aName, boolean isFifo)
        throws IOException {
        return get(aName, DEFAULT_MAX_ALLOCATORS, isFifo);
    }

    public static Allocator get(String aName, int anAllocSpaceSize,
                                boolean isFifo)
        throws IOException {
        return getImpl(aName, anAllocSpaceSize, isFifo);
    }

    static Allocator getImpl(String aName, int anAllocSpaceSize,
                             boolean isFifo)
        throws IOException {

        synchronized(theAllocators) {
            Allocator myData = (Allocator) theAllocators.get(aName);

            if (myData == null) {
                myData = newAllocator(aName, anAllocSpaceSize, isFifo);
                theAllocators.put(aName, myData);
            }

            return myData;
        }
    }

    public static void delete(String aName) throws IOException {
        synchronized(theAllocators) {
            AllocatorAdmin myData =
                (AllocatorAdmin) theAllocators.remove(aName);

            if (myData != null)
                myData.delete();
        }
    }

    private static Allocator newAllocator(String aName,
                                          int anAllocSpaceSize,
                                          boolean isFifo) 
        throws IOException {

        if (isFifo)
            return new FIFOAllocatorImpl(aName);
        else
            return new AllocatorImpl(aName, anAllocSpaceSize);
    }
}
