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
 * An extension of ArrayList that insures that all of the items added are 
 * sorted. A binary search method has been added to provide a quick way to 
 * auto sort this Collection. The method call for this search has been made 
 * public so that this Collection can be searched.
 * Note: Not all methods for adding and removing elements are supported.
 *
 * @author Sean T. Treat
 * @version
 * @see java.util.Collection
 * @see java.util.List
 * @see java.util.ArrayList
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  5/04/01 <!-- $-->
 */
public class SortedArrayList extends ArrayList {

    //
    // Data Members
    //

    private Comparator mComparator = null;

    /**
     * Constructs a new SortedArrayList. To guarantee that this Collection
     * is properly sorted, this constructor should be called.
     * @param c The comparator to use when sorting this Collection.
     */
    public SortedArrayList(Comparator c) {
        mComparator = c;
    }

    public SortedArrayList() {
    }
    
    public SortedArrayList(Collection c) {
        addAll(c);
    }

    /**
     * @return the Comparator that has been assigned to this Collection.
     */
    public Comparator comparator() {
        return mComparator;
    }

    /**
     * Adds an Object to this Collection. 
     * @param o The Object to be added.
     * @return true if this Collection is modified as a result of this call. 
     */
    public boolean add(Object o) {
        // find the index where this item should go
        // add the item @ the specified index
        int idx = 0;
        if(!isEmpty()) {
            idx = findInsertionPoint(o);
        }

        try {
            super.add(idx, o);
        }
        catch(IndexOutOfBoundsException e) {
            return false;
        }
        return true;
    }

    /**
     * Add all of the elements in the c to this List.
     * @param c The Collection that is to be added.
     * @return true if this Collection is altered. Otherwise, false.
     */
    public boolean addAll(Collection c) {
        Iterator i = c.iterator();
        boolean changed = false;
        boolean ret;
        while(i.hasNext()) {
            ret = add(i.next());
            if(!changed) {
                changed = ret;
            }
        }
        return changed;
    }

    /**
     * Retrieves the last element in this List.
     * @exception Thrown if this List is empty.
     */
    public Object lastElement() throws NoSuchElementException {
        if(isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return get(size()-1);
    }

    /**
     * Finds the index at which o should be inserted.
     * @param o The Object that is to be inserted.
     * @return The index where Object o should be inserted.
     */
    public int findInsertionPoint(Object o) {
        return findInsertionPoint(o, 0, size()-1);
    }

    //
    // Unsupported Methods
    //

    /**
     * @exception This method not supported.
     */
    public void add(int index, Object element) {
        System.out.println("add");
        throw new UnsupportedOperationException("add(int index, Object element is not Supported");
    }

    /**
     * @exception This method not supported.
     */
    public Object set(int index, Object element) {
        throw new UnsupportedOperationException("set(int index, Object element) is not Supported");
    }
    
    /**
     * @exception This method not supported.
     */
    public boolean addAll(int index, Collection c) {        
        throw new UnsupportedOperationException("addAll(int index, Collection c) is not Supported");
    }

    //
    // Private Methods
    //

    /**
     * Compares two keys using the correct comparison method for this 
     * Collection.
     * @param k1 The first item to be compared.
     * @param k2 The second item to be compared.
     * @return a positive or negative integer if they differ, and zero if 
     *         equal.
     */
    private int compare(Object k1, Object k2) {
        return (mComparator==null ? ((Comparable)k1).compareTo(k2)
                : mComparator.compare(k1, k2));
    }

    /**
     * Conducts a binary search to find the index where Object o should
     * be inserted.
     * @param o The Object that is to be inserted.
     * @param startIndex The starting point for the search.
     * @param endIndex The end boundary for this search.
     * @return The index where Object o should be inserted.
     */
    private int findInsertionPoint(Object o, int startIndex, int endIndex) {

        int halfPt = ((endIndex - startIndex)/2) + startIndex;          
        int delta = compare(get(halfPt), o);

        if(delta < 0) {
            endIndex = halfPt;
        }
        else if(delta > 0) {
            startIndex = halfPt;
        }
        else {
            // System.out.println("halfPt: " + halfPt);
            return halfPt;
        }

        // the object in question falls between two elements
        if((endIndex - startIndex) <= 1) {
            // System.out.println("endIndex: " + endIndex);
            return endIndex+1;
        }

        return findInsertionPoint(o, startIndex, endIndex);
    }
}
