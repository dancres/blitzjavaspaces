package org.dancres.blitz.stats;

import java.util.Collection;

/**
   <p>Tracks the known fields of a particular type known to the blitz core.
   One can consider this a summary of the schema of the Entry.</p>
 */
public class FieldsStat implements Stat, StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private String theType;
    private String[] theFields;

    public FieldsStat(String aType, Collection aFieldNames) {
        theType = aType;
        String[] myFields = new String[aFieldNames.size()];
        theFields = (String[]) aFieldNames.toArray(myFields);
    }

    private FieldsStat(long anId) {
        theId = anId;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public String[] getFields() {
        return theFields;
    }

    public String getType() {
        return theType;
    }

    private void setType(String aType) {
        theType = aType;
    }

    private void setFields(String[] aFields) {
        theFields = aFields;
    }

    public Stat generate() {
        String[] myFields = new String[theFields.length];

        System.arraycopy(theFields, 0, myFields, 0, theFields.length);

        FieldsStat myStat = new FieldsStat(theId);
        myStat.setFields(myFields);
        myStat.setType(theType);

        return myStat;
    }

    public String toString() {
        StringBuffer myFields = new StringBuffer("Fields for " + theType +
                                                 ": ");

        for (int i = 0; i < theFields.length; i++) {
            myFields.append(theFields[i]);
            myFields.append(", ");
        }

        return myFields.toString();
    }
}
