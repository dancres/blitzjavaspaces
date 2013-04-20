package org.dancres.blitz.arc;

import java.io.IOException;

import java.util.HashMap;
import java.util.Random;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;

public class ArcTest {
    private static Random theRNG = new Random();
    private static int theMaxEntries;
    private static ArcCache theCache;

    public static void main(String args[]) {
        BackingStore myStore = new BackingStoreImpl();

        System.out.println("ArcTest with CacheSize: " + args[0] +
                           " Max Entry:" + args[1]);

        theCache = new ArcCache(myStore, Integer.parseInt(args[0]));

        theMaxEntries = Integer.parseInt(args[1]);

        try {
            // Write the entries in
            for (int i = 0; i < theMaxEntries; i++) {
                IdentifierImpl myId = new IdentifierImpl(i);
                Element myElement = new Element(myId, i);
                theCache.insert(myElement);
            }

            while (true) {
                int myChoice = theRNG.nextInt(2);

                switch(myChoice) {
                    case 0 : {
                        randomSelect();
                        break;
                    }

                    case 1 : {
                        randomRange();
                        break;
                    }
                }
            }
        } catch (Exception anE) {
            System.err.println("Exceptioned");
            anE.printStackTrace(System.err);
        }
    }

    private static final void randomSelect() throws Exception {
        // Random select
        int myInt = theRNG.nextInt(theMaxEntries);

        System.out.println("Random: " + myInt);
        CacheBlockDescriptor myCBD =
            theCache.find(new IdentifierImpl(myInt));

        myCBD.release();
    }

    private static final void randomRange() throws Exception {
        // Random start point
        int myStart = theRNG.nextInt(theMaxEntries);

        int myLength = theRNG.nextInt((theMaxEntries - myStart));

        System.out.println("Linear: " + myStart + ", " + myLength);

        for (int i = myStart; i < (myStart + myLength); i++) {
            CacheBlockDescriptor myCBD =
                theCache.find(new IdentifierImpl(i));

            myCBD.release();
        }
    }

    private static final class IdentifierImpl implements Identifier {
        private int theInt;

        IdentifierImpl(int anInt) {
            theInt = anInt;
        }

        public boolean equals(Object anObject) {
            if (anObject instanceof IdentifierImpl) {
                IdentifierImpl myOther = (IdentifierImpl) anObject;

                return (myOther.theInt == theInt);
            }

            return false;
        }

        public int hashCode() {
            return theInt;
        }

        public int compareTo(Object anObject) {
            IdentifierImpl myOther = (IdentifierImpl) anObject;

            return theInt - myOther.theInt;
        }

        public String toString() {
            return "Id: " + theInt;
        }
    }

    private static final class Element implements Identifiable {
        private IdentifierImpl theId;
        private int theVal;

        Element(IdentifierImpl anId, int aVal) {
            theId = anId;
            theVal = aVal;
        }

        public Identifier getId() {
            return theId;
        }

        public String toString() {
            return "Element: " + theVal;
        }
    }

    private static final class BackingStoreImpl implements BackingStore {
        private HashMap thePrepared = new HashMap();
        private HashMap theStorage = new HashMap();

        public String getName() {
            return "ArcTest::BackingStoreImpl";
        }

        public void prepareForCaching(Identifiable anIdentifiable) {
            synchronized(thePrepared) {
                thePrepared.put(anIdentifiable.getId(), anIdentifiable);
            }
        }

        /**
           @return Identifiable associated with Identifier or <code>null</code>
           if it cannot be found.
        */
        public Identifiable load(Identifier anId) throws IOException {
            Identifiable myIdent;

            // First check prepcache
            synchronized(thePrepared) {
                myIdent = (Identifiable) thePrepared.remove(anId);

                if (myIdent != null) {
                    synchronized(theStorage) {
                        theStorage.put(anId, myIdent);
                    }

                    return myIdent;
                }
            }

            synchronized(theStorage) {
                myIdent = (Identifiable) theStorage.get(anId);

                if (myIdent == null) {
                    System.err.println("Panic: storage lost entry");
                    throw new RuntimeException();
                } else
                    return myIdent;
            }
        }

        /**
           Must deal with handling of delete, update and write.
        */
        public void save(Identifiable anIdentifiable) throws IOException {
            // Nothing for us to do in this case
        }

        public void force() throws IOException {
            // Nothing for us to do in this case
        }
    }
}
