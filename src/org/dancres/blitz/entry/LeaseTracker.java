package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.lease.ReapFilter;

/**
   <p>
   Tracks the lease expiries for all EntrySleeves of a particular type.
   </p>

   <p>
   When you are ready to process expired leases, invoke
   <code>bringOutTheDead</code> which will cause the <code>LeaseTracker</code>
   to pass one or more <code>TupleLocator<code> instances to the passed
   <code>EntryReaper</code>. The <code>EntryReaper</code> should iterate
   through each Locator instance and do the following:
   </p>

   <ol>
   <li>Load the <code>EntrySleeve</code> associated with the UID</li>
   <li>Check it has actually expired.  Note that, due to caching, the version
   of the sleeve in cache may contain an updated expiry time which hasn't yet
   been communicated to the <code>LeaseTracker</code>.</li>
   <li>If the <code>EntrySleeve</code> has expired, filter using appropriate
   <code>ReapFilter</code>s and mark the <code>EntrySleeve</code> for deletion
   assuming the filters don't boycott.</li>
   </ol>

   @see org.dancres.blitz.entry.TupleLocator
   @see org.dancres.blitz.entry.EntryReaper
 */
public interface LeaseTracker {
    /**
       Locate expired entries and pass their UIDs to the specified filter.
     */
    public void bringOutTheDead(EntryReaper aReaper) throws IOException;

    public void delete(PersistentEntry anEntry) throws IOException;
        
    public void update(PersistentEntry anEntry) throws IOException;

    public void write(PersistentEntry anEntry) throws IOException;
    
    public void close() throws IOException;

    public void delete() throws IOException;
}
