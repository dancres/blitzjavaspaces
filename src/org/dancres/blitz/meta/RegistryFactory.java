package org.dancres.blitz.meta;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.disk.Disk;

public class RegistryFactory {
    private static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            synchronized(theMetas) {
                theMetas.clear();
            }
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }

    private static Map theMetas = new HashMap();

    /**
       @param aName the name of the registry you wish to load/create
       @param anInitializer Registry instances are created under a transaction.
       Certain uses may require that initialization be done under the same
       transaction.  In these cases, the caller should pass in an Initializer
       instance containing the appropriate "boot code".  If no initialization
       is required, the caller may pass <code>null</code>.
     */
    public static Registry get(String aName, Initializer anInitializer)
        throws IOException {

        synchronized(theMetas) {
            Registry myData = (Registry) theMetas.get(aName);

            if (myData == null) {
                myData = new RegistryImpl(aName, anInitializer);
                theMetas.put(aName, myData);
            }

            return myData;
        }
    }

    public static boolean exists(String aName) {
        return RegistryImpl.exists(aName);
    }

    public static void delete(String aName) throws IOException {
        synchronized(theMetas) {
            RegistryImpl myData = (RegistryImpl) theMetas.get(aName);

            if (myData != null) {
                theMetas.remove(aName);
                myData.delete();
            }
        }
    }
}
