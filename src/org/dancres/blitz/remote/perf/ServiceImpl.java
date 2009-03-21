package org.dancres.blitz.remote.perf;

import java.io.IOException;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;

import java.rmi.activation.ActivationID;
import java.rmi.activation.ActivationException;
import java.rmi.activation.ActivationSystem;
import java.rmi.activation.Activatable;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

import net.jini.config.ConfigurationException;
import net.jini.config.Configuration;

import net.jini.export.ProxyAccessor;
import net.jini.export.Exporter;

import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;

import net.jini.jeri.tcp.TcpServerEndpoint;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.TransactionException;

import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import net.jini.core.entry.Entry;

import com.sun.jini.start.LifeCycle;

import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.notify.EventGenerator;

import org.dancres.blitz.Logging;

/**
 */
public class ServiceImpl implements Server {
    static final String STUB_FILE = "stub.ser";

    static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote.perf", Level.INFO);

    private static ServiceImpl theServiceImpl;

    private Exporter theExporter;

    private Server theStub;

    private boolean isStopped = false;

    private Uuid theUuid = UuidFactory.generate();

    private long theFakeExpiry = System.currentTimeMillis();

    private MangledEntry theEntry;

    public static void main(String anArgs[]) {

        try {
            theServiceImpl = new ServiceImpl(anArgs, null);
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Init failed", anE);
        }
    }

    /**
       This constructor is required to use
       com.sun.jini.start.NonActivatableServiceDescriptor.
     */
	public ServiceImpl(String [] anArgs, LifeCycle aLifeCycle)
		throws RemoteException, ConfigurationException {

        try {
            init(anArgs);
        } catch (ConfigurationException aCE) {
            throw aCE;
        } catch (RemoteException anRE) {
            throw anRE;
        }

        EntryMangler myMangler = new EntryMangler();

        theEntry = myMangler.mangle(new DummyEntry("rhubarb"));

        /*
           Keep a static reference to ourselves in order to avoid premature
           GC
        */
        theServiceImpl = this;
	}

    /**
       This constructor is required to use
       com.sun.jini.start.SharedActivatableServiceDescriptor which invokes
       on ActivateWrapper which will call this constructor.
       SharedActivatabkeServiceDescriptor's serverConfigArgs are wrapped
       into a MarshalledObject and pass in with the ActivationID.
     */
	public ServiceImpl(ActivationID anActivationID,
                       MarshalledObject aData)
        throws RemoteException, ConfigurationException {

        String[] myArgs = null;

        try {
            myArgs = (String[]) aData.get();
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to unpacked marshalled args",
                          anE);
            throw new RemoteException("Failed to unpack marshalled args");
        }

        try {
            init(myArgs);
        } catch (ConfigurationException aCE) {
            throw aCE;
        } catch (RemoteException anRE) {
            throw anRE;
        }
    }

    private void init(String[] anArgs) throws ConfigurationException,
                                              RemoteException {
        
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        if ((anArgs != null) && (anArgs.length > 0)) {
            ConfigurationFactory.setup(anArgs);
        }

        try {
            doPriv(new PrivilegedInitImpl());
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
            theLogger.log(Level.SEVERE, "Oops privilege problem?", anE);
            throw new ConfigurationException("loginContext has insufficient privileges", anE);
        }
    }

    private Object doPriv(PrivilegedExceptionAction aPAE)
        throws Exception {

        return aPAE.run();
    }

    private class PrivilegedInitImpl implements PrivilegedExceptionAction {
        public Object run() throws Exception {
            try {
                initImpl();
            } catch (Exception anE) {
                theLogger.log(Level.SEVERE, "Could init with subject", anE);
                throw anE;
            }
            return null;
        }
    }

    private void initImpl() throws ConfigurationException, RemoteException {
        Exporter myDefaultExporter = 
            new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                  new BasicILFactory(), false, true);

        ConfigurationFactory.setup(new String[] {"config/nettest.config"});

        theExporter =
            (Exporter) ConfigurationFactory.getEntry("serverExporter",
                                                     Exporter.class,
                                                     myDefaultExporter);
        if (theExporter == null) {
            throw new ConfigurationException("serverExporter must be non-null if it's specified");
        }

        System.err.println("Using exporter: " + theExporter);

        theStub = (Server) theExporter.export(this);     

        Class[] myInterfaces = theStub.getClass().getInterfaces();

        for (int i = 0; i < myInterfaces.length; i++) {
            System.err.println("Stub supports: " + myInterfaces[i].getName());
        }

        try {
            File myFlattenedStub = new File(STUB_FILE);

            ObjectOutputStream myOOS =
                new ObjectOutputStream(new FileOutputStream(myFlattenedStub));

            myOOS.writeObject(theStub);

            myOOS.close();
        } catch (IOException anIOE) {
            throw new RemoteException("Oops, didn't save stub", anIOE);
        }

        System.err.println("Stub saved in " + STUB_FILE + " - start client with it");
    }

    public Lease write(MangledEntry anEntry, Transaction aTxn,
                          long aLeaseTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        return
            ProxyFactory.newLeaseImpl(theStub, theUuid,
                                      new FakeUID(),
                                      theFakeExpiry);
    }

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException {

        stopBarrier();

        return theEntry;
    }

    /**
       Used to check whether calls are blocked and, if they are, throws
       a RemoteException back to the client.
     */
    private void stopBarrier() throws RemoteException {
        synchronized(this) {
            if (isStopped)
                throw new RemoteException("Space has been stopped");
        }
    }

    public long renew(SpaceUID aUID, long aDuration)
        throws UnknownLeaseException, LeaseDeniedException, RemoteException {
        
        // System.out.println("Renew: " + aUID + ", " + aDuration);

        stopBarrier();

        throw new org.dancres.util.NotImplementedException();
    }

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, RemoteException {
        
        stopBarrier();


        throw new org.dancres.util.NotImplementedException();
    }
    
    public LeaseResults renew(SpaceUID[] aLeases, long[] aDurations)
        throws RemoteException {

        stopBarrier();

        throw new org.dancres.util.NotImplementedException();
    }

    public LeaseResults cancel(SpaceUID[] aLeases)
        throws RemoteException {

        stopBarrier();

        throw new org.dancres.util.NotImplementedException();
    }

    private Exception[] initFails(int aLength, int anOffset) {
        return new Exception[aLength];
    }
}
