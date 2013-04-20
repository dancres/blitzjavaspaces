package org.dancres.blitz.txn;

import java.io.File;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.config.StorageModel;
import org.dancres.blitz.config.Persistent;
import org.dancres.blitz.config.TimeBarrierPersistent;
import org.dancres.blitz.config.Transient;
import org.dancres.blitz.config.ConfigurationFactory;

/**
   @see org.dancres.blitz.txn.StoragePersonality
 */
public class StoragePersonalityFactory {

    private static Object _lock = new Object();

    private static StorageModel STORAGE_MODEL;

    private static StoragePersonality STORAGE_PERSONALITY;

    private static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            synchronized(_lock) {
                STORAGE_MODEL = null;
                STORAGE_PERSONALITY = null;
            }
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }

    private static void init() {
        try {
            STORAGE_MODEL =
                ((StorageModel)
                 ConfigurationFactory.getEntry("storageModel",
                                               StorageModel.class));
            
            String myLogDir = (String)
                ConfigurationFactory.getEntry("logDir", String.class);

            new File(myLogDir).mkdirs();

            if (STORAGE_MODEL instanceof Persistent) {
                STORAGE_PERSONALITY = 
                    new PersistentPersonality((Persistent) STORAGE_MODEL,
                                              myLogDir);
            } else if (STORAGE_MODEL instanceof Transient) {
                STORAGE_PERSONALITY = new TransientPersonality(myLogDir);
            } else if (STORAGE_MODEL instanceof TimeBarrierPersistent) {
                STORAGE_PERSONALITY =
                    new TimeBarrierPersonality((TimeBarrierPersistent) STORAGE_MODEL, myLogDir);
            } else {
                throw new Error("Unrecognised storage personality, fatal: " + STORAGE_MODEL);
            }

        } catch (ConfigurationException aCE) {
            throw new Error("Problem loading configuration", aCE);
        }
    }

    public static StoragePersonality getPersonality() {
        synchronized(_lock) {
            if (STORAGE_MODEL == null)
                init();
        }

        return STORAGE_PERSONALITY;
    }
}
