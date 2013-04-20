package org.dancres.blitz.stats;

import java.net.InetAddress;
import java.net.NetworkInterface;

import java.util.ArrayList;
import java.util.Enumeration;

/**
   <p>Provides information about the host on which this Blitz instance is
   running.</p>
 */
public class HostStat implements Stat, StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private InetAddress[] theHostAddrs;
    private InetAddress theHostAddr;

    HostStat() {
        try {
            ArrayList myAllAddrs = new ArrayList();

            Enumeration myIfs = NetworkInterface.getNetworkInterfaces();

            while(myIfs.hasMoreElements()) {
                NetworkInterface myIf = (NetworkInterface) myIfs.nextElement();

                Enumeration myAddrs = myIf.getInetAddresses();
                while (myAddrs.hasMoreElements()) {
                    InetAddress myAddr = (InetAddress) myAddrs.nextElement();

                    myAllAddrs.add(myAddr);
                }
            }

            theHostAddrs = new InetAddress[myAllAddrs.size()];
            theHostAddrs = (InetAddress[]) myAllAddrs.toArray(theHostAddrs);

            theHostAddr = InetAddress.getLocalHost();
        } catch (Exception anE) {
            // Nothing to be done....
        }
    }

    HostStat(long anId, InetAddress anAddr, InetAddress[] anAddrs) {
        theId = anId;
        theHostAddr = anAddr;
        theHostAddrs = anAddrs;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public InetAddress getHostAddr() {
        return theHostAddr;
    }

    public InetAddress[] getAllAddr() {
        return theHostAddrs;
    }

    public synchronized Stat generate() {
        HostStat myStat = new HostStat(theId, 
                                       theHostAddr,
                                       theHostAddrs);
        return myStat;
    }

    public String toString() {
        return theHostAddr.getHostName() + "[" +
            theHostAddr.getHostAddress() + "]";
    }
}
