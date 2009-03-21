package org.dancres.blitz;

import java.util.ArrayList;

/**
   @see org.dancres.blitz.ActiveObject
 */
public class ActiveObjectRegistry {
    private static ArrayList theObjects = new ArrayList();
    private static boolean haveStarted = false;

    public synchronized static void add(ActiveObject anObject) {
        theObjects.add(anObject);

        // If already started we have to "automagically" start this object.
        if (haveStarted)
            anObject.begin();
    }

    public synchronized static void startAll() {
        haveStarted = true;

        for (int i = 0; i < theObjects.size(); i++) {
            ((ActiveObject) theObjects.get(i)).begin();
        }
    }

    public synchronized static void stopAll() {
        for (int i = 0; i < theObjects.size(); i++) {
            ActiveObject myObject = (ActiveObject) theObjects.get(i);
            myObject.halt();
        }
    }

    public synchronized static boolean hasStarted() {
        return haveStarted;
    }
}
