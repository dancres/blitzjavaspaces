package org.dancres.util;

/**
   A class to pack and unpack longs or ints into a byte array.
   In this case, longs and ints are packed MSB first
 */
public class MSBBytePacker extends BytePacker {
    private byte[] theBytes;

    MSBBytePacker() {
    }

    MSBBytePacker(byte[] aBytes) {
        theBytes = aBytes;
    }

    public void setData(byte[] aBytes) {
        theBytes = aBytes;
    }

    public int getInt(int anOffset) {
        int b1 = theBytes[anOffset] << 24;
        int b2 = theBytes[anOffset + 1] << 16;
        int b3 = theBytes[anOffset + 2] << 8;
        int b4 = theBytes[anOffset + 3] <<0;

        return ((b1 & 0xFF000000) | (b2 & 0x00FF0000) |
                (b3 & 0X0000FF00) | (b4 & 0x000000FF));
    }

    public void putArray(byte[] anArray, int anOffset) {
        System.arraycopy(anArray, 0, theBytes, anOffset, anArray.length);
    }

    public byte[] getArray(int anOffset, int aLength) {
        byte[] myArray = new byte[aLength];

        System.arraycopy(theBytes, anOffset, myArray, 0, aLength);

        return myArray;
    }

    public long getLong(int anOffset) {
        return (((long) getInt(anOffset)) << 32) +
            ((getInt(anOffset + 4)) & 0xFFFFFFFFL);
    }

    public void putInt(int anInt, int anOffset) {
        theBytes[anOffset] = (byte) ((anInt >>> 24) & 0xFF);
        theBytes[anOffset + 1] = (byte) ((anInt >>> 16) & 0xFF);
        theBytes[anOffset + 2] = (byte) ((anInt >>> 8) & 0xFF);
        theBytes[anOffset + 3] = (byte) ((anInt >>> 0) & 0xFF);
    }

    public void putLong(long aLong, int anOffset) {
        theBytes[anOffset] = (byte) ((aLong >>> 56) & 0xFF);
        theBytes[anOffset + 1] = (byte) ((aLong >>> 48) & 0xFF);
        theBytes[anOffset + 2] = (byte) ((aLong >>> 40) & 0xFF);
        theBytes[anOffset + 3] = (byte) ((aLong >>> 32) & 0xFF);
        theBytes[anOffset + 4] = (byte) ((aLong >>> 24) & 0xFF);
        theBytes[anOffset + 5] = (byte) ((aLong >>> 16) & 0xFF);
        theBytes[anOffset + 6] = (byte) ((aLong >>> 8) & 0xFF);
        theBytes[anOffset + 7] = (byte) ((aLong >>> 0) & 0xFF);
    }
}
