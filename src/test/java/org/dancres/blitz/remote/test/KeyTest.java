package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.admin.Administrable;

public class KeyTest {

    public void exec() throws Exception {
        Lookup myFinder = new Lookup(JavaSpace.class);

        JavaSpace mySpace = (JavaSpace) myFinder.getService();

        BufferedReader myReader =
            new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.println("Hit return");
            myReader.readLine();

            mySpace.write(new TestEntry("abcdef", new Integer(33)),
                          null, Lease.FOREVER);
            System.out.println(mySpace.take(new TestEntry(), null, 10000));
        }
    }

    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new KeyTest().exec();
        } catch (Exception anE) {
            System.err.println("Whoops");
            anE.printStackTrace(System.err);
        }
    }

    public static class TestEntry implements Entry {
        public String theName;
        public Integer theValue;

        public TestEntry() {
        }

        public TestEntry(String aName, Integer aValue) {
            theName = aName;
            theValue = aValue;
        }
    }
}
