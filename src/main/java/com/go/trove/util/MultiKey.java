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

package com.go.trove.util;

import java.util.Arrays;

/******************************************************************************
 * MultiKey allows arrays and arrays of arrays to be used as hashtable keys.
 * Hashcode computation and equality tests will fully recurse into the array
 * elements. MultiKey can be used in conjunction with {@link Depot} for
 * caching against complex keys.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/07/09 <!-- $-->
 * @see Pair
 */
public class MultiKey implements java.io.Serializable {
    /**
     * Computes an object hashcode for any kind of object including null,
     * arrays, and arrays of arrays.
     */    
    static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }

        Class clazz = obj.getClass();

        if (!clazz.isArray()) {
            return obj.hashCode();
        }
        
        // Compute hashcode of array.

        int hash = clazz.hashCode();
        
        if (obj instanceof Object[]) {
            Object[] array = (Object[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + hashCode(array[i]);
            }
        }
        else if (obj instanceof int[]) {
            int[] array = (int[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else if (obj instanceof float[]) {
            float[] array = (float[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + Float.floatToIntBits(array[i]);
            }
        }
        else if (obj instanceof long[]) {
            long[] array = (long[])obj;
            for (int i = array.length; --i >= 0; ) {
                long value = array[i];
                hash = hash * 31 + (int)(value ^ value >>> 32);
            }
        }
        else if (obj instanceof double[]) {
            double[] array = (double[])obj;
            for (int i = array.length; --i >= 0; ) {
                long value = Double.doubleToLongBits(array[i]);
                hash = hash * 31 + (int)(value ^ value >>> 32);
            }
        }
        else if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else if (obj instanceof char[]) {
            char[] array = (char[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        else if (obj instanceof boolean[]) {
            boolean[] array = (boolean[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + (array[i] ? 1 : 0);
            }
        }
        else if (obj instanceof short[]) {
            short[] array = (short[])obj;
            for (int i = array.length; --i >= 0; ) {
                hash = hash * 31 + array[i];
            }
        }
        
        return hash;
    }
    
    /**
     * Performans an object equality for any kind of objects including null,
     * arrays, and arrays of arrays.
     */    
    static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        else if (obj1 == null || obj2 == null) {
            return false;
        }

        Class clazz1 = obj1.getClass();

        if (!(clazz1.isArray())) {
            return obj1.equals(obj2);
        }
        
        if (clazz1 != obj2.getClass()) {
            return false;
        }
        
        // Perform array equality test.
        if (obj1 instanceof Object[]) {
            // Don't use Arrays.equals for objects since it doesn't
            // recurse into arrays of arrays.
            Object[] array1 = (Object[])obj1;
            Object[] array2 = (Object[])obj2;
            
            int i;
            if ((i = array1.length) != array2.length) {
                return false;
            }
            
            while (--i >= 0) {
                if (!equals(array1[i], array2[i])) {
                    return false;
                }
            }
            
            return true;
        }
        else if (obj1 instanceof int[]) {
            return Arrays.equals((int[])obj1, (int[])obj2);
        }
        else if (obj1 instanceof float[]) {
            return Arrays.equals((float[])obj1, (float[])obj2);
        }
        else if (obj1 instanceof long[]) {
            return Arrays.equals((long[])obj1, (long[])obj2);
        }
        else if (obj1 instanceof double[]) {
            return Arrays.equals((double[])obj1, (double[])obj2);
        }
        else if (obj1 instanceof byte[]) {
            return Arrays.equals((byte[])obj1, (byte[])obj2);
        }
        else if (obj1 instanceof char[]) {
            return Arrays.equals((char[])obj1, (char[])obj2);
        }
        else if (obj1 instanceof boolean[]) {
            return Arrays.equals((boolean[])obj1, (boolean[])obj2);
        }
        else if (obj1 instanceof short[]) {
            return Arrays.equals((short[])obj1, (short[])obj2);
        }
        else {
            return obj1.equals(obj2);
        }
    }

    private final Object mComponent;
    private final int mHash;

    /**
     * Contruct a new MultiKey against a component which may be any kind of
     * object including an array, or an array of arrays, or null.
     */
    public MultiKey(Object component) {
        mComponent = component;
        mHash = MultiKey.hashCode(component);
    }

    /**
     * Returns the original component used to construct this MultiKey.
     */
    public Object getComponent() {
        return mComponent;
    }

    public int hashCode() {
        return mHash;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        else if (other instanceof MultiKey) {
            MultiKey key = (MultiKey)other;
            return MultiKey.equals(mComponent, key.mComponent);
        }
        else {
            return false;
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        append(buf, mComponent);
        return buf.toString();
    }

    private void append(StringBuffer buf, Object obj) {
        if (obj == null) {
            buf.append("null");
            return;
        }

        if (!obj.getClass().isArray()) {
            buf.append(obj);
            return;
        }

        buf.append('[');

        if (obj instanceof Object[]) {
            Object[] array = (Object[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                append(buf, array[i]);
            }
        }
        else if (obj instanceof int[]) {
            int[] array = (int[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof float[]) {
            float[] array = (float[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof long[]) {
            long[] array = (long[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof double[]) {
            double[] array = (double[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof char[]) {
            char[] array = (char[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof boolean[]) {
            boolean[] array = (boolean[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else if (obj instanceof short[]) {
            short[] array = (short[])obj;
            for (int i=0; i<array.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(array[i]);
            }
        }
        else {
            buf.append(obj);
        }

        buf.append(']');
    }
}
