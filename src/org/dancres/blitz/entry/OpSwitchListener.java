package org.dancres.blitz.entry;

import org.dancres.blitz.stats.SwitchListener;
import org.dancres.blitz.stats.Switch;
import org.dancres.blitz.stats.OpSwitch;
import org.dancres.blitz.stats.OpStat;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.SwitchSettings;

/**
   Listens for changes in OpSwitches pertinent to a particular Entry type
   and manages counts and statistics accordingly.
 */
class OpSwitchListener implements SwitchListener {
    private boolean trackTakes;
    private boolean trackReads;
    private boolean trackWrites;

    private String theType;

    private OpStat theTakes;
    private OpStat theReads;
    private OpStat theWrites;

    OpSwitchListener(String aType) {
        theType = aType;
        theTakes = new OpStat(theType, OpStat.TAKES);
        theReads = new OpStat(theType, OpStat.READS);
        theWrites = new OpStat(theType, OpStat.WRITES);

        SwitchSettings.get().add(this);
    }

    public synchronized void switchFlipped(Switch aSwitch) {
        if (aSwitch instanceof OpSwitch) {
            OpSwitch mySwitch = (OpSwitch) aSwitch;

            if ((mySwitch.getType().equals(theType)) ||
                (mySwitch.isWildcard())) {

                boolean mySetting = mySwitch.isEnabled();

                switch (mySwitch.getOp()) {
                    case OpSwitch.TAKE_OPS : {
                        trackTakes = mySetting;

                        if (mySetting)
                            StatsBoard.get().add(theTakes);
                        else
                            StatsBoard.get().remove(theTakes);

                        break;
                    }
                    case OpSwitch.READ_OPS : {
                        trackReads = mySetting;

                        if (mySetting)
                            StatsBoard.get().add(theReads);
                        else
                            StatsBoard.get().remove(theReads);

                        break;
                    }
                    case OpSwitch.WRITE_OPS : {
                        trackWrites = mySetting;

                        if (mySetting)
                            StatsBoard.get().add(theWrites);
                        else
                            StatsBoard.get().remove(theWrites);

                        break;
                    }
                }
            }
        }
    }

    synchronized void didWrite() {
        if (trackWrites)
            theWrites.incCount();
    }

    synchronized void didRead() {
        if (trackReads)
            theReads.incCount();
    }

    synchronized void didTake() {
        if (trackTakes)
            theTakes.incCount();
    }

    void destroy() {
        SwitchSettings.get().remove(this);
        StatsBoard.get().remove(theReads);
        StatsBoard.get().remove(theTakes);
        StatsBoard.get().remove(theWrites);
    }
}
