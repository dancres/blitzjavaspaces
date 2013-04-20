package org.dancres.blitz.entry;

import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.SwitchListener;
import org.dancres.blitz.stats.Switch;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.InstanceSwitch;
import org.dancres.blitz.stats.InstanceCount;
import org.dancres.blitz.stats.SwitchSettings;

class InstanceSwitchListener implements SwitchListener {
    private String theInstanceType;
    private InstanceCount theCount;

    InstanceSwitchListener(String aType, int anInitialCount) {
        theInstanceType = aType;
        theCount = new InstanceCount(aType, anInitialCount);

        SwitchSettings.get().add(this);
    }

    public synchronized void switchFlipped(Switch aSwitch) {
        if (aSwitch instanceof InstanceSwitch) {
            InstanceSwitch mySwitch = (InstanceSwitch) aSwitch;

            if ((mySwitch.getType().equals(theInstanceType)) ||
                (mySwitch.isWildcard())) {

                if (mySwitch.isOn()) {
                    StatsBoard.get().add(theCount);
                } else
                    StatsBoard.get().remove(theCount);
            }
        }
    }

    synchronized void wrote() {
        theCount.wrote();
    }

    synchronized void took() {
        theCount.took();
    }

    Stat getInstanceStat() {
        return theCount.generate();
    }

    synchronized int getTotal() {
        return theCount.getCount();
    }

    void destroy() {
        SwitchSettings.get().remove(this);
        StatsBoard.get().remove(theCount);
    }
}
