package org.dancres.blitz.lease;

/**
   Objects which maintain leased resources should implement this interface
   and register with LeaseReaper such that when LeaseReaper is activated by
   configuration, the implementing objects will be invoked upon after the
   appropriate time periods.
 */
public interface Reapable {
    /**
       Before deleting a resource, the Reapable should first offer the
       resource to the passed filter.
     */
    public void reap(ReapFilter aFilter);
}
