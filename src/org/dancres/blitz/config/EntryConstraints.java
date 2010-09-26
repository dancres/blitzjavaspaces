package org.dancres.blitz.config;

import java.util.HashMap;

import java.util.logging.Level;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.config.*;

/**
 * @todo Move this into the config package and add a factory to track all
 * of them.  Also define a MINIMAL_CONSTRAINTS static which is an instance of
 * this class created via the null constructor.
 */
public class EntryConstraints {

    public static final EntryConstraints MINIMUM =
        new EntryConstraints();

    private static int DEFAULT_CACHE_SIZE;
    private static int DEFAULT_READ_AHEAD;

    static {
        try {
            DEFAULT_CACHE_SIZE =
                ((Integer)
                 ConfigurationFactory.getEntry("entryReposCacheSize",
                                               int.class,
                                               new Integer(200))).intValue();

            ConfigurationFactory.theLogger.log(Level.INFO,
                                      "Cache size: " + DEFAULT_CACHE_SIZE);

            DEFAULT_READ_AHEAD =
                ((Integer)
                 ConfigurationFactory.getEntry("entryReposReadahead",
                                               int.class,
                                               new Integer(0))).intValue();

            ConfigurationFactory.theLogger.log(Level.INFO,
                                      "Read ahead: " + DEFAULT_READ_AHEAD);
        } catch (ConfigurationException aCE) {
        }
    }

    static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            theAllConstraints.clear();
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }


    private static HashMap theAllConstraints = new HashMap();

    private HashMap theConstraints = new HashMap();
    private String theType;

    public static EntryConstraints getConstraints(String aType)
        throws ConfigurationException {
        synchronized(theAllConstraints) {
            EntryConstraints myConstraints =
                (EntryConstraints) theAllConstraints.get(aType);

            if (myConstraints == null) {
                myConstraints = new EntryConstraints(aType);

                theAllConstraints.put(aType, myConstraints);
            }

            return myConstraints;
        }
    }

    /**
       Create an instance of EntryConstraints with no settings loaded from
       config and a minimal set of useful constraints.
     */
    private EntryConstraints() {
        theConstraints.put(CacheSize.class, new CacheSize(1));
    }

    private EntryConstraints(String aType) throws ConfigurationException {
        theType = aType;

        init();
    }

    private void init() throws ConfigurationException {
        String myConfigName = theType.replaceAll("\\.", "_");
        myConfigName = myConfigName.replaceAll("\\$", "_");

        EntryConstraint[] myConstraints = (EntryConstraint[])
            ConfigurationFactory.getEntry(myConfigName,
                                          EntryConstraint[].class,
                                          new EntryConstraint[0]);

        for (int i = 0; i < myConstraints.length; i++) {
            theConstraints.put(myConstraints[i].getClass(), myConstraints[i]);
        }

        if (theConstraints.get(CacheSize.class) == null) {
            theConstraints.put(CacheSize.class,
                               new CacheSize(DEFAULT_CACHE_SIZE));
        }

        // If FIFO is enabled, force read ahead to zero
        //
        if (theConstraints.get(Fifo.class) != null) {
            theConstraints.put(ReadAhead.class, new ReadAhead(0));
        } else {
            if (theConstraints.get(ReadAhead.class) == null) {
                theConstraints.put(ReadAhead.class,
                        new ReadAhead(DEFAULT_READ_AHEAD));
            }
        }
    }

    public EntryConstraint get(Class aConstraint) {
        EntryConstraint myResult =
            (EntryConstraint) theConstraints.get(aConstraint);

        return myResult;
    }
}