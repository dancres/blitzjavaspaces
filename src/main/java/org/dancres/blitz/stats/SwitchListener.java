package org.dancres.blitz.stats;

/**
   When registered with SwitchSettings, will be informed of changes in switch
   state and can take appropriate action in respect of statistics advertised
   on the StatsBoard.
 */
public interface SwitchListener {
    public void switchFlipped(Switch aSwitch);
}
