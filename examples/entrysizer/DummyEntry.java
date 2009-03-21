package entrysizer;

import net.jini.core.entry.*;

public class DummyEntry implements Entry {
    public String theName;
    public String anotherField;

    public DummyEntry() {
    }

    public DummyEntry(String aName) {
        theName = aName;
    }

    public String toString() {
        return theName;
    }

    public boolean equals(Object anObject) {
        if ((anObject != null) && (anObject instanceof DummyEntry)) {

            DummyEntry myEntry = (DummyEntry) anObject;

            if (myEntry.theName == null)
                return (myEntry.theName == theName);
            else
                return ((DummyEntry) anObject).theName.equals(theName);
        }

        return false;
    }
}
