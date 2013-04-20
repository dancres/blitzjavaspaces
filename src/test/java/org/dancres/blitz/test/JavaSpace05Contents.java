package org.dancres.blitz.test;

import java.util.Collection;
import java.util.Collections;

import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;
import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;

import org.dancres.blitz.remote.LocalSpace;

/**
 * Make sure that a null transaction doesn't hold locks
 */
public class JavaSpace05Contents {
    public static void main(String args[]) throws Exception {
        LocalSpace myLocalSpace = new LocalSpace(null);

        JavaSpace05 mySpace = (JavaSpace05) myLocalSpace.getJavaSpaceProxy();

        for (int i = 0; i < 10; i++) {
            mySpace.write(new Contents05Entry("1"), null, Lease.FOREVER);
        }

        Collection myTemplates =
            Collections.singletonList(new Contents05Entry());

        MatchSet mySet = mySpace.contents(myTemplates, null, Lease.FOREVER,
                                            Long.MAX_VALUE);

        Entry myResult;

        while ((myResult = mySet.next()) != null) {
            System.out.println(myResult);
        }

        while ((myResult = mySpace.take(new Contents05Entry(), null, 0))
            != null) {
            System.out.println("Took: " + myResult);
        }

        System.out.println("Cancel lease");

        mySet.getLease().cancel();

        myLocalSpace.stop();
    }

    public static final class Contents05Entry implements Entry {
        public String _field;

        public Contents05Entry() {
        }

        Contents05Entry(String aValue) {
            _field = aValue;
        }

        public String toString() {
            return _field;
        }
    }
}
