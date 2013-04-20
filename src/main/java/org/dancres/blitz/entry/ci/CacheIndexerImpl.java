package org.dancres.blitz.entry.ci;

import java.util.Set;

import java.util.logging.*;

import org.dancres.blitz.entry.EntrySleeve;
import org.dancres.blitz.entry.TupleLocator;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.cache.Identifiable;

import org.dancres.blitz.oid.OID;

/**
 * A skeleton implementation of a cache indexer.
 * Requires that the underlying index mechanism can support the CacheLines interface.
 *
 * @see org.dancres.blitz.entry.ci.CacheLines
 *
*/
public abstract class CacheIndexerImpl extends CacheIndexer {

    /**
       Because fields are ordered and always present, even if they are null
       we don't need to use a HashMap here.
    */
    private CacheLines[] theCacheLines;

    /**
       Contains all ids we know about which is required for a wildcard
       search.  A possibly better solution would iterate across all indexes but
       then there's the need to do intersection to prevent repeated passes over
       the same id which means much more complex code and might perform worse
       than this simple solution.
    */
    private Set theAllIds;

    private String theType;

    CacheIndexerImpl(String aType) {
        theType = aType;
        theAllIds = newIds();
    }

    abstract Set newIds();

    public void loaded(Identifiable anIdentifiable) {
        // System.err.println("CI:Loaded: " + anIdentifiable.getId() + "," + this);

        EntrySleeve mySleeve = (EntrySleeve) anIdentifiable;

        /*
          No point indexing something that's already deleted - we only want
          to generate load requests in response to explicit address'ing via
          UID.
         */
        if (mySleeve.isDeleted())
            return;

        initBarrier(mySleeve);

        insert(mySleeve);
    }

    public void flushed(Identifiable anIdentifiable) {
        // System.err.println("CI:Flushed: " + anIdentifiable.getId() + "," + this);

        EntrySleeve mySleeve = (EntrySleeve) anIdentifiable;
        initBarrier(mySleeve);

        remove(mySleeve);
    }

    public void dirtied(Identifiable anIdentifiable) {
        // System.err.println("CI:Dirtied: " + anIdentifiable.getId() + "," + this);

        EntrySleeve mySleeve = (EntrySleeve) anIdentifiable;
        initBarrier(mySleeve);

        if (mySleeve.isDeleted())
            remove(mySleeve);
    }

    public TupleLocator find(MangledEntry anEntry) {
        try {
            return findImpl(anEntry);
        } catch (ArrayIndexOutOfBoundsException anE) {
            theLogger.log(Level.SEVERE, "Find broke - did you add/remove fields in your Entry?", anE);
            theLogger.log(Level.SEVERE, "CacheIndexer for: " +
                          theType);
            theLogger.log(Level.SEVERE, "Entry looks like");
            theLogger.log(Level.SEVERE, "Entry type: " +
                          anEntry.getType());
            for (int i = 0; i < anEntry.getFields().length; i++) {
                MangledField myField = anEntry.getField(i);

                theLogger.log(Level.SEVERE, "Name: " +
                              myField.getName() + ", hashcode:" +
                              myField.hashCode() + ", offset:" +
                              i + ", isNull: " +
                              myField.isNull());
            }
            theLogger.log(Level.SEVERE, "Cachelines looks like");
            theLogger.log(Level.SEVERE, "Cachelines size:" +
                          theCacheLines.length);
            for (int i = 0; i < theCacheLines.length; i++) {
                theLogger.log(Level.SEVERE, "Name: " +
                              theCacheLines[i].getName() +
                              " Offset: " + i +
                              " size: " + 
                              theCacheLines[i].getSize());
            }

            throw anE;
        }
    }

