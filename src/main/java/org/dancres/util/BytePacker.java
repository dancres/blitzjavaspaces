package org.dancres.util;

/**
   A class to pack and unpack longs or ints into a byte array
 */
public abstract class BytePacker {

    /**
       @return BytePacker that stores things in MSB order
     */
    public static BytePacker getMSBPacker() {
        return new MSBBytePacker();
    }

    /**
       @return BytePacker that stores things in MSB order
     */
    public static BytePacker getMSBPacker(byte[] aBytes) {
        return new MSBBytePacker(aBytes);
    }

    public abstract void setData(byte[] aBytes);
    public abstract int getInt(int anOffset);
    public abstract void putArray(byte[] anArray, int anOffset);
    public abstract byte[] getArray(int anOffset, int aLength);
    public abstract long getLong(int anOffset);
    public abstract void putInt(int anInt, int anOffset);
    public abstract void putLong(long aLong, int anOffset);
}
