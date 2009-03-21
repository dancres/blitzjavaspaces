package org.dancres.blitz.test;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.EntryView;
import org.dancres.blitz.EntryChit;
import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;
import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;
import net.jini.entry.AbstractEntry;

import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class InheritingContents {
        public static void main(String args[]) {

            try {
                int myTotal = 10;

                System.out.println("Start space");

                SpaceImpl mySpace = new SpaceImpl(null);

                System.out.println("Prepare entry");

                EntryMangler myMangler = new EntryMangler();

                System.out.println("init'd entry");

                System.out.println("Do write: " + myTotal);

                long myStart = System.currentTimeMillis();

                for (int i = 0;i < myTotal; i++) {
                    MyInheriting myOne =
                            new MyInheriting(Integer.toString(i));

                    MyOther myTwo =
                            new MyOther(Integer.toString(i));

                    YetAnother myThree =
                            new YetAnother(Integer.toString(i),
                                    Integer.toString(i));

                    MangledEntry myPackedEntry = myMangler.mangle(myOne);

                    mySpace.write(myPackedEntry, null, Lease.FOREVER);

                    myPackedEntry = myMangler.mangle(myTwo);

                    mySpace.write(myPackedEntry, null, Lease.FOREVER);

                    myPackedEntry = myMangler.mangle(myThree);

                    mySpace.write(myPackedEntry, null, Lease.FOREVER);
                }

                long myEnd = System.currentTimeMillis();

                System.out.println("Writes completed: " + (myEnd - myStart));


                System.out.println("My inheriting test");

                Entry myInheritTemplate = new MyInheriting(null);
                MangledEntry myInheritMangled =
                        myMangler.mangle(myInheritTemplate);

                EntryView myView =
                    mySpace.getView(new MangledEntry[] {myInheritMangled},
                                    null, true, Long.MAX_VALUE);

                EntryChit myChit;

                while ((myChit = myView.next()) != null) {
                    System.out.println(myChit.getCookie());

                    Entry myResult = myMangler.unMangle(myChit.getEntry());
                    System.out.println(myResult);
                }

                myView.close();
                
                System.out.println("Do stop");

                mySpace.stop();

            } catch (Exception anE) {
                System.err.println("Got exception :(");
                anE.printStackTrace(System.err);
            }
        }

        public static class MyOther extends AbstractEntry {
            public String yetAnother;

            public MyOther() {
            }

            public MyOther(String b) {
                yetAnother = b;
            }
        }

        public static class MyInheriting extends AbstractEntry {
            public String yetAnother;

            public MyInheriting() {
            }

            public MyInheriting(String b) {
                yetAnother = b;
            }
        }

        public static class YetAnother extends MyInheriting {
            public String yetAgain;

            public YetAnother() {
            }

            public YetAnother(String a, String b) {
                super(a);
                yetAgain = b;
            }
        }
}