    private TupleLocator findImpl(MangledEntry anEntry) {
        /*
          Unlike the cache listener methods which can only be called with
          an entry of this indexers type, there's a possibility this method's
          first entry will be a different type.  If it is, it can't be used
          to init the barrier - thus, if we fail to init the barrier we should
          return an empty locator
        */
        if (!initBarrier(anEntry))
            return ArrayLocatorImpl.EMPTY_LOCATOR;

        if ((anEntry == null) || (anEntry.isWildcard())) {
            // Handle wildcard requests
            //
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Wildcard match");

            synchronized(theAllIds) {
                if (theAllIds.size() == 0)
                    return ArrayLocatorImpl.EMPTY_LOCATOR;
                else {
                    OID[] myUids = new OID[theAllIds.size()];
                    myUids = (OID[]) theAllIds.toArray(myUids);

                    return new ArrayLocatorImpl(myUids);
                }
            }
        } else {
            // Proper indexing effort
            //
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Specific match");

            MangledField myChoice = null;
            int myChoicesSize = 0;
            int myChoicesOffset = 0;

            if ((anEntry.getType().equals(theType)) &&
                    (anEntry.getFields().length != theCacheLines.length))
                    theLogger.log(Level.WARNING, "Possible schema change detected - matching may fail" + theType);
            
            // Find the smallest index available
            for (int i = 0; i < anEntry.getFields().length; i++) {
                MangledField myField = anEntry.getField(i);
                int mySize;

                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, "Consider: " + myField.getName());

                // If field is empty, we can't search on it
                if (myField.isNull())
                    continue;

                mySize = getSize(myField, i);

                /*
                  Field is searchable - but, if we get no hits from cache
                  indexes, that means we have no matches in memory.  This
                  works because for a match to succeed it must match on 
                  all fields in the template.  Thus if the template has a
                  field with a hashcode that produces no hits then whilst
                  we may have Entry's that match other fields of the template
                  we know that none of these Entry's will match this field
                  of the template so we should stop now.
                 */
                if (mySize == 0) {
                    if (theLogger.isLoggable(Level.FINE))
                        theLogger.log(Level.FINE,
                                      "One key not indexed - abort");
                    return ArrayLocatorImpl.EMPTY_LOCATOR;
                }

                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, "Available size: " + mySize);

                if (myChoice == null) {
                    myChoice = myField;
                    myChoicesSize = mySize;
                    myChoicesOffset = i;
                } else {
                    if (mySize < myChoicesSize) {
                        myChoice = myField;
                        myChoicesSize = mySize;
                        myChoicesOffset = i;
                    }
                }
            }

            if (myChoice == null) {
                return ArrayLocatorImpl.EMPTY_LOCATOR;
            } else {
                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, "Chose: " + myChoicesSize +
                                  myChoice.getName());
                return getIds(myChoice, myChoicesOffset);
            }
        }
    }
    
    private TupleLocator getIds(MangledField aField, int anOffset) {
        CacheLines myLine = getCacheLines(anOffset);
        return myLine.getIds(aField.hashCode());
    }

    private int getSize(MangledField aField, int anOffset) {
        CacheLines myLine = getCacheLines(anOffset);
        return myLine.getSize(aField.hashCode());
    }

    private boolean initBarrier(EntrySleeve aSleeve) {
        synchronized(this) {
            if (theCacheLines == null)
                return initBarrier(aSleeve.getEntry());
            else
                return true;
        }
    }

    /**
       @return true if the barrier is already inited or if
       the barrier was successfully inited, false otherwise.
    */
    private boolean initBarrier(MangledEntry anEntry) {
        synchronized(this) {
            if (theCacheLines == null) {
                theLogger.log(Level.FINE,
                              "Initing cache indexer: " + theType);

                if (!anEntry.getType().equals(theType)) {
                    theLogger.log(Level.FINE,
                                  "Can't init indexer with this: " +
                                  anEntry.getType());
                    return false;
                }

                theLogger.log(Level.FINE, "Entry looks like");
                theLogger.log(Level.FINE, "Entry type: " +
                              anEntry.getType());
                for (int i = 0; i < anEntry.getFields().length; i++) {
                    MangledField myField = anEntry.getField(i);

                    theLogger.log(Level.FINE, "Name: " +
                                  myField.getName() + ", hashcode:" +
                                  myField.hashCode() + ", offset:" +
                                  i + ", isNull: " +
                                  myField.isNull());
                }

                MangledField[] myFields = anEntry.getFields();
                theCacheLines = newLinesArray(myFields.length);

                for (int i = 0; i < myFields.length; i++) {
                    theCacheLines[i] = newLines(i, myFields[i].getName());
                }
            }
            
            return true;
        }
    }

    abstract CacheLines[] newLinesArray(int aSize);

    abstract CacheLines newLines(int anIndex, String aFieldName);

    private CacheLines getCacheLines(int anOffset) {
        return theCacheLines[anOffset];
    }

    /**
       Add a sleeve to the indexer
    */
    private void insert(EntrySleeve aSleeve) {
        synchronized(theAllIds) {

            if (theAllIds.add(aSleeve.getOID())) {
                for (int i = 0; i < theCacheLines.length; i++) {
                    theCacheLines[i].insert(aSleeve);
                }
            } else {
                // System.err.println("Already added");
            }
        }
    }
    
    /**
       Remove a sleeve from the indexer
    */
    private void remove(EntrySleeve aSleeve) {
        synchronized(theAllIds) {

            if (theAllIds.remove(aSleeve.getOID())) {
                for (int i = 0; i < theCacheLines.length; i++) {
                    theCacheLines[i].remove(aSleeve);
                }
                /*
                new RuntimeException(
                    "Removed: " + aSleeve.getOID()).printStackTrace(
                    System.err);
                    */
            } else {
                // new RuntimeException("Already removed: " + aSleeve.getOID()).printStackTrace(System.err);
            }
        }
    }
}
