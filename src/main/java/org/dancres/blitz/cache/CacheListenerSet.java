package org.dancres.blitz.cache;

import java.util.ArrayList;

/**
   Utility class which handles aspects of CacheListener management including
   signalling of an event
 */
public class CacheListenerSet {
    public static final int LOADED = 1;
    public static final int FLUSHED = 2;
    public static final int DIRTIED = 3;

    private ArrayList theListeners = new ArrayList();

    public CacheListenerSet() {
    }

    public void add(CacheListener aListener) {
        synchronized(theListeners) {
            if (! theListeners.contains(aListener)) {
                theListeners.add(aListener);
            }
        }
    }

    public void signal(int aSignal, Identifiable aTarget) {
        CacheListener[] myListeners;

        synchronized(theListeners) {
            myListeners = new CacheListener[theListeners.size()];
            myListeners = (CacheListener[]) theListeners.toArray(myListeners);
        }

        for (int i = 0; i < myListeners.length; i++) {
            CacheListener myListener = myListeners[i];

            switch (aSignal) {
                case LOADED: {
                    myListener.loaded(aTarget);
                    break;
                }
                case FLUSHED: {
                    myListener.flushed(aTarget);
                    break;
                }
                case DIRTIED: {
                    myListener.dirtied(aTarget);
                    break;
                }
            }
        }
    }
}
