package org.dancres.blitz.tools.dash;

import java.rmi.RMISecurityManager;

import javax.swing.JOptionPane;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import javax.security.auth.login.LoginContext;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;

import net.jini.lookup.ServiceDiscoveryManager;

import net.jini.lookup.entry.Name;

import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.lookup.ServiceItem;

import net.jini.core.entry.Entry;

import net.jini.discovery.DiscoveryManagement;

import net.jini.admin.Administrable;

import net.jini.space.JavaSpace;

import net.jini.security.ProxyPreparer;
import net.jini.security.BasicProxyPreparer;

import org.dancres.blitz.remote.StatsAdmin;

/**
   Use this class to start dashboard from a JINI config file.
   If you wish to use dashboard in a secure configuration, you'll need this!

   See page 43 of Murph's slides on client boilerplate

   Add preparer for lookup service
 */
public class StartFromConfig {
    private static final String MODULE = "org.dancres.blitz.dash";
    private static final String LOGIN = "loginContext";
    private static final String SPACENAME = "spaceName";
    private static final String DM = "discoveryManagement";
    private static final String SPACE_PREP = "javaspacePreparer";
    private static final String ADMIN_PREP = "adminPreparer";
    private static final String TIMEOUT = "lookupTimeout";

    public static void main(String args[]) {
        try {
            System.setSecurityManager(new RMISecurityManager());

            final Configuration myConfig = 
                ConfigurationProvider.getInstance(args);

            final LoginContext myContext =
                (LoginContext)
                myConfig.getEntry(MODULE, LOGIN, LoginContext.class);

            if (myContext == null) {
                StartFromConfig myStart = new StartFromConfig(myConfig);
                myStart.startup();
            } else {
                myContext.login();

                Subject.doAsPrivileged(myContext.getSubject(),
                                       new PrivilegedExceptionAction() {
                                           public Object run() throws Exception {
                                               StartFromConfig myStart =
                                                   new StartFromConfig(myConfig);
                                               myStart.startup();
                                               
                                               return null;
                                           }
                                       },
                                       null);
            }
            
        } catch (Exception anE) {
            System.err.println("Encountered error during startup, quitting");
            anE.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    private Configuration theConfig;
    private ServiceDiscoveryManager theSDM;
    private DiscoveryManagement theDM;

    private StartFromConfig(Configuration aConfig) {
        theConfig = aConfig;
    }

    private void startup() throws Exception {
        try {
            startupImpl();
        } finally {
            if (theSDM != null)
                try {
                    theSDM.terminate();
                } catch (Throwable aT) {
                }

            if (theDM != null)
                try {
                    theDM.terminate();
                } catch (Throwable aT) {
                }
        }
    }

    private void startupImpl() throws Exception {
        ProxyPreparer mySpacePrep = (ProxyPreparer)
            theConfig.getEntry(MODULE, SPACE_PREP, ProxyPreparer.class,
                               new BasicProxyPreparer());

        ProxyPreparer myAdminPrep = (ProxyPreparer)
            theConfig.getEntry(MODULE, ADMIN_PREP, ProxyPreparer.class,
                               new BasicProxyPreparer());

        theDM = (DiscoveryManagement)
            theConfig.getEntry(MODULE, DM, DiscoveryManagement.class);

        theSDM = new ServiceDiscoveryManager(theDM, null, theConfig);

        Name myServiceName = new Name((String)
                                      theConfig.getEntry(MODULE,
                                                         SPACENAME,
                                                         String.class));

        ServiceTemplate myTemplate =
            new ServiceTemplate(null, new Class[] {JavaSpace.class},
                                new Entry[] {myServiceName});

        long myTimeout =
            ((Long) theConfig.getEntry(MODULE, TIMEOUT, Long.class,
                                       new Long(30 * 1000))).longValue();

        ServiceItem myResult = theSDM.lookup(myTemplate, null, myTimeout);

        if (myResult == null) {
                JOptionPane.showMessageDialog(null, "Didn't find the specified JavaSpace :(", "Blitz Dash", JOptionPane.ERROR_MESSAGE);
                return;
        }

        JavaSpace mySpace = (JavaSpace) mySpacePrep.prepareProxy(myResult.service);
        if (mySpace instanceof Administrable) {
            Administrable myAdministrable = (Administrable) mySpace;

            Object myAdmin =
                myAdminPrep.prepareProxy(myAdministrable.getAdmin());

            if (myAdmin instanceof StatsAdmin) {
                StatsAdmin myStats = (StatsAdmin) myAdmin;

                DashBoardFrame myFrame =
                    new DashBoardFrame(StartDashBoard.VER, myStats, true);

                myFrame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Space didn't have StatsAdmin :(", "Blitz Dash", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
                JOptionPane.showMessageDialog(null, "Space wasn't Administrable :(", "Blitz Dash", JOptionPane.ERROR_MESSAGE);
                return;
        }
    }
}
