package org.dancres.blitz.entry;

/**
   Each Sleeve held in-cache has a chunk of management state associated with it
   which is maintained in an instance of this class
 */
class SleeveState {
    /**
       If set, indicates this Sleeve does not exist on disk.
       i.e.  If it needs to be written to disk it should be inserted rather
       than updated (create as opposed to update).
     */
    static final int NOT_ON_DISK = 1;

    /**
       Indicates this Sleeve has been deleted and the space can be reclaimed.
     */
    static final int DELETED = 2;

    /**
       If set, indicates that this Sleeve should not be saved to disk

       @see org.dancres.blitz.entry.WriteScheduler
     */
    static final int PINNED = 4;

    private int theFlags;

    /**
       Applies the specified state cummulatively to the current state flags.
       i.e. Logical OR
     */
    synchronized void set(int aFlags) {
        theFlags |= aFlags;
    }

    /**
       Replace existing state with that passed in.
     */
    synchronized void setExplicit(int aFlags) {
        theFlags = aFlags;
    }

    synchronized boolean test(int aFlag) {
        return ((theFlags & aFlag) != 0);
    }

    synchronized void clear(int aFlag) {
        int myMask = aFlag ^ 0xFFFFFFFF;
        theFlags &= myMask;
    }

    synchronized int get() {
        return theFlags;
    }

    public synchronized String toString() {
        return  Integer.toBinaryString(theFlags);
    }
}
