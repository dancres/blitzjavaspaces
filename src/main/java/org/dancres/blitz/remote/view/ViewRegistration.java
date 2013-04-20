package org.dancres.blitz.remote.view;

/**
   The result of a call to EntryViewFactory.newView includes the assigned id
   and the initial lease expiry.
 */
public class ViewRegistration {
    private EntryViewUID theUID;
    private long theLeaseExpiry;

    ViewRegistration(EntryViewUID aUID, long aExpiry) {
        theUID = aUID;
        theLeaseExpiry = aExpiry;
    }

    public EntryViewUID getUID() {
        return theUID;
    }

    public long getExpiry() {
        return theLeaseExpiry;
    }
}