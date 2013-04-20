package org.dancres.blitz.remote.view;

import java.io.IOException;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseHandler;
import org.dancres.blitz.lease.LeaseBounds;

import org.dancres.blitz.util.Time;

public class ViewLeaseHandlerImpl implements LeaseHandler {
    public boolean recognizes(SpaceUID aUID) {
        return (aUID instanceof EntryViewUID);
    }

    public long renew(SpaceUID aUID, long aLeaseDuration)
        throws UnknownLeaseException, LeaseDeniedException, IOException {

        long myDuration = LeaseBounds.boundView(aLeaseDuration);
        long myExpiry = Time.getAbsoluteTime(myDuration);

        boolean myResult;

        myResult = EntryViewFactory.get().renew((EntryViewUID) aUID,
                                                myExpiry);

        if (!myResult)
            throw new UnknownLeaseException();

        return myDuration;
    }

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, IOException {

        boolean myResult;

        myResult = EntryViewFactory.get().cancel((EntryViewUID) aUID);

        if (!myResult)
            throw new UnknownLeaseException();
    }
}
