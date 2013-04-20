package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;
import net.jini.entry.AbstractEntry;

import org.dancres.blitz.remote.LocalSpace;

/**
 * Make sure we can search on individual fields within multiple fields.
 * This ensures we are correctly indexing/matching on the appropriate field
 * offsets.
 */
public class MultiFieldMatch {
    public static void main(String args[]) throws Exception {
        LocalSpace myLocalSpace = new LocalSpace();
        JavaSpace mySpace = myLocalSpace.getJavaSpaceProxy();

        Entry a = new MultiFieldEntry("a", null, null);
        Entry b = new MultiFieldEntry(null, "b", null);
        Entry c = new MultiFieldEntry(null, null, "c");

        mySpace.write(a, null, Lease.FOREVER);
        mySpace.write(b, null, Lease.FOREVER);
        mySpace.write(c, null, Lease.FOREVER);

        Entry myResult;

        myResult = mySpace.read(a, null, 0);

        if (!a.equals(myResult)) {
            System.err.println("Failed match on a:" + a);
        } else {
            System.out.println("Got: " + myResult);
        }

        myResult = mySpace.read(b, null, 0);

        if (!b.equals(myResult)) {
            System.err.println("Failed match on b:" + b);
        } else {
            System.out.println("Got: " + myResult);
        }

        myResult = mySpace.read(c, null, 0);

        if (!c.equals(myResult)) {
            System.err.println("Failed match on c:" + c);
        } else {
            System.out.println("Got: " + myResult);
        }

        myLocalSpace.stop();
    }

    public static class MultiFieldEntry extends AbstractEntry {
        public String _first;
        public String _second;
        public String _third;

        public MultiFieldEntry() {
        }

        public MultiFieldEntry(String aFirst, String aSecond, String aThird) {
            _first = aFirst;
            _second = aSecond;
            _third = aThird;
        }
    }
}
