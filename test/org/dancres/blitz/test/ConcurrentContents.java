package org.dancres.blitz.test;

import java.util.TreeSet;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.entry.AbstractEntry;

import org.dancres.blitz.EntryChit;
import org.dancres.blitz.EntryView;
import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;

/**
 */
public class ConcurrentContents {
    public static void main(String args[]) {
        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("init'd entry");

            long myStart = System.currentTimeMillis();

            for (int i = 0; i < 3; i++) {
                MyInheriting myOne =
                        new MyInheriting(Integer.toString(i));

                MangledEntry myPackedEntry = myMangler.mangle(myOne);

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            long myEnd = System.currentTimeMillis();

            System.out.println("Writes completed: " + (myEnd - myStart));


            System.out.println("My inheriting test");

            Entry myInheritTemplate = new MyInheriting(null);
            MangledEntry myInheritMangled =
                    myMangler.mangle(myInheritTemplate);

            EntryView myView =
                    mySpace.getView(new MangledEntry[]{myInheritMangled},
                            null, true, Long.MAX_VALUE);

            EntryChit myChit;

            while ((myChit = myView.next()) != null) {
                System.out.println(myChit.getCookie());

                Entry myResult = myMangler.unMangle(myChit.getEntry());
                System.out.println(myResult);
            }

            myView.close();

            Thread.sleep(10000);

            new Contentser(mySpace).start();

            for (int j = 0; j < 100000; j++) {
                System.out.println("Write a bunch: " + j);

                for (int k = 0; k < 4; k++) {
                    MyOther myTwo =
                            new MyOther(Integer.toString(k));

                    MangledEntry myPackedEntry = myMangler.mangle(myTwo);

                    mySpace.take(myPackedEntry, null, 0);
                }

                for (int k = 0; k < 4; k++) {
                    MyOther myTwo =
                            new MyOther(Integer.toString(k));

                    MangledEntry myPackedEntry = myMangler.mangle(myTwo);

                    mySpace.write(myPackedEntry, null, Lease.FOREVER);
                }
            }

            for (int j = 0; j < 100000; j++) {
                System.out.println("Write a same bunch: " + j);

                for (int i = 0; i < 4; i++) {
                    MyInheriting myOne =
                            new MyInheriting(Integer.toString(i));

                    MangledEntry myPackedEntry = myMangler.mangle(myOne);

                    mySpace.take(myPackedEntry, null, 0);
                }

                for (int i = 0; i < 4; i++) {
                    MyInheriting myOne =
                            new MyInheriting(Integer.toString(i));

                    MangledEntry myPackedEntry = myMangler.mangle(myOne);

                    mySpace.write(myPackedEntry, null, Lease.FOREVER);
                }
            }

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
    }

    static class Contentser extends Thread {
        private SpaceImpl theSpace;
        private EntryMangler theMangler = new EntryMangler();

        Contentser(SpaceImpl aSpace) {
            theSpace = aSpace;
        }

        public void run() {
            try {
                while (true) {

                    TreeSet mySet = new TreeSet();

                    Entry myInheritTemplate = new MyInheriting(null);
                    MangledEntry myInheritMangled =
                            theMangler.mangle(myInheritTemplate);

                    EntryView myView =
                            theSpace.getView(new MangledEntry[]{myInheritMangled},
                                    null, true, 10);

                    EntryChit myChit;
                    Entry myResult;

                    while ((myChit = myView.next()) != null) {
                        Object myCookie = myChit.getCookie();
                        myResult = theMangler.unMangle(myChit.getEntry());

                        if (mySet.contains(myCookie)) {
                            System.out.println("Duplicate: " + myCookie);
                            System.exit(0);
                        } else if (! (myResult instanceof MyInheriting)) {
                            System.out.println("Wrong type: " + myResult.getClass());
                            System.exit(0);
                        } else
                            mySet.add(myCookie);


                        Thread.sleep(1000);

                        /*
                        if (mySet.size() > 3) {
                            System.out.println("Too many Entry's");
                            System.exit(0);
                        }
                        */
                    }

                    myView.close();
                }
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
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
}
