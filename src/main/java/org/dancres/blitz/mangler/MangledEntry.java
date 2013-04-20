package org.dancres.blitz.mangler;

import java.io.*;

import com.sun.jini.proxy.MarshalledWrapper;
import java.util.Collection;
import java.util.Iterator;
import net.jini.entry.AbstractEntry;
import net.jini.io.ObjectStreamContext;
import net.jini.io.context.IntegrityEnforcement;

/**
   <p>Represents a packaged up Entry ready for unpacking or passing to the
   server for matching.  We include null fields as well so as to ensure
   we have a complete, ordered, list of fields which allows as to build
   a composite hash to speed searching under some circumstances.</p>

   <p>Integrity checking starts here.  If MangledEntry detects that integrity
   checking was active when it was unpacked from the stream (see
   <code>readObject()</code> in this class) it makes a note of the fact.
   Later, this is interrogated by EntryMangler (see
   <code>needsIntegrityCheck</code>) and handled accordingly.</p>

   @todo Fix up disk usage on null template match - see NULL_TEMPLATE
 */
public class MangledEntry implements Externalizable, net.jini.core.entry.Entry {

    static final long serialVersionUID = 462890915488647301L;

    private String[] theParents;
    private MangledField[] theFields;
    private String theType;
    private String theCodebase;
    private boolean isWildcard;
    private boolean isSnapshot;

    /**
     * Set to <code>true</code> in <code>readExternal</code> method
     * (called when we're deserialized).  When we unpack the contents using
     * EntryMangler, we check this flag and perform integrity checks if
     * required.
     */
    private transient boolean checkIntegrity = false;

    /**
     * As all entry instances are ultimately rooted at java.lang.Object we
     * can simply search from java.lang.Object downwards when we receive
     * a null-template query.  This will cause a little extra disk I/O but
     * nevermind for now.
     */
    public static final MangledEntry NULL_TEMPLATE =
        new MangledEntry("java.lang.Object", null, new MangledField[0],
            new String[0], true);

    static boolean integrityEnforced(ObjectInput aStream) {
        if (aStream instanceof ObjectStreamContext) {
            Collection ctx =
                    ((ObjectStreamContext) aStream).getObjectStreamContext();
            for (Iterator i = ctx.iterator(); i.hasNext();) {
                Object obj = i.next();
                if (obj instanceof IntegrityEnforcement) {
                    return ((IntegrityEnforcement) obj).integrityEnforced();
                }
            }
        }

        return false;
    }

    public MangledEntry() {

    }

    MangledEntry(String aType, String aCodebase,
                 MangledField[] aListOfFields, String[] aListOfParents,
                 boolean doWildcard) {
        this(aType, aCodebase, aListOfFields, aListOfParents,
            doWildcard, false);
    }

    MangledEntry(String aType, String aCodebase,
                 MangledField[] aListOfFields, String[] aListOfParents,
                 boolean doWildcard, boolean doSnapshot) {
        theType = aType;
        theCodebase = aCodebase;
        theFields = aListOfFields;
        theParents = aListOfParents;
        isWildcard = doWildcard;
        isSnapshot = doSnapshot;
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject(theParents);
        objectOutput.writeInt(theFields.length);
        for (int i = 0; i < theFields.length; i++) {
            objectOutput.writeInt(theFields[i].getContent().length);
            objectOutput.writeInt(theFields[i].getAnnotations().length);
            objectOutput.write(theFields[i].getContent());
            objectOutput.write(theFields[i].getAnnotations());
            objectOutput.writeUTF(theFields[i].getName());
            objectOutput.writeInt(theFields[i].hashCode());
        }

        objectOutput.writeUTF(theType);
        objectOutput.writeUTF((theCodebase == null) ? "" : theCodebase);
        objectOutput.writeBoolean(isWildcard);
        objectOutput.writeBoolean(isSnapshot);
    }

