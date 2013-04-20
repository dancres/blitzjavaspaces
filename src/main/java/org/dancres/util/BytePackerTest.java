package org.dancres.util;

public class BytePackerTest {
    public static void main(String args[]) {
        long myFirstLong = 0x7654321076543210L;
        long mySecondLong = 0xfedcba98fedcba98L;
        int myFirstInt = 0x76543210;
        int mySecondInt = 0xfedcba98;

        byte[] myBytes = new byte[12];

        BytePacker myPacker = BytePacker.getMSBPacker(myBytes);

        myPacker.putInt(myFirstInt, 0);
        myPacker.putLong(mySecondLong, 4);

        int myInt = myPacker.getInt(0);
        long myLong = myPacker.getLong(4);

        System.out.println(Integer.toHexString(myInt));
        System.out.println(Long.toHexString(myLong));

        myPacker.putInt(mySecondInt, 0);
        myPacker.putLong(myFirstLong, 4);

        myInt = myPacker.getInt(0);
        myLong = myPacker.getLong(4);

        System.out.println(Integer.toHexString(myInt));
        System.out.println(Long.toHexString(myLong));
    }
}
