/*
 * Blitz Stats GUI contributed by Inca X ( www.incax.com )
 *
 * This code was contibuted by Inca X as a starting point
 * for creating GUI clients for the Blitz stats interface
 *
 */
package org.dancres.blitz.tools.dash;

import javax.swing.JOptionPane;

import net.jini.admin.Administrable;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.StatsAdmin;

import org.dancres.jini.util.ServiceLocator;
import org.dancres.jini.util.DiscoveryUtil;

public class StartDashBoard{

    private static final long MAX_DISCOVER_TIME = 15 * 1000;
    //changed to public as this is accessed by the ServiceUI
    public static final String VER="Blitz dashboard: v1.3";

    public static void main(String [] args){
        try{
            long myDiscoverTime = MAX_DISCOVER_TIME;

            String myOverrideTime = System.getProperty("maxDiscover");

            if (myOverrideTime != null)
                myDiscoverTime = Long.parseLong(myOverrideTime);

            JavaSpace mySpace = null;

            if(args.length==0){
                System.err.println("Usage: requires <spacename> or <lushost> <spacename>");
                System.exit(0);
            } else if (args.length == 1) {
                System.out.println("Multicast discovery");

                mySpace = (JavaSpace)
                    ServiceLocator.getService(JavaSpace.class, args[0],
                        myDiscoverTime);
            } else if (args.length == 2) {
                System.out.println("Unicast discovery");
                mySpace = (JavaSpace)
                    ServiceLocator.getService(args[0],
                        JavaSpace.class, args[1]);
            }

            if (mySpace != null) {
                Administrable admin=(Administrable)mySpace;

                System.out.println("StatsAdmin from: " + StatsAdmin.class.getClassLoader());

                DiscoveryUtil.dumpInterfaces(admin.getAdmin());

                StatsAdmin myStatsAdmin = (StatsAdmin)admin.getAdmin();

                DashBoardFrame frame=new DashBoardFrame(VER,myStatsAdmin,true/*exitOnClose*/);

                frame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Didn't find the specified JavaSpace :(", "Blitz Dash", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        }catch(Exception ex){
            ex.printStackTrace(System.err);
        }
    }
}
