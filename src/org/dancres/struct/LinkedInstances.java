package org.dancres.struct;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
   In most general situations one might store a number of instances in a
   java.util.LinkedList or similar but this has an overhead as we create
   a holder object for each instance we add - i.e. we're delegating the
   management of being in the list to this holder object.  This has two
   disadvantages:

   <OL>
   <LI> Memory overhead - we have two objects per object we wish to hold. </LI>
   <LI> Lack of direct addressability - having obtained a reference to an
   object we might like to use that directly to access members of the list.
   </LI>
   </OL>

   User is expected to provide suitable multi-threaded protection.
 */
public class LinkedInstances implements Serializable {
    private LinkedInstance theHead;
    private LinkedInstance theTail;

    private int theTotalInstances = 0;

    public LinkedInstances() {
    }

    public LinkedInstance getHead() {
        return theHead;
    }

    public LinkedInstance getTail() {
        return theTail;
    }

    public List copy() {
        ArrayList myMembers = new ArrayList();

        LinkedInstance myInstance = theHead;
        while (myInstance != null) {
            myMembers.add(myInstance);

            myInstance = myInstance.getNext();
        }

        return myMembers;
    }

    public void insert(LinkedInstance aLink) {
        aLink.setNext(null);
        aLink.setPrev(null);

        if (theHead == null) {
            theHead = theTail = aLink;
        } else {
            theHead.setPrev(aLink);
            aLink.setNext(theHead);
            theHead = aLink;
        }

        ++theTotalInstances;
    }

    public void add(LinkedInstance aLink) {
        aLink.setNext(null);
        aLink.setPrev(null);

        if (theHead == null) {
            theHead = theTail = aLink;
        } else {
            theTail.setNext(aLink);
            aLink.setPrev(theTail);
            theTail = aLink;
        }

        ++theTotalInstances;
    }

    public void remove(LinkedInstance aLink) {
        LinkedInstance myPrev = aLink.getPrev();
        LinkedInstance myNext = aLink.getNext();

        if (myPrev != null)
            myPrev.setNext(myNext);
        else
            theHead = myNext;

        if (myNext != null)
            myNext.setPrev(myPrev);
        else
            theTail = myPrev;

        --theTotalInstances;

        aLink.setPrev(null);
        aLink.setNext(null);
    }

    public LinkedInstance removeLast() {
        LinkedInstance myLast = theTail;
        theTail = myLast.getPrev();

        if (theTail != null)
            theTail.setNext(null);
        else
            theHead = null;

        myLast.setPrev(null);
        myLast.setNext(null);

        --theTotalInstances;

        return myLast;
    }

    public int getSize() {
        return theTotalInstances;
    }
}
