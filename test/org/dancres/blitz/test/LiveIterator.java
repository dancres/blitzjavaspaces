package org.dancres.blitz.test;

import java.util.Random;
import java.util.Iterator;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 */
public class LiveIterator {
    private ConcurrentHashMap _collection
        = new ConcurrentHashMap();

    public static void main(String anArgs[]) {
        new LiveIterator().test();
    }

    private void test() {
        Random myRandom = new Random();

        for (int i = 0; i < 4096; i++) {
            Integer myValue = new Integer(myRandom.nextInt());

            _collection.put(myValue, myValue);
        }

        new Reader().start();

        for (int i = 0; i < 4; i++)
            new Permutter().start();
    }

    class Reader extends Thread {
        public void run() {
            while (true) {
                Iterator myValues = _collection.values().iterator();

                int myTotal = 0;

                while (myValues.hasNext()) {
                    myValues.next();
                    myTotal++;
                }
                System.out.println(
                    "Read all values: " + myTotal + ", " +
                        System.currentTimeMillis());

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    class Permutter extends Thread {
        private Random _random = new Random();

        public void run() {
            while (true) {
                try {
                    Integer myNew = new Integer(_random.nextInt());

                    Iterator myIterator = _collection.keySet().iterator();
                    if (myIterator.hasNext()) {
                        myIterator.next();
                        myIterator.remove();
                    }

                    _collection.put(myNew, myNew);
                    
                    Thread.sleep(50);
                } catch (Throwable aT) {
                    aT.printStackTrace(System.err);
                }
            }
        }
    }
}
