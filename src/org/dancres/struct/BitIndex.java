package org.dancres.struct;

import java.util.ArrayList;

import java.util.LinkedList;

import java.util.Random;

import java.util.HashMap;

/**
   <p>An MT-safe BitIndex.  Because BitIndex's can never change size and all
   updates are lock protected, there is no need to clone an index via copy
   unless one requires a stable copy.  i.e.  If one can tolerate an out-of-date
   access that may render misses, the cost of a copy can be avoided.  Iteration
   incurs locking therefore but only to access a snapshot of data bits.</p>

   <p>Be sure to run this with plenty of memory to avoid GC skewing timings
   during tests.</p>
 */
public class BitIndex {
    private Object[] theLocks;
    private long[] theIndex;
    private int theSize;

    private Object theTotalLock = new Object();
    private int theTotalSet;

    public BitIndex(int aSize) {
        theSize = aSize;

        int mySize = aSize / 64;

        if ((aSize % 64) != 0)
            ++mySize;

        theIndex = new long[mySize];
        theLocks = new Object[mySize];
        for (int i = 0; i < theLocks.length; i++) {
            theLocks[i] = new Object();
        }
    }

    public int getSize() {
        return theSize;
    }

    public void set(int aPos) {
        if (aPos >= theSize)
            throw new IllegalArgumentException("Position larger than size");

        int myIndex = aPos / 64;
        int myOffset = aPos % 64;
        long myNewBit = 1L << myOffset;

        synchronized(theLocks[myIndex]) {
            theIndex[myIndex] |= myNewBit;
        }

        synchronized(theTotalLock) {
            ++theTotalSet;
        }
    }

    public void clear(int aPos) {
        if (aPos >= theSize)
            throw new IllegalArgumentException("Position larger than size");

        int myIndex = aPos / 64;
        int myOffset = aPos % 64;
        long myNewBit = 1L << myOffset;
        myNewBit ^= 0xffffffffffffffffL;

        synchronized(theLocks[myIndex]) {
            theIndex[myIndex] &= myNewBit;
        }

        synchronized(theTotalLock) {
            --theTotalSet;
        }
    }

    public int count() {
        synchronized(theTotalLock) {
            return theTotalSet;
        }
    }

    public BitVisitor getVisitor(boolean forZero) {
        return new BitVisitorImpl(theIndex, theLocks, forZero);
    }

    public void iterate(BitListener aListener) {
        int myBase = 0;

        for (int i = 0; i < theIndex.length; i++) {
            long myMask = 1L;
            long myLong;

            synchronized(theLocks[i]) {
                myLong = theIndex[i];
            }

            if (myLong != 0) {
                for (int j = 0; j < 64; j++) {
                    if ((myLong & myMask) != 0)
                        if (aListener.active(myBase + j))
                            return;
                    
                    myMask = myMask << 1;
                }
            }

            myBase += 64;
        }
    }

    public void iterateZero(BitListener aListener) {
        int myBase = 0;

        for (int i = 0; i < theIndex.length; i++) {
            long myMask = 1L;
            long myLong;
            
            synchronized(theLocks[i]) {
                myLong = theIndex[i];
            }

            for (int j = 0; j < 64; j++) {
                if ((myLong & myMask) == 0)
                    if (aListener.inactive(myBase + j))
                        return;
                
                myMask = myMask << 1;
            }

            myBase += 64;
        }
    }

    public BitIndex copy() {
        BitIndex myIndex = new BitIndex(theSize);
        System.arraycopy(theIndex, 0, myIndex.theIndex, 0, theIndex.length);

        return myIndex;
    }

    public long[] rawCopy() {
        long[] myRaw = new long[theIndex.length];

        System.arraycopy(theIndex, 0, myRaw, 0, theIndex.length);

        return myRaw;
    }

