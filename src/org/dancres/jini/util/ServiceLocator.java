package org.dancres.jini.util;

import net.jini.lookup.entry.Name;
import net.jini.core.entry.Entry;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.LookupDiscovery;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

/** 
 * ServiceLoactor is a simple wrapper class over Jini's LookupDiscover.
 * It which returns the first matching instance of a service either via
 * unicast or multicast discovery
 */

public class ServiceLocator{

    static {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    private Object _proxy;
    private Object _lock=new Object();
    private ServiceTemplate _template;

    /**
     *  Locates a service via Unicast discovery
     * @param lusHost The name of the host where a Jini lookup service is running
     * @param serviceClass The class object representing the interface of the service
     * @throws MalformedURLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @return The proxy to the discovered service  
     */    
    public static Object getService(String lusHost,Class serviceClass)
        throws java.net.MalformedURLException,java.io.IOException,ClassNotFoundException{
    
        LookupLocator loc=new LookupLocator("jini://"+lusHost);
        ServiceRegistrar reggie=loc.getRegistrar();
        ServiceTemplate tmpl=new ServiceTemplate(null, new Class[]{serviceClass},null);
        return reggie.lookup(tmpl);
        
    }
    /**
     *
     * @param lusHost
     * @param serviceClass
     * @param serviceName
     * @return proxy or <code>null</code>
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    public static Object getService(String lusHost,Class serviceClass,
                                    String serviceName)
        throws java.net.MalformedURLException,java.io.IOException,
               ClassNotFoundException{

        Class [] types=new Class[]{serviceClass};
        Entry [] entry=null;

        if(serviceName!=null){
            entry=new Entry[]{new Name(serviceName)};
        }

        ServiceTemplate _template = new ServiceTemplate(null,types,entry);
        LookupLocator loc=new LookupLocator("jini://"+lusHost);
        ServiceRegistrar reggie=loc.getRegistrar();

        return reggie.lookup(_template);
    } 
    /**
     * Locates the first matching service via multicast discovery
     * @param serviceClass The class object representing the interface of the service
     * @throws IOException
     * @throws InterruptedException
     * @return  */    
    public static Object getService(Class serviceClass)
        throws java.io.IOException,InterruptedException{
            
        return getService(serviceClass,null,Long.MAX_VALUE);    
    }
    /**
     * Locates the first matching service via multicast discovery
     * @param serviceClass The class object representing the interface of the service
     * @param waitTime How to wait for the service to be discovered
     * @throws IOException
     * @throws InterruptedException
     * @return  */    
    public static Object getService(Class serviceClass,long waitTime)
        throws java.io.IOException,InterruptedException{
            
        return getService(serviceClass,null,waitTime);    
    }
    /**
     * Locates the first matching service via multicast discovery
     * @param serviceClass The class object representing the interface of the service
     * @param serviceName The Name attribute of the service
     * @throws IOException
     * @throws InterruptedException
     * @return  */    
    public static Object getService(Class serviceClass,String serviceName,long waitTime)
        throws java.io.IOException,InterruptedException{
    
        ServiceLocator sl=new ServiceLocator();
        return sl.getServiceImpl(serviceClass,serviceName,waitTime);
    }

   
    private Object getServiceImpl(Class serviceClass,String serviceName,long waitTime)
        throws java.io.IOException,InterruptedException{
        
                
         Class [] types=new Class[]{serviceClass};        
         Entry [] entry=null;
         
         if(serviceName!=null){
            entry=new Entry[]{new Name(serviceName)};
        }

         _template=new ServiceTemplate(null,types,entry);

        LookupDiscovery disco=
            new LookupDiscovery(LookupDiscovery.ALL_GROUPS);

        disco.addDiscoveryListener(new Listener());
        
                
        
       synchronized(_lock){
            _lock.wait(waitTime);               
       }
       
		disco.terminate();
		if(_proxy==null){
			throw new InterruptedException("Service not found within wait time");
		}
		return _proxy;
        
    }
     class Listener implements DiscoveryListener {
        //invoked when a LUS is discovered       
        public void discovered(DiscoveryEvent ev) {
            ServiceRegistrar[] reg = ev.getRegistrars();
            for (int i=0 ; i<reg.length && _proxy==null ; i++) {
                findService(reg[i]);
            }
        }
             
        public void discarded(DiscoveryEvent ev) {
        }
    }
     private void findService(ServiceRegistrar lus) {
        
        try {
            synchronized(_lock){               
                _proxy=lus.lookup(_template);
                if(_proxy!=null){                 
                    _lock.notifyAll();
                }
            }
        }catch(RemoteException ex) {           
            ex.printStackTrace(System.err);
        }
    }
}
