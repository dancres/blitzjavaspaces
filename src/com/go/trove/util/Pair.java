/* ====================================================================
 * Trove - Copyright (c) 1997-2001 Walt Disney Internet Group
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

/******************************************************************************
 * Simple object for pairing two objects together for use as a hash or tree
 * key.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/07/09 <!-- $-->
 * @see MultiKey
 */
public class Pair implements Comparable, java.io.Serializable {
    private final Object mObj1;
    private final Object mObj2;

    public Pair(Object obj1, Object obj2) {
        mObj1 = obj1;
        mObj2 = obj2;
    }
    
    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }

        Pair other = (Pair)obj;

        Object a = mObj1;
        Object b = other.mObj1;

        firstTest: {
            if (a == null) {
                if (b != null) {
                    return 1;
                }
                // Both a and b are null.
                break firstTest;
            }
            else {
                if (b == null) {
                    return -1;
                }
            }

            int result = ((Comparable)a).compareTo(b);
            
            if (result != 0) {
                return result;
            }
        }

        a = mObj2;
        b = other.mObj2;
        
        if (a == null) {
            if (b != null) {
                return 1;
            }
            // Both a and b are null.
            return 0;
        }
        else {
            if (b == null) {
                return -1;
            }
        }
        
        return ((Comparable)a).compareTo(b);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Pair)) {
            return false;
        }
        
        Pair key = (Pair)obj;
        
        return 
            (mObj1 == null ?
             key.mObj1 == null : mObj1.equals(key.mObj1)) &&
            (mObj2 == null ?
             key.mObj2 == null : mObj2.equals(key.mObj2));
    }
    
    public int hashCode() {
        return 
            (mObj1 == null ? 0 : mObj1.hashCode()) +
            (mObj2 == null ? 0 : mObj2.hashCode());
    }
    
    public String toString() {
        return "[" + mObj1 + ':' + mObj2 + ']';
    }
}
