package org.dancres.blitz.mangler;

import java.io.IOException;

import java.rmi.server.RMIClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import java.util.WeakHashMap;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.loader.ClassLoading;

/**
   This class takes an Entry instance and reduces it down into it's
   constituent parts (MangledEntry) which can be stored in the server-side.

   @todo Hints about the kind of indexing for an Entry type are an
   interesting idea - sometimes we're dealing with a queue, sometimes
   hashing and sometimes neither.  We could achieve it with a simple
   static field with a fixed name and a possible set of values.  Non-standard
   but interesting......We could even set a default of indexed so we can
   implement it and do something sane by default but possibly more optimal
   with a hint.

   @todo Consider adding test for Entry being a public class with a public
   no-args constructor
 */
public class EntryMangler {
    private static final Logger theLogger =
            Logger.getLogger("org.dancres.blitz.mangler.EntryMangler");

    private static final int INVALID_MODIFIERS = Modifier.TRANSIENT |
        Modifier.STATIC | Modifier.FINAL;

    private static final Comparator theFieldSorter = new FieldSorter();

    private static EntryMangler theMangler = new EntryMangler();

    public static EntryMangler getMangler() {
        return theMangler;
    }

    /**
       DEBUG AND TESTING ONLY
     */
    public EntryMangler() {
    }

    public MangledEntry mangle(Entry anEntry) {
        return mangle(anEntry, false);
    }

    /**
     * @param isSnapshot indicates whether this mangle is for a snapshot operation on
     * a template Entry 
     */
    public MangledEntry mangle(Entry anEntry, boolean isSnapshot) {
        Class myEntryClass = anEntry.getClass();
        String myClassName = myEntryClass.getName();
        String myCodebase = RMIClassLoader.getClassAnnotation(myEntryClass);

        MangledField[] myEntryFields = getFields(anEntry);
        String[] myParents = getParents(myEntryClass);

        // Is it wildcard - i.e. all fields are null
        boolean isWildcard = true;
        for (int i = 0; i < myEntryFields.length; i++) {
            if (! myEntryFields[i].isNull()) {
                isWildcard = false;
                break;
            }
        }

        return new MangledEntry(myClassName, myCodebase, myEntryFields,
                                myParents, isWildcard, isSnapshot);
    }

    public Entry unMangle(MangledEntry anME) throws UnusableEntryException {
        try {
            Class myEntryClass =
                ClassLoading.loadClass(anME.getCodebase(), anME.getType(),
                                       null, anME.needsIntegrityCheck(), null);

            Entry myEntry = (Entry) myEntryClass.newInstance();

            Field[] myFields = getFields(myEntryClass);

            MangledField[] myMangledFields = anME.getFields();
            int myNextField = 0;

            for (int i = 0; i < myFields.length; i++) {
                if (theLogger.isLoggable(Level.FINEST)) {
                    theLogger.finest("Looking at field: " +
                            myFields[i].getName());
                    theLogger.finest("Valid:" +
                            myMangledFields[myNextField]);
                    theLogger.finest("Hash: " +
                            myMangledFields[myNextField].hashCode());
                }
                if (! myMangledFields[myNextField].isNull()) {
                    if (theLogger.isLoggable(Level.FINEST)) {
                        theLogger.finest("Not null");
                        theLogger.finest("Copy from: " +
                                myMangledFields[myNextField].getName());
                        theLogger.finest("Value will be: " +
                                myMangledFields[myNextField].unMangle(null,
                                        anME.needsIntegrityCheck()));
                    }
                    myFields[i].set(myEntry,
                                    myMangledFields[myNextField++].unMangle(null,
                                            anME.needsIntegrityCheck()));
                } else
                    myNextField++;
            }

            return myEntry;
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Error during unpack: ", anE);
            throw new UnusableEntryException(anE);
        }
    }

    private static Map theParentsCache = new WeakHashMap();

    private String[] getParents(Class aClass) {
        String[] myParents = null;

        synchronized(theParentsCache) {
            myParents = (String[]) theParentsCache.get(aClass);

            if (myParents == null) {
                ArrayList mySuperClasses = new ArrayList();

                for(Class myCurrentClass = aClass.getSuperclass();
                    myCurrentClass != null;
                    myCurrentClass = myCurrentClass.getSuperclass()) {

                    mySuperClasses.add(myCurrentClass.getName());
                }

                myParents = new String[mySuperClasses.size()];
                Object[] myClasses = mySuperClasses.toArray();
                System.arraycopy(myClasses, 0, myParents, 0, myParents.length);

                theParentsCache.put(aClass, myParents);
            }
        }

        return myParents;
    }

    /**
       We wish to sort fields into order with those of super-class before
       subclass.  We're using alphabetical order for fields within one
       particular class but, in reality, any predicatable ordering would do.
     */
    private static class FieldSorter implements Comparator {
        public int compare(Object anObject, Object anotherObject) {
            Field myFirst = (Field) anObject;
            Field mySecond = (Field) anotherObject;

            if (myFirst == mySecond)
                return 0;
            else if (myFirst.getDeclaringClass() ==
                     mySecond.getDeclaringClass()) {
                return myFirst.getName().compareTo(mySecond.getName());
            } else if (myFirst.getDeclaringClass().isAssignableFrom(mySecond.getDeclaringClass())) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private static Map theFieldCache = new WeakHashMap();

    private Field[] getFields(Class aClass) {
        synchronized(theFieldCache) {
            Field[] myFields = (Field[]) theFieldCache.get(aClass);

            if (myFields == null) {
                myFields = aClass.getFields();
                Arrays.sort(myFields, theFieldSorter);

                ArrayList myRelevant = new ArrayList();

                for (int i = 0; i < myFields.length; i++) {
                    if (isValid(myFields[i]))
                        myRelevant.add(myFields[i]);
                }

                myFields = new Field[myRelevant.size()];
                myFields = (Field[]) myRelevant.toArray(myFields);
                theFieldCache.put(aClass, myFields);
            }

            return myFields;
        }
    }

    private MangledField[] getFields(Entry anEntry) {
        Field[] myFields = getFields(anEntry.getClass());

        MangledField[] myValidFields =
            new MangledField[myFields.length];

        int i = 0;
        try {
            for (i = 0; i < myFields.length; i++) {
                Object myFieldValue = myFields[i].get(anEntry);
                String myFieldName = myFields[i].getName();

                if (myFieldValue == null)
                    myValidFields[i] =
                        new MangledField(myFieldName);
                else
                    myValidFields[i] =
                        new MangledField(myFieldName, myFieldValue);
            }
        } catch (IllegalAccessException anIAE) {
            theLogger.log(Level.SEVERE, "Problem accessing field", anIAE);
            throw new InternalError("Couldn't access field: " + myFields[i]);
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "Fatal error", anIOE);
            throw new InternalError("Fatal error: " + anIOE);
        }

        return myValidFields;
    }

    private boolean isValid(Field aField) {
        if ((aField.getModifiers() & INVALID_MODIFIERS) != 0)
            return false;

        if (aField.getType().isPrimitive())
            return false;

        return true;
    }
}
