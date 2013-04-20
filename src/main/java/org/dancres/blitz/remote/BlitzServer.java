package org.dancres.blitz.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import net.jini.core.lease.Lease;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.server.TransactionParticipant;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.admin.JoinAdmin;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.lease.SpaceUID;

import com.sun.jini.start.ServiceProxyAccessor;

import com.sun.jini.admin.DestroyAdmin;

/**
   <p> We must implement ServiceProxyAccessor to ensure that our smart proxy is
   available to the appropriate jini.start descriptor. If this isn't
   implemented, ServiceStarter will end up "seeing" a null proxy and may
   assume something broke because there's no valid proxy.  i.e.  If we
   don't implement the interface, the proxy is assumed to be null, unless
   ProxyAccessor is implemented in which case the return from that will be
   used.</p>

   <p> The impl class (BlitzServiceImpl) should also implement
   net.jini.export.ProxyAccessor which is used by the activation system to
   obtain a stub remote reference (not the smart proxy) for passing to clients.
   If this interface isn't implemented it is assumed that:

   <ol>
   <li> The class implements the (ActivationID, MarshalledObject) constructor.
   </li>
   <li> The class is serializable. </li>
   <li> When serialized/marshalled, an object of this class will become a
   suitable remote reference.  This, of course, would be achieved using
   replaceObject etc. to cause replacement of the server class with it's stub.
   </li>
   </ol>

   See com.sun.jini.phoenix.ActivationGroupImpl
 */
public interface BlitzServer extends AdminServer, ServiceProxyAccessor, 
                                     TransactionParticipant,
                                     JS05Server,
                                     Landlord, BackEndSpace, Remote {
}
