package org.dancres.blitz.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.Switch;

/**
   Proxy interface to provide access to and configuration of Blitz statistics.
*/
public interface StatsAdmin extends Remote {
    /**
       @return an array of Stat instances.  The instances will be some mix of
       the Stat implementations in <code>org.dancres.blitz.stats</code>.

       @see org.dancres.blitz.stats.TypesStat
       @see org.dancres.blitz.stats.OpStat
     */
    public Stat[] getStats() throws RemoteException;

    /**
       Remotely configure the generation of stats.  Typically switching some
       on and some off (hence Switch).  Switch instances will be any of those
       in <code>org.dancres.blitz.stats</code>
   
       @see org.dancres.blitz.stats.OpSwitch
     */
    public void setSwitches(Switch[] aListOfSwitches) throws RemoteException;
}
