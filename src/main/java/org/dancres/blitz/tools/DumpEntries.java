package org.dancres.blitz.tools;

import java.io.IOException;

import java.rmi.RMISecurityManager;
import java.util.Set;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.entry.EntryRepositoryFactory;
import org.dancres.blitz.entry.EntryRepository;
import org.dancres.blitz.entry.SearchVisitor;
import org.dancres.blitz.entry.SearchOffer;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.mangler.EntryMangler;

/**
   <p>DumpEntries performs an off-line dump of the contents of a Blitz
   instance.</p>

   <p><b>WARNING:</b> <em>DO NOT</em> run this tool against an active space
   instance's databases.  Shut it down first using
   <code>SyncAndShutdown</code>.</p>

   <p>Single required argument is the configuration file for the space instance
   whose contents you wish to dump.  By default, the tool will print both
   internal Entry information associated with storage functions <em>and</em>
   attempt to unpack the Entry instance and display it on
   <code>System.out</code>.  The displaying of unpacked Entry instances may
   be problematic in cases such as when a codebase is not available thus
   this step can be disabled by passing in <code>-Dnounpack=true</code>.</p>

   <p>Typical usages:

   <pre>
   java -Xmx256m -Djava.security.policy=config/policy.all
     -classpath /home/dan/lib/db.jar:/home/dan/jini/jini2_0/lib/jsk-platform.jar:/home/dan/src/jini/space/build:/home/dan/jini/jini2_0/lib/jini-ext.jar:/home/dan/jini/jini2_0/lib/sun-util.jar
     org.dancres.blitz.tools.DumpEntries config/blitz.config

   java -Dnounpack=true -Xmx256m -Djava.security.policy=config/policy.all
     -classpath /home/dan/lib/db.jar:/home/dan/jini/jini2_0/lib/jsk-platform.jar:/home/dan/src/jini/space/build:/home/dan/jini/jini2_0/lib/jini-ext.jar:/home/dan/jini/jini2_0/lib/sun-util.jar
     org.dancres.blitz.tools.DumpEntries config/blitz.config
   </pre>
   </p>

   <p>This tool is non-destructive so it's perfectly possible to shut a
   Blitz instance down using <code>SyncAndShutdown</code>,
   run this tool and then restart the space.</p>

   <p>It's also worth mentioning that, because Blitz's transient mode is
   still disk backed to allow for swapping, this tool can be used to examine
   the contents of a transient Blitz instance shutdown with <code>
   SyncAndShutdown</code>.</p>

   @see org.dancres.blitz.tools.SyncAndShutdown
 */
public class DumpEntries {
    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("Usage: DumpEntries <config_file>");
            System.exit(-1);
        }

        /**
           We need this to access codebases for objects
         */
        System.setSecurityManager(new RMISecurityManager());

        try {
            ConfigurationFactory.setup(args);

            Disk.init();

            Set<String> myKnownTypes =
                EntryRepositoryFactory.get().get(EntryRepository.ROOT_TYPE).getSubtypes();

            for (String t: myKnownTypes) {
                EntryRepository myRepos =
                    EntryRepositoryFactory.get().get(t);
                
                System.out.println("Repository: " + t);
                dumpRepos(myRepos);

                System.out.println("");
            }

        } catch (IOException anIOE) {
            System.err.println("Yikes, got I/O problems");
            anIOE.printStackTrace(System.err);
        } finally {
            try {
                Disk.stop();
            } catch (Exception anE) {
                System.err.println("Warning failed to close Disk properly");
                anE.printStackTrace(System.err);
            }
        }
    }

    private static void dumpRepos(EntryRepository aRepos) throws IOException {
        if (aRepos == null) {
            System.out.println("Empty");
            return;
        }

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            if (aRepos.noSchemaDefined()) {
                System.out.println("Has No Schema");
                return;
            }

            if (aRepos.getTotalStoredEntries() == 0) {
                System.out.println("No entries");
                return;
            } else {
                System.out.println("Total entries: " +
                                   aRepos.getTotalStoredEntries());
            }

            System.out.println("");
            DumpVisitor myVisitor = new DumpVisitor();

            aRepos.find(MangledEntry.NULL_TEMPLATE, myVisitor);

            System.out.println("Lease expired entries (ignored): " +
                               (aRepos.getTotalStoredEntries() - myVisitor.getTotalEntries()));
        } finally {
            myTxn.commit();
        }
    }

    private static final class DumpVisitor implements SearchVisitor {
        private EntryMangler theMangler = new EntryMangler();
        private int theTotalEntries;

        public int offer(SearchOffer anOffer) {

            ++theTotalEntries;

            MangledEntry myEntry = anOffer.getEntry();
            myEntry.dump(System.out);

            System.out.println();

            if (!Boolean.getBoolean("nounpack")) {
                try {
                    System.out.println("Unpacked Entry.toString(): " + theMangler.unMangle(myEntry));
                } catch (Exception anE) {
                    System.err.println("Couldn't unpack entry");
                    anE.printStackTrace(System.err);
                }
            } else {
                System.out.println("Unpack disabled");
            }

            System.out.println("");
            System.out.println("--------------------------------------------");
            System.out.println("");

            return SearchVisitor.TRY_AGAIN;
        }

        int getTotalEntries() {
            return theTotalEntries;
        }

        public boolean isDeleter() {
            return false;
        }
    }
}
