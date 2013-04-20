package org.dancres.blitz.stats;

/**
   <p>Holds a list of all types known to the Blitz instance including those
   written explicitly as Entry's and those inferred from hierarchy information
   of written Entry's.</p>

   <p>Tracking of known types is always done - there is no Switch to turn this
   on and off (this information is tracked anyway so no cost is incurred).</p>
 */
public class TypesStat implements Stat, StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private String[] theTypes = new String[0];

    public TypesStat() {
    }

    private TypesStat(long anId) {
        theId = anId;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public synchronized String[] getTypes() {
        return theTypes;
    }

    public synchronized void setTypes(String[] aTypes) {
        theTypes = aTypes;
    }

    public synchronized Stat generate() {
        String[] myTypes = new String[theTypes.length];

        System.arraycopy(theTypes, 0, myTypes, 0, theTypes.length);

        TypesStat myStat = new TypesStat(theId);
        myStat.setTypes(myTypes);

        return myStat;
    }

    public String toString() {
        StringBuffer myTypes = new StringBuffer("Types: ");

        for (int i = 0; i < theTypes.length; i++) {
            myTypes.append(theTypes[i]);
            myTypes.append(", ");
        }

        return myTypes.toString();
    }
}