    public void readExternal(ObjectInput objectInput) throws IOException,
        ClassNotFoundException {

        checkIntegrity = integrityEnforced(objectInput);
        theParents = (String[]) objectInput.readObject();
        theFields = new MangledField[objectInput.readInt()];

        for (int i = 0; i < theFields.length; i++) {
            int myObjectSize = objectInput.readInt();
            int myAnnotSize = objectInput.readInt();

            byte[] myContent = new byte[myObjectSize];
            byte[] myAnnot = new byte[myAnnotSize];

            objectInput.readFully(myContent);
            objectInput.readFully(myAnnot);

            theFields[i] = new MangledField(objectInput.readUTF(),
                myContent, myAnnot, objectInput.readInt());
        }

        theType = objectInput.readUTF();
        String myCodebase = objectInput.readUTF();
        theCodebase = (myCodebase.length() == 0) ? null : myCodebase;
        isWildcard = objectInput.readBoolean();
        isSnapshot = objectInput.readBoolean();
    }

    public int sizeOf() {
        int myFieldTotal = 0;

        for (int i = 0; i < theFields.length; i++) {
            myFieldTotal += theFields[i].sizeOf();
        }

        int myParentsTotal = 0;

        for (int i = 0; i < theParents.length; i++) {
            myParentsTotal += theParents[i].length();
        }

        int myCodebaseLength = 0;

        if (theCodebase != null)
            myCodebaseLength = theCodebase.length();

        return theType.length() + myCodebaseLength + 4 + myParentsTotal +
            myFieldTotal;
    }

    /**
     * In the case of a template, this indicates that none of it's fields
     * have specific values so any match will do.
     */
    public boolean isWildcard() {
        return isWildcard;
    }

    public String[] tearOffParents() {
        return theParents;
    }

    public MangledField[] getFields() {
        return theFields;
    }

    public MangledField getField(int anOffset) {
        return theFields[anOffset];
    }

    public String getType() {
        return theType;
    }

    public String getCodebase() {
        return theCodebase;
    }

    public int getNumFields() {
        return theFields.length;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    /**
     * @param anEntry the Entry to test against.  The instance on which this
     *                method is called is implicitly treated as the template.
     */
    public boolean match(MangledEntry anEntry) {
        if (isWildcard)
            return true;

        MangledField[] myEntryFields = anEntry.getFields();

        for (int i = 0; i < theFields.length; i++) {
            if (!theFields[i].isNull()) {
                if (!theFields[i].matches(myEntryFields[i]))
                    return false;
            }
        }

        return true;
    }

    public int hashCode() {
        int myHash = 0;

        for (int i = 0; i < theFields.length; i++) {
            myHash ^= theFields[i].hashCode();
        }

        return myHash;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof MangledEntry) {
            MangledEntry myOther = (MangledEntry) anObject;

            if (myOther.theType.equals(theType)) {
                return match(myOther);
            }
        }

        return false;
    }

    public void dump(PrintStream aStream) {
        aStream.println("Type: " + theType);
        aStream.println("Codebase: " + theCodebase);
        aStream.print("Parents: ");

        if (theParents != null) {
            for (int i = 0; i < theParents.length; i++) {
                aStream.print(theParents[i] + ", ");
            }
            aStream.println();
        }

        aStream.println("Fields:");
        for (int i = 0; i < theFields.length; i++) {
            aStream.println("  " + theFields[i].getName() + ": " +
                theFields[i].hashCode());
        }
    }

    boolean needsIntegrityCheck() {
        return checkIntegrity;
    }

    public net.jini.core.entry.Entry get()
        throws net.jini.core.entry.UnusableEntryException {
        return EntryMangler.getMangler().unMangle(this);
    }

    public net.jini.core.entry.Entry getSnapshot() {
        return this;
    }


    public String toString() {
        String myEntryRep = "Unusable";

        try {
            myEntryRep =
                AbstractEntry.toString(
                    EntryMangler.getMangler().unMangle(this));
        } catch (Exception anE) {
            // Nothing to do
            myEntryRep = myEntryRep + ": " + anE.getClass();
        }

        return myEntryRep;
    }
}