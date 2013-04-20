package org.dancres.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class NumUtil {
    private static final String GB = "g";
    private static final String MB = "m";
    private static final String KB = "k";

    public static long convertToBytes(String theDbCacheSizeValue)
        throws IllegalArgumentException {

        String format = "\\d\\s*[" + GB + MB + KB + "]?";

        // check format
        //
        Pattern pattern = Pattern.compile(format, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(theDbCacheSizeValue);
        if (!matcher.find())
            throw new IllegalArgumentException("Could not parse " +
                theDbCacheSizeValue +
                ". Should be of the form '0-9[k|m|g]'");

        // grab the number piece
        //
        String splitOnUOM = "[kKmMgG]";
        String[] splitOnUOMArr = theDbCacheSizeValue.split(splitOnUOM);
        long size = Long.parseLong(splitOnUOMArr[0].trim());

        // grab the unit of measure piece
        //
        String uom = theDbCacheSizeValue.substring(splitOnUOMArr[0].length());
        if (uom.length() > 0)
            uom = uom.substring(0, 1);

        // convert the size to bytes
        //
        long sizeInBytes = size;
        if (KB.equalsIgnoreCase(uom))
            sizeInBytes = size * 1024;
        else if (MB.equalsIgnoreCase(uom))
            sizeInBytes = size * 1024 * 1024;
        else if (GB.equalsIgnoreCase(uom))
            sizeInBytes = size * 1024 * 1024 * 1024;
        else
            sizeInBytes = size;

        return sizeInBytes;
    }

    public static void main(String anArgs[]) throws Exception {
        System.out.println(convertToBytes(anArgs[0]));
    }
}
