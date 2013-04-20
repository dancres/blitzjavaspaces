package org.dancres.blitz.entry;

import java.io.Serializable;

import java.util.ArrayList;

/**
   If logging of instance counts is enabled, at each checkpoint, we will lodge
   an instance of this class as a checkpoint contribution
 */
class InstanceCheckpoint implements Serializable {
    private ArrayList theCounts = new ArrayList();
    private long theTime = System.currentTimeMillis();

    void add(String aType, int aCount) {
        theCounts.add(new CountAction(aType, aCount));
    }

    public String toString() {
        StringBuffer myBuffer = new StringBuffer();

        for (int i = 0; i < theCounts.size(); i++) {
            CountAction myCount = (CountAction) theCounts.get(i);

            myBuffer.append(theTime + " :" + myCount.toString() + "\n");
        }

        return myBuffer.toString();
    }
}