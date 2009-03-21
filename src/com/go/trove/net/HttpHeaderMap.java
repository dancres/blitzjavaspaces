/* ====================================================================
 * Trove - Copyright (c) 1997-2000 Walt Disney Internet Group
 * ====================================================================
 * The Tea Software License, Version 1.1
 *
 * Copyright (c) 2000 Walt Disney Internet Group. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Walt Disney Internet Group (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "BeanDoc" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@dig.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle", "Trove" or "BeanDoc" appear in their name, without prior
 *    written permission of the Walt Disney Internet Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE WALT DISNEY INTERNET GROUP OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */

package com.go.trove.net;

import java.util.*;
import java.text.*;
import java.io.*;
import com.go.trove.io.*;

/******************************************************************************
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/07/16 <!-- $-->
 */
public class HttpHeaderMap implements Map, Serializable {
    private final static TimeZone GMT_ZONE;
    private final static DateFormat DATE_PARSER_1;
    private final static DateFormat DATE_PARSER_2;
    private final static String[] DAYS;
    private final static String[] MONTHS;

    static {
        GMT_ZONE = TimeZone.getTimeZone("GMT");

        DATE_PARSER_1 =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
        DATE_PARSER_1.setTimeZone(GMT_ZONE);
        DATE_PARSER_1.setLenient(true);

        DATE_PARSER_2 =
            new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss", Locale.US);
        DATE_PARSER_2.setTimeZone(GMT_ZONE);
        DATE_PARSER_2.setLenient(true);

        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        DAYS = symbols.getShortWeekdays();
        MONTHS = symbols.getShortMonths();
    }

    private static void appendDate(CharToByteBuffer buffer, Date d)
        throws IOException
    {
        Calendar c = new GregorianCalendar(GMT_ZONE, Locale.US);
        c.setTime(d);

        buffer.append(DAYS[c.get(Calendar.DAY_OF_WEEK)]);
        buffer.append(", ");
        append2Digit(buffer, c.get(Calendar.DAY_OF_MONTH));
        buffer.append(' ');
        buffer.append(MONTHS[c.get(Calendar.MONTH)]);
        buffer.append(' ');
        append4Digit(buffer, c.get(Calendar.YEAR));
        buffer.append(' ');
        append2Digit(buffer, c.get(Calendar.HOUR_OF_DAY));
        buffer.append(':');
        append2Digit(buffer, c.get(Calendar.MINUTE));
        buffer.append(':');
        append2Digit(buffer, c.get(Calendar.SECOND));
        buffer.append(" GMT");
    }

    private static void append2Digit(CharToByteBuffer buffer, int val)
        throws IOException
    {
        buffer.append((char)(val / 10 + '0'));
        buffer.append((char)(val % 10 + '0'));
    }

    private static void append4Digit(CharToByteBuffer buffer, int val)
        throws IOException
    {
        if (val < 1000 || val > 9999) {
            buffer.append(Integer.toString(val));
        }
        else {
            buffer.append((char)(val / 1000 + '0'));
            buffer.append((char)(val / 100 % 10 + '0'));
            buffer.append((char)(val / 10 % 10 + '0'));
            buffer.append((char)(val % 10 + '0'));
        }
    }

    private Map mMap;

    public HttpHeaderMap() {
        mMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Read and parse headers from the given InputStream until a blank line is
     * reached. Except for Cookies, all other headers that contain multiple
     * fields delimited by commas are parsed into multiple headers.
     *
     * @param in stream to read from
     */
    public void readFrom(InputStream in) throws IOException {
        readFrom(in, new byte[80]);
    }

    /**
     * Read and parse headers from the given InputStream until a blank line is
     * reached. Except for Cookies, all other headers that contain multiple
     * fields delimited by commas are parsed into multiple headers.
     *
     * @param in stream to read from
     * @param buffer temporary buffer to use
     */
    public void readFrom(InputStream in, byte[] buffer) throws IOException {
        String header;
        while ((header = HttpUtils.readLine(in, buffer, 4000)) != null) {
            if (header.length() == 0) {
                break;
            }
            processHeaderLine(header);
        }
    }

    /**
     * Read and parse headers from the given InputStream until a blank line is
     * reached. Except for Cookies, all other headers that contain multiple
     * fields delimited by commas are parsed into multiple headers.
     *
     * @param in stream to read from
     * @param buffer temporary buffer to use
     */
    public void readFrom(InputStream in, char[] buffer) throws IOException {
        String header;
        while ((header = HttpUtils.readLine(in, buffer, 4000)) != null) {
            if (header.length() == 0) {
                break;
            }
            processHeaderLine(header);
        }
    }

    private void processHeaderLine(String header) {
        int index = header.indexOf(':');
        if (index < 0) {
            return;
        }
        
        String name = header.substring(0, index);

        String value;
        int length = header.length();
        parseValue: {
            do {
                if (++index >= length) {
                    value = "";
                    break parseValue;
                }
            } while (header.charAt(index) == ' ');

            value = header.substring(index);
        }
        
        if ("Cookie".equalsIgnoreCase(name) ||
            "Set-Cookie".equalsIgnoreCase(name) ||
            (value.indexOf(',') == 3 &&
             (value.startsWith("Mon") ||
              value.startsWith("Tue") ||
              value.startsWith("Wed") ||
              value.startsWith("Thu") ||
              value.startsWith("Fri") ||
              value.startsWith("Sat") ||
              value.startsWith("Sun")))) {
            
            add(name, value.trim());
        }
        else {
            // Parse up header by commas unless its a Cookie or value
            // is a date.
            while ((index = value.indexOf(',')) >= 0) {
                add(name, value.substring(0, index).trim());
                value = value.substring(index + 1);
            }
            
            add(name, value.trim());
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        CharToByteBuffer buffer = new FastCharToByteBuffer
            (new DefaultByteBuffer(), "8859_1");
        buffer = new InternedCharToByteBuffer(buffer);
        appendTo(buffer);
        buffer.writeTo(out);
    }
    
    public void appendTo(CharToByteBuffer buffer) throws IOException {
        Iterator it = mMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            String strKey = key.toString();

            if (value instanceof List) {
                Iterator values = ((List)value).iterator();
                if ("Set-Cookie".equalsIgnoreCase(strKey)) {
                    while (values.hasNext()) {
                        buffer.append(strKey);
                        buffer.append(": ");
                        value = values.next();
                        if (value instanceof Date) {
                            appendDate(buffer, (Date)value);
                        }
                        else {
                            buffer.append(value.toString());
                        }
                        buffer.append("\r\n");
                    }
                }
                else {
                    // Write multiple headers together except Cookies.
                    int count = 0;
                    while (values.hasNext()) {
                        value = values.next();

                        if (count < 0) {
                            buffer.append("\r\n");
                            count = 0;
                        }
                        if (count++ == 0) {
                            buffer.append(strKey);
                            buffer.append(": ");
                        }

                        if (value instanceof Date) {
                            // Comma in date, so must isolate header.
                            if (count > 1) {
                                buffer.append("\r\n");
                                buffer.append(strKey);
                                buffer.append(": ");
                            }
                            appendDate(buffer, (Date)value);
                            count = -1;
                        }
                        else {
                            String strVal = value.toString();
                            if (strVal.indexOf(',') < 0) {
                                if (count > 1) {
                                    buffer.append(',');
                                }
                                buffer.append(strVal);
                            }
                            else {  
                                // Comma in value, so must isolate header.
                                if (count > 1) {
                                    buffer.append("\r\n");
                                    buffer.append(strKey);
                                    buffer.append(": ");
                                }
                                buffer.append(strVal);
                                count = -1;
                            }
                        }
                    }
                    buffer.append("\r\n");
                }
            }
            else {
                buffer.append(strKey);
                buffer.append(": ");
                if (value instanceof Date) {
                    appendDate(buffer, (Date)value);
                }
                else {
                    buffer.append(value.toString());
                }
                buffer.append("\r\n");
            }
        }
    }

    public int size() {
        return mMap.size();
    }

    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return mMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        Iterator it = mMap.values().iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof List) {
                Iterator it2 = ((List)obj).iterator();
                while (it2.hasNext()) {
                    obj = it2.next();
                    return (value == null) ? obj == null : value.equals(obj);
                }
            }
            else {
                return (value == null) ? obj == null : value.equals(obj);
            }
        }
        return false;
    }
    
    /**
     * Returns the first value associated with the given key.
     */
    public Object get(Object key) {
        Object value = mMap.get(key);
        if (value instanceof List) {
            return ((List)value).get(0);
        }
        else {
            return value;
        }
    }

    public String getString(Object key) {
        Object obj = get(key);
        if (obj instanceof String) {
            return (String)obj;
        }
        else if (obj != null) {
            return obj.toString();
        }
        return null;
    }

    public Integer getInteger(Object key) {
        Object obj = get(key);
        if (obj instanceof Integer) {
            return (Integer)obj;
        }
        else if (obj != null) {
            try {
                return new Integer(obj.toString());
            }
            catch (NumberFormatException e) {
            }
        }
        return null;
    }

    public Date getDate(Object key) {
        Object obj = get(key);
        if (obj instanceof Date) {
            return (Date)obj;
        }
        else if (obj == null) {
            return null;
        }

        String val = obj.toString();

        // Trim after a possible ';' to separate the date
        // from other optional data.
        int index = val.indexOf(';');
        if (index >= 0) {
            val = val.substring(0, index);
        }

        Date date;

        try {
            synchronized (DATE_PARSER_1) {
                date = DATE_PARSER_1.parse(val);
            }
        }
        catch (ParseException e) {
            try {
                synchronized (DATE_PARSER_2) {
                    date = DATE_PARSER_2.parse(val);
                }
            }
            catch (ParseException e2) {
                return null;
            }
        }

        return date;
    }
    

    /**
     * Returns all the values associated with the given key. Changes to the
     * returned list will be reflected in this map.
     */
    public List getAll(Object key) {
        Object value = mMap.get(key);
        if (value instanceof List) {
            return ((List)value);
        }
        else {
            List list = new ArrayList();
            if (value != null || mMap.containsKey(key)) {
                list.add(value);
            }
            mMap.put(key, list);
            return list;
        }
    }

    /**
     * May return a list if the key previously mapped to multiple values.
     */
    public Object put(Object key, Object value) {
        if (value instanceof List) {
            return mMap.put(key, new ArrayList((List)value));
        }
        else {
            return mMap.put(key, value);
        }
    }

    /**
     * Add more than one value associated with the given key.
     */
    public void add(Object key, Object value) {
        Object existing = mMap.get(key);
        if (existing instanceof List) {
            if (value instanceof List) {
                ((List)existing).addAll((List)value);
            }
            else {
                ((List)existing).add(value);
            }
        }
        else if (existing == null && !mMap.containsKey(key)) {
            if (value instanceof List) {
                mMap.put(key, new ArrayList((List)value));
            }
            else {
                mMap.put(key, value);
            }
        }
        else {
            List list = new ArrayList();
            list.add(existing);
            if (value instanceof List) {
                list.addAll((List)value);
            }
            else {
                list.add(value);
            }
            mMap.put(key, list);
        }
    }

    public Object remove(Object key) {
        return mMap.remove(key);
    }

    public void putAll(Map map) {
        mMap.putAll(map);
    }

    public void clear() {
        mMap.clear();
    }

    public Set keySet() {
        return mMap.keySet();
    }

    public Collection values() {
        return mMap.values();
    }

    public Set entrySet() {
        return mMap.entrySet();
    }

    public boolean equals(Object obj) {
        return mMap.equals(obj);
    }

    public int hashCode() {
        return mMap.hashCode();
    }

    public String toString() {
        return super.toString() + ":" + mMap.toString();
    }
}
