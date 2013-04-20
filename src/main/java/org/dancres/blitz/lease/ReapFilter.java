package org.dancres.blitz.lease;

/**
   <p>ReapFilter objects are used by Reapables to determine whether it's okay
   for them to cleanup one of their resources.  It's possible that some
   other entity may wish to block a cleanup under certain circumstances.</p>

   <p>ReapFilter instances are loaded from a config file at initialization
   time and <em>cannot</em> be changed after that.  These settings are
   closely associated with Blitz internals and should not be changed by an
   end-user.  They are expected to be the domain of the Blitz programming
   team only.</p>

   <p>Currently, ReapFilter instances can expect to receive objects of
   type EntrySleeve and EventGenerator</p>

   @see org.dancres.blitz.entry.EntrySleeve
   @see org.dancres.blitz.notify.EventGenerator
 */
public interface ReapFilter {
    /**
       @return <code>true</true> if this object should <em>not</em> be deleted
       by the Reapable which invoked on the filter.
     */
    public boolean filter(LeasedResource aResource);
}
