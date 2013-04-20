package org.dancres.struct;

/**
   A class to take a bit array and iterate over them reporting offset of
   each set bit.
*/
public class BitVisitorImpl implements BitVisitor {
    private Object[] theLocks;
    private long[] theBits;

    private int theBitGroupIndex = 0;
    private int theBitGroupPtr = 0;

    private int theBase = 0;

    private long theMask = 1L;

    private boolean testZero;

    public BitVisitorImpl(long[] aBits, Object[] aLocks, boolean findZero) {
        theBits = aBits;
        theLocks = aLocks;
        testZero = findZero;
    }

    /**
       @return -1 when no bits are left
     */
    public int getNext() {
        while (theBitGroupIndex < theBits.length) {
            long myLong;

            synchronized(theLocks[theBitGroupIndex]) {
                myLong = theBits[theBitGroupIndex];
            }

            if (myLong != 0) {
                while (theBitGroupPtr < 64) {
                    ++theBitGroupPtr;

                    if (testZero) {
                        if ((myLong & theMask) == 0) {
                            theMask = theMask << 1;
                            return (theBase + theBitGroupPtr - 1);
                        } else 
                            theMask = theMask << 1;
                    } else {
                        if ((myLong & theMask) != 0) {
                            theMask = theMask << 1;
                            return (theBase + theBitGroupPtr - 1);
                        } else 
                            theMask = theMask << 1;
                    }
                }

                // Get ready for next block
                theBitGroupPtr = 0;
            }

            theBase += 64;
            ++theBitGroupIndex;
            theMask = 1L;
        }

        return -1;
    }
}