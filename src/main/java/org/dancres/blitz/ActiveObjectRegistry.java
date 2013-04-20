package org.dancres.blitz;

import java.util.ArrayList;

/**
 * ActiveObjectRegistry handles thread lifecycles across a number of objects within the Blitz JVM (e.g.
 * <code>LeaseReaper</code>). One is guaranteed that <code>Lifecycle.init</code> will be called before
 * <code>startAll</code> and that <code>stopAll</code> will be called before <code>Lifecycle.deinit</code>.
 *
 * Note: ActiveObjectRegistry discards all references when <code>stopAll</code> is called as objects registered with
 * <code>Lifecycle</code> are expected to restart and re-register threads on <code>init</code>.
 * 
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

        theObjects.clear();
        haveStarted = false;
    }

    public synchronized static boolean hasStarted() {
        return haveStarted;
    }
}
