package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;

public class Boot {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.in.read();

            System.out.println("Do stop");

            mySpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }
}
