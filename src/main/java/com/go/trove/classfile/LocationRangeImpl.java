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

package com.go.trove.classfile;

/******************************************************************************
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
class LocationRangeImpl implements LocationRange {
    private final Location mStart;
    private final Location mEnd;

    LocationRangeImpl(Location a, Location b) {
        if (a.compareTo(b) <= 0) {
            mStart = a;
            mEnd = b;
        }
        else {
            mStart = b;
            mEnd = a;
        }
    }

    LocationRangeImpl(LocationRange a, LocationRange b) {
        mStart = (a.getStartLocation().compareTo(b.getStartLocation()) <= 0) ?
            a.getStartLocation() : b.getStartLocation();

        mEnd = (b.getEndLocation().compareTo(a.getEndLocation()) >= 0) ?
            b.getEndLocation() : a.getEndLocation();
    }

    public Location getStartLocation() {
        return mStart;
    }

    public Location getEndLocation() {
        return mEnd;
    }

    public boolean equals(Object obj) {
        return compareTo(obj) == 0;
    }

    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }

        LocationRange other = (LocationRange)obj;

        int result = getStartLocation().compareTo(other.getStartLocation());

        if (result == 0) {
            result = getEndLocation().compareTo(other.getEndLocation());
        }

        return result;
    }

    public String toString() {
        int start = getStartLocation().getLocation();
        int end = getEndLocation().getLocation() - 1;

        if (start == end) {
            return String.valueOf(start);
        }
        else {
            return start + ".." + end;
        }
    }
}
