package org.dancres.blitz.entry;

/**
   Counters are updated via this interface

   @see org.dancres.blitz.entry.CountersImpl
 */
public interface Counters {
    public void didRead();
    public void didTake();
    public void didWrite();

    /**
       Indicates a lease-expired entry was cleaned up
     */
    public void didPurge();

    public int getInstanceCount();
}