    public static void main(String args[]) {
        BitIndex myIndex = new BitIndex(512);

        myIndex.set(1);

        System.out.println(Long.toBinaryString(myIndex.theIndex[0]));

        myIndex.set(35);

        System.out.println(Long.toBinaryString(myIndex.theIndex[0]));

        myIndex.set(67);

        System.out.println(Long.toBinaryString(myIndex.theIndex[1]));

        myIndex.clear(35);

        System.out.println(Long.toBinaryString(myIndex.theIndex[0]));

        myIndex.set(35);

        myIndex.iterate(new TestListener());

        myIndex.set(63);

        System.out.println(Long.toBinaryString(myIndex.theIndex[0]));

        BitVisitor myVisitor = myIndex.getVisitor(false);

        int mySlot;

        while ((mySlot = myVisitor.getNext()) != -1) {
            System.out.print("Bit set at: " + mySlot + " ");
        }
        System.out.println();
        
        myVisitor = myIndex.getVisitor(true);

        while ((mySlot = myVisitor.getNext()) != -1) {
            System.out.print("Bit clear at: " + mySlot + " ");
        }
        System.out.println();

        /*
          Here we test out how fast we might be able to do add/remove's
          from a cache indexer based on a HashMap which is 
          secondary indexed via a HashMap of hashcodes containing buckets
          implemented as ArrayLists vs a combination of array,
          indexed by HashMap to get offsets and then secondary indexed via a
          HashMap of hashcodes containing BitMap indexes
         */
        ArrayList myList = new ArrayList(512);

        for (int i = 0; i < 512; i++) {
            myList.add(new Integer(i));
        }

        HashMap myMap = new HashMap();

        for (int i = 0; i < 512; i++) {
            myIndex.set(i);
            myMap.put(new Integer(i), new Integer(i));
        }

        Random myRandom = new Random();

        long myStart = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            Integer myId = new Integer(myRandom.nextInt(512));

            myMap.remove(myId);
            myList.remove(myId);
            myList.add(myId);
            myMap.put(myId, myId);
        }

        long myEnd = System.currentTimeMillis();

        System.out.println("100000 remove/adds on ArrayList: " +
                           (myEnd - myStart));

        myIndex = new BitIndex(512);

        myStart = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            int myId = myRandom.nextInt(512);

            Integer myPos = (Integer) myMap.remove(new Integer(myId));
            myIndex.clear(myPos.intValue());

            myMap.put(myPos, myPos);
            myIndex.set(myPos.intValue());
        }

        myEnd = System.currentTimeMillis();

        System.out.println("100000 remove/adds on Bitmap Indexed Map: " +
                           (myEnd - myStart));


        /*
          Now we test how a search might behave - first case is based on
          copying the entire contents of a secondary index containing
          primary keys whilst the second case consists of BitMap indexes
          which index into one array of primary keys.  Thus in the second
          case we must at least copy the bitmap index and probably ought to
          copy the keys (note we could copy the keys in chunks same as we
          lock and copy chunks of the secondary indexes in the first case).

          In fact we don't need to copy the base array in the BitIndex case
          as an approximation is acceptable.  If the entry that would be a
          partial hit has been replaced, we'll filter it out later.  However,
          in high workload's this might give us a lot of pointless searches.
         */
        myStart = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            Object[] myArray = new Object[512];
            myArray = myList.toArray(myArray);
        }

        myEnd = System.currentTimeMillis();

        System.out.println("100000 copies of array: " + (myEnd - myStart));

        myStart = System.currentTimeMillis();

        for (int i = 0 ; i < 100000; i++) {
            BitIndex myBits = myIndex.copy();
        }

        myEnd = System.currentTimeMillis();

        System.out.println("100000 copies of bitset: " +
                           (myEnd - myStart));

        myStart = System.currentTimeMillis();

        for (int i = 0 ; i < 100000; i++) {
            Object[] myArray = new Object[512];
            myArray = myList.toArray(myArray);
            myIndex.copy();
        }

        myEnd = System.currentTimeMillis();

        System.out.println("100000 copies of array and bitset: " +
                           (myEnd - myStart));


        myStart = System.currentTimeMillis();

    }

    private static class TestListener implements BitListener {
        public boolean active(int aPos) {
            System.out.println("Slot: " + aPos + " is active");
            return false;
        }

        public boolean inactive(int aPos) {
            throw new org.dancres.util.NotImplementedException();
        }
    }
}