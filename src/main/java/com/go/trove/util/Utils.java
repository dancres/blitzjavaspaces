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

import java.util.*;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/******************************************************************************
 * Some generic utilities.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 28 <!-- $-->, <!--$$JustDate:--> 00/12/18 <!-- $-->
 * @see java.util.Collections
 */
public class Utils {
    // The choice of determining whether these constants are public or
    // private is based on the design of the java.util.Collections class.
    // For consistency, the "empty" fields are public, and the comparators are
    // accessed from static methods.

    public static final Enumeration EMPTY_ENUMERATION = new EmptyEnum();

    /** This Map is already provided in JDK1.3. */
    public static final Map EMPTY_MAP = new EmptyMap();

    /**
     * Similar to EMPTY_MAP, except the put operation is supported, but the
     * values are immediately forgotten.
     */
    public static final Map VOID_MAP = new VoidMap();

    private static final Comparator NULL_LOW_ORDER = new NullLowOrder();

    private static final Comparator NULL_HIGH_ORDER = new NullHighOrder();

    private static final Comparator NULL_EQUAL_ORDER = new NullEqualOrder();

    private static FlyweightSet cFlyweightSet;
    
    /**
     * Returns a Comparator that uses a Comparable object's natural ordering,
     * except null values are always considered low order. This Comparator
     * allows naturally ordered TreeMaps to support null values.
     */
    public static Comparator nullLowOrder() {
        return NULL_LOW_ORDER;
    }

    /**
     * Returns a Comparator that wraps the given Comparator except null values
     * are always considered low order. This fixes Comparators that don't
     * support comparisons against null and allows them to be used in TreeMaps.
     */
    public static Comparator nullLowOrder(Comparator c) {
        return new NullLowOrderC(c);
    }

    /**
     * Returns a Comparator that uses a Comparable object's natural ordering,
     * except null values are always considered high order. This Comparator
     * allows naturally ordered TreeMaps to support null values.
     */
    public static Comparator nullHighOrder() {
        return NULL_HIGH_ORDER;
    }

    /**
     * Returns a Comparator that wraps the given Comparator except null values
     * are always considered high order. This fixes Comparators that don't
     * support comparisons against null and allows them to be used in TreeMaps.
     */
    public static Comparator nullHighOrder(Comparator c) {
        return new NullHighOrderC(c);
    }

    /**
     * Returns a Comparator that uses a Comparable object's natural ordering,
     * except null values are always considered equal order. This Comparator
     * should not be used in a TreeMap, but can be used in a sorter.
     */
    public static Comparator nullEqualOrder() {
        return NULL_EQUAL_ORDER;
    }
    
    /**
     * Returns a Comparator that wraps the given Comparator except null values
     * are always considered equal order. This Comparator should not be used in
     * a TreeMap, but can be used in a sorter.
     */
    public static Comparator nullEqualOrder(Comparator c) {
        return new NullEqualOrderC(c);
    }

    /**
     * Returns a Comparator that wraps the given Comparator, but orders in
     * reverse.
     */
    public static Comparator reverseOrder(Comparator c) {
        return new ReverseOrderC(c);
    }

    /**
     * Just like {@link String#intern() String.intern}, except it generates
     * flyweights for any kind of object, and it does not prevent them from
     * being garbage collected. Calling intern on a String does not use the
     * same String pool used by String.intern because those Strings are not
     * always garbage collected. Some virtual machines free up Strings from the
     * interned String pool, others do not.
     * <p>
     * For objects that do not customize the hashCode and equals methods,
     * calling intern is not very useful because the object returned will
     * always be the same as the one passed in.
     * <p>
     * The object type returned from intern is guaranteed to be exactly the
     * same type as the one passed in. Calling intern on null returns null.
     *
     * @param obj Object to intern
     * @return Interned object.
     * @see FlyweightSet
     */
    public static Object intern(Object obj) {
        FlyweightSet set;
        if ((set = cFlyweightSet) == null) {
            synchronized (Utils.class) {
                if ((set = cFlyweightSet) == null) {
                    set = new FlyweightSet();
                }
            }
            cFlyweightSet = set;
        }
        return set.put(obj);
    }

    protected Utils() {
    }

    private static class EmptyEnum implements Enumeration, Serializable {
        public boolean hasMoreElements() {
            return false;
        }
        
        public Object nextElement() throws NoSuchElementException {
            throw new NoSuchElementException();
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return EMPTY_ENUMERATION;
        }
    };

    private static class EmptyMap implements Map, Serializable {
        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }
        
        public Object get(Object key) {
            return null;
        }

        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException
                ("Cannot put into immutable empty map");
        }

        public Object remove(Object key) {
            return null;
        }

        public void putAll(Map map) {
            throw new UnsupportedOperationException
                ("Cannot put into immutable empty map");
        }

        public void clear() {
        }

        public Set keySet() {
            return Collections.EMPTY_SET;
        }

        public Collection values() {
            return Collections.EMPTY_LIST;
        }

        public Set entrySet() {
            return Collections.EMPTY_SET;
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return EMPTY_MAP;
        }
    }

    private static class VoidMap extends EmptyMap {
        public Object put(Object key, Object value) {
            return null;
        }

        public void putAll(Map map) {
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return VOID_MAP;
        }
    }

    private static class NullLowOrder implements Comparator, Serializable {
        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? ((Comparable)obj1).compareTo(obj2) : 1;
            }
            else {
                return (obj2 != null) ? -1 : 0;
            }
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return NULL_LOW_ORDER;
        }
    }

    private static class NullLowOrderC implements Comparator, Serializable {
        private Comparator c;

        public NullLowOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? c.compare(obj1, obj2) : 1;
            }
            else {
                return (obj2 != null) ? -1 : 0;
            }
        }
    }

    private static class NullHighOrder implements Comparator, Serializable {
        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? ((Comparable)obj1).compareTo(obj2): -1;
            }
            else {
                return (obj2 != null) ? 1 : 0;
            }
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return NULL_HIGH_ORDER;
        }
    }

    private static class NullHighOrderC implements Comparator, Serializable {
        private Comparator c;

        public NullHighOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            if (obj1 != null) {
                return (obj2 != null) ? c.compare(obj1, obj2) : -1;
            }
            else {
                return (obj2 != null) ? 1 : 0;
            }
        }
    }

    private static class NullEqualOrder implements Comparator, Serializable {
        public int compare(Object obj1, Object obj2) {
            return (obj1 != null && obj2 != null) ?
                ((Comparable)obj1).compareTo(obj2) : 0;
        }

        // Serializable singleton classes should always do this.
        private Object readResolve() throws ObjectStreamException {
            return NULL_EQUAL_ORDER;
        }
    }
    
    private static class NullEqualOrderC implements Comparator, Serializable {
        private Comparator c;

        public NullEqualOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            return (obj1 != null && obj2 != null) ? c.compare(obj1, obj2) : 0;
        }
    }

    private static class ReverseOrderC implements Comparator, Serializable {
        private Comparator c;

        public ReverseOrderC(Comparator c) {
            this.c = c;
        }

        public int compare(Object obj1, Object obj2) {
            return c.compare(obj2, obj1);
        }
    }
}
