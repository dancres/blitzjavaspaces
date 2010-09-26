package org.dancres.blitz;

import java.util.ArrayList;

/**
 * Registrants with this class are never forgotten. Typically they register at class loading time to initialise state
 * and thus never re-register or "die". Almost all of Blitz's activity is bounded by <code>startAll</code> and
 * </code>stopAll</code>. The other crucial part of Blitz's lifecycle is found in <code>ActiveObjectRegistry</code>
 * which is responsible for handling all thread-related activity.
 */
public class LifecycleRegistry {
    private static ArrayList theObjects = new ArrayList();
    private static boolean haveStarted = false;

    public synchronized static void add(Lifecycle anObject) {
        theObjects.add(anObject);

        // If already started we have to "automagically" start this object.
        if (haveStarted)
            anObject.init();
    }

    public synchronized static void init() {
        haveStarted = true;

        for (int i = 0; i < theObjects.size(); i++) {
            ((Lifecycle) theObjects.get(i)).init();
        }
    }

    public synchronized static void deinit() {
        for (int i = 0; i < theObjects.size(); i++) {
            Lifecycle myObject = (Lifecycle) theObjects.get(i);
            myObject.deinit();
        }

        haveStarted = false;
    }

    public synchronized static boolean hasStarted() {
        return haveStarted;
    }
}
