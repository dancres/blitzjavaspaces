package org.dancres.blitz.remote.test;

import java.io.Serializable;

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.EventRegistration;

import net.jini.core.lease.Lease;

import net.jini.core.entry.Entry;

import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

public class SequenceNumberTest {
    public static void main(String [] args) {
        try {
            LocalSpace mySpace = new LocalSpace();

            JavaSpace space = mySpace.getProxy();
              
            //Create a tmpl for the event registration
            ServiceError tmpl=new ServiceError();
            
            EventRegistration er=space.notify(tmpl, null, new EventListener(), Lease.ANY, null);
            //now write some ServiceErrors to the space
            
            ServiceError error=new ServiceError("MyService", "Out of memory error");
            for(int i=0;i<3;i++) {
                space.write(error, null, Lease.FOREVER);
            } 
            
            //sleep tp allow remote events to get processed
            Thread.currentThread().sleep(5000);

            mySpace.stop();

            System.exit(0);
        
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static final class EventListener
        implements RemoteEventListener, java.io.Serializable {
        public void notify(RemoteEvent anEvent) throws RemoteException {
            System.out.println("Ping!");
        }
    }

    public static final class ServiceError 
        implements net.jini.core.entry.Entry {
    
        public String _serviceName;
        public String _occuredAt;
        public String _errMsg;
        
        public ServiceError() {
        }

        public ServiceError(String src, String msg) {
            _serviceName=src;
            _errMsg=msg;
            _occuredAt=new java.util.Date().toString();
        }
    }
}
