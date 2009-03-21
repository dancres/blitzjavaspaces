package org.dancres.blitz.stats;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Level;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.config.ConfigurationFactory;

/**
   This class tracks currently set profiling switches and keeps listener's
   up-to-date on changes in switch state.

   @todo Load default switch settings from configuration
 */
public class SwitchSettings {
    private static final String STATS_SWITCHES = "stats";

    private static SwitchSettings theSettings = new SwitchSettings();

    private ArrayList theSwitches = new ArrayList();
    private LinkedList theListeners = new LinkedList();

    SwitchSettings() {
        try {
            Switch[] mySwitches =
                (Switch[]) ConfigurationFactory.getEntry(STATS_SWITCHES,
                                                         Switch[].class,
                                                         new Switch[0]);

            if (mySwitches.length != 0) {
                for (int i = 0; i < mySwitches.length; i++) {
                    theSwitches.add(mySwitches[i]);
                }
            }
        } catch (ConfigurationException aCE) {
            StatsBoard.theLogger.log(Level.SEVERE, "Failed to load switch settings for statistics tracking", aCE);
        }
    }

    public static SwitchSettings get() {
        return theSettings;
    }

    public void add(SwitchListener aListener) {
        boolean wasAdded = false;

        synchronized(theListeners) {
            if (! theListeners.contains(aListener)) {
                theListeners.add(aListener);
                wasAdded = true;
            }
        }
        
        // If we added a listener - make it aware of current switch settings
        if (wasAdded)
            refresh(aListener);
    }

    public void remove(SwitchListener aListener) {
        synchronized(theListeners) {
            if (theListeners.contains(aListener))
                theListeners.remove(aListener);
        }
    }

    /**
       Update the settings of a particular switch
     */
    public void update(Switch aSwitch) {
        synchronized (theSwitches) {
            int myPos = theSwitches.indexOf(aSwitch);
            if (myPos != -1) {
                theSwitches.remove(myPos);
            }

            theSwitches.add(aSwitch);
        }

        synchronized(theListeners) {
            Iterator myListeners = theListeners.iterator();

            while (myListeners.hasNext()) {
                SwitchListener myListener =
                    (SwitchListener) myListeners.next();

                myListener.switchFlipped(aSwitch);
            }
        }
    }

    /**
       Update settings based on the passed array of switches
     */
    public void update(Switch[] aListOfSwitches) {
        for (int i = 0; i < aListOfSwitches.length; i++) {
            update(aListOfSwitches[i]);
        }
    }

    /**
       Notify a switch listener of all currently set switches
     */
    private synchronized void refresh(SwitchListener aListener) {
        synchronized(theSwitches) {
            Iterator mySwitches = theSwitches.iterator();

            while (mySwitches.hasNext()) {
                Switch mySwitch = (Switch) mySwitches.next();

                aListener.switchFlipped(mySwitch);
            }
        }
    }
}
