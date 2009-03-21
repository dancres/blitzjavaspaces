package org.dancres.blitz.remote;

import java.rmi.Remote;

import com.sun.jini.admin.DestroyAdmin;

import net.jini.admin.JoinAdmin;

/**
   All Admin interfaces are grouped under this interface to make
   differentiation between the various remote aspects easier.
   Note that although we implement destroyadmin we only shutdown - we do
   not delete persistent storage.  This allows for remote shutdown which, IMHO,
   should be a separate management method.  If one wishes to actually destroy
   a Blitz instance, one could call this destroy method and then delete the
   contents of the persistence and log directories as specified in the
   config file.
 */
public interface AdminServer extends Remote, JoinAdmin, StatsAdmin, 
                                     BlitzAdmin, DestroyAdmin, EntryViewAdmin {
}
