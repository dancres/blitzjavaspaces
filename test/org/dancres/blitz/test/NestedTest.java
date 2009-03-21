package org.dancres.blitz.test;

import java.util.Random;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

public class NestedTest {
    public static void main(String args[]) {

        try {

            LocalSpace myLocalSpace = new LocalSpace(null);

            JavaSpace mySpace = myLocalSpace.getProxy();

            for (int i = 0; i < 4; i++) {
                mySpace.write(new NestedEntry("a", "b"), null, Lease.FOREVER);
            }

            /*
              Test non-wildcard match (there are two different branches in
              the EntryStorage find() code we need to test)
             */
            for (int i = 0; i < 10; i++) {
                System.out.println(mySpace.take(new BaseEntry("a"), null, 100));
            }

            /*
              Test wildcard match (there are two different branches in
              the EntryStorage find() code we need to test)
             */
            for (int i = 0; i < 10; i++) {
                System.out.println(mySpace.take(new BaseEntry(), null, 100));
            }

            myLocalSpace.stop();

        } catch (Exception anE) {
            System.err.println("Ooops");
            anE.printStackTrace(System.err);
        }
    }

    public static class BaseEntry implements Entry {
        public String theBaseName;

        public BaseEntry() {
        }

        public BaseEntry(String aBase) {
            theBaseName = aBase;
        }
    }

    public static class NestedEntry extends BaseEntry implements Entry {
        public NestedString theNested;

        public NestedEntry() {
        }

        public NestedEntry(String aBase, String aNested) {
            super(aBase);
            theNested = new NestedString(aNested);
        }

        public String toString() {
            return theNested.toString() + ":" + theBaseName;
        }

        private class NestedString implements java.io.Serializable {
            private String theString;

            NestedString(String aString) {
                theString = aString;
            }

            public String toString() {
                return "private:" + theString;
            }
        }
    }
}
