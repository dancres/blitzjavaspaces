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

/******************************************************************************
 * A Set that orders its elements based on how recently they have been used.
 * Most recently used elements appear first in the Set. Elements are marked as
 * being used whenever they are added to the Set. To re-position an element,
 * re-add it.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 16 <!-- $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class UsageSet extends MapBackedSet {
    /**
     * Creates a UsageSet in forward order, MRU first.
     */
    public UsageSet() {
        this(new HashMap());
    }

    /**
     * @param backingMap map to use for storage
     */
    public UsageSet(Map backingMap) {
        super(new UsageMap(backingMap));
    }

    /**
     * With reverse order, elements are ordered least recently used first. The
     * ordering of the elements will be consistent with the order they were
     * added in. Switching to and from reverse order is performed quickly
     * and is not affected by the current size of the set.
     */
    public void setReverseOrder(boolean reverse) {
        ((UsageMap)mMap).setReverseOrder(reverse);
    }

    /**
     * Returns the first element in the set, the most recently used. If reverse
     * order, then the least recently used is returned.
     */
    public Object first() throws NoSuchElementException {
        return ((UsageMap)mMap).firstKey();
    }

    /**
     * Returns the last element in the set, the least recently used. If reverse
     * order, then the most recently used is returned.
     */
    public Object last() throws NoSuchElementException {
        return ((UsageMap)mMap).lastKey();
    }
}
