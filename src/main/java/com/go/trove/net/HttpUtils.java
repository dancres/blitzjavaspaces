/*
 * HttpUtils.java
 * 
 * Copyright (c) 2000 Walt Disney Internet Group. All Rights Reserved.
 * 
 * Original author: Brian S O'Neill
 * 
 * $Workfile:: HttpUtils.java                                                 $
 *   $Author: dan $
 * $Revision: 1.1 $
 *     $Date: Mon, 13 Oct 2003 12:20:39 +0100 $
 */

package com.go.trove.net;

import java.io.*;

/******************************************************************************
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/04/03 <!-- $-->
 */
public class HttpUtils {
    /**
     * Reads a line from an HTTP InputStream, using the given buffer for
     * temporary storage.
     *
     * @param in stream to read from
     * @param buffer temporary buffer to use
     * @throws IllegalArgumentException if the given InputStream doesn't
     * support marking
     */
    public static String readLine(InputStream in, byte[] buffer)
        throws IllegalArgumentException, IOException
    {
        return readLine(in, buffer, -1);
    }

    /**
     * Reads a line from an HTTP InputStream, using the given buffer for
     * temporary storage.
     *
     * @param in stream to read from
     * @param buffer temporary buffer to use
     * @throws IllegalArgumentException if the given InputStream doesn't
     * support marking
     * @throws LineTooLongException when line is longer than the limit
     */
    public static String readLine(InputStream in, byte[] buffer, int limit)
        throws IllegalArgumentException, IOException, LineTooLongException
    {
        if (!in.markSupported()) {
            throw new IllegalArgumentException
                ("InputStream doesn't support marking: " + in.getClass());
        }

        String line = null;

        int cursor = 0;
        int len = buffer.length;

        int count = 0;
        int c;
    loop:
        while ((c = in.read()) >= 0) {
            if (limit >= 0 && ++count > limit) {
                throw new LineTooLongException(limit);
            }

            switch (c) {
            case '\r':
                in.mark(1);
                if (in.read() != '\n') {
                    in.reset();
                }
                // fall through
            case '\n':
                if (line == null && cursor == 0) {
                    return "";
                }
                break loop;
            default:
                if (cursor >= len) {
                    if (line == null) {
                        line = new String(buffer, "8859_1");
                    }
                    else {
                        line = line.concat(new String(buffer, "8859_1"));
                    }
                    cursor = 0;
                }
                buffer[cursor++] = (byte)c;
            }
        }

        if (cursor > 0) {
            if (line == null) {
                line = new String(buffer, 0, cursor, "8859_1");
            }
            else {
                line = line.concat(new String(buffer, 0, cursor, "8859_1"));
            }
        }

        return line;
    }

    /**
     * Reads a line from an HTTP InputStream, using the given buffer for
     * temporary storage.
     *
     * @param in stream to read from
     * @param buffer temporary buffer to use
     * @throws IllegalArgumentException if the given InputStream doesn't
     * support marking
     */
    public static String readLine(InputStream in, char[] buffer)
        throws IllegalArgumentException, IOException
    {
        return readLine(in, buffer, -1);
    }

    /**
     * Reads a line from an HTTP InputStream, using the given buffer for
     * temporary storage.
     *
     * @param in stream to read from
     * @param buffer temporary buffer to use
     * @throws IllegalArgumentException if the given InputStream doesn't
     * support marking
     * @throws LineTooLongException when line is longer than the limit
     */
    public static String readLine(InputStream in, char[] buffer, int limit)
        throws IllegalArgumentException, IOException, LineTooLongException
    {
        if (!in.markSupported()) {
            throw new IllegalArgumentException
                ("InputStream doesn't support marking: " + in.getClass());
        }

        String line = null;

        int cursor = 0;
        int len = buffer.length;

        int count = 0;
        int c;
    loop:
        while ((c = in.read()) >= 0) {
            if (limit >= 0 && ++count > limit) {
                throw new LineTooLongException(limit);
            }

            switch (c) {
            case '\r':
                in.mark(1);
                if (in.read() != '\n') {
                    in.reset();
                }
                // fall through
            case '\n':
                if (line == null && cursor == 0) {
                    return "";
                }
                break loop;
            default:
                if (cursor >= len) {
                    if (line == null) {
                        line = new String(buffer);
                    }
                    else {
                        line = line.concat(new String(buffer));
                    }
                    cursor = 0;
                }
                buffer[cursor++] = (char)c;
            }
        }

        if (cursor > 0) {
            if (line == null) {
                line = new String(buffer, 0, cursor);
            }
            else {
                line = line.concat(new String(buffer, 0, cursor));
            }
        }

        return line;
    }
}
