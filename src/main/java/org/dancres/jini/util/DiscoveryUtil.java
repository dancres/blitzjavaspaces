package org.dancres.jini.util;

import java.lang.reflect.Field;

import java.rmi.RemoteException;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceItem;

import net.jini.core.entry.Entry;

public class DiscoveryUtil {
    public static void dumpRegistrar(ServiceRegistrar aRegistrar) 
        throws RemoteException {

        String[] myGroups = aRegistrar.getGroups();

        System.out.println("Registrar ServiceID: " +
                           aRegistrar.getServiceID());
        System.out.print("Groups: ");
        for (int i = 0; i < myGroups.length; i++) {
            System.out.print(myGroups[i] + " ");
        }
        System.out.println();
        System.out.println("LookupLocator: " + aRegistrar.getLocator());
    }

    public static void dumpContents(ServiceRegistrar aRegistrar) 
        throws RemoteException {

        ServiceTemplate myWildcard = new ServiceTemplate(null, null, null);

        ServiceMatches myMatches = aRegistrar.lookup(myWildcard,
                                                     Integer.MAX_VALUE);

        System.out.println("Total services: " + myMatches.totalMatches);

        for (int i = 0; i < myMatches.totalMatches; i++) {
            ServiceItem myItem = myMatches.items[i];
            Object myProxy = myItem.service;

            System.out.println("ServiceId: " + myItem.serviceID);
            System.out.println("Type: " + myProxy.getClass());
            System.out.print("Interfaces: ");
            dumpInterfaces(myProxy);

            System.out.println("Total attributes: " +
                               myItem.attributeSets.length);

            dump(myItem.attributeSets);

            System.out.println();
        }
    }

    public static ServiceMatches
        findServicesOfType(Class aClass,
                           ServiceRegistrar aRegistrar) 
        throws RemoteException {

        ServiceTemplate myTemplate =
            new ServiceTemplate(null, new Class[] {aClass}, null);

        ServiceMatches myServices = aRegistrar.lookup(myTemplate, 255);

        return myServices;
    }

    public static void dump(Entry[] aListOfEntries) {
        for (int i = 0; i < aListOfEntries.length; i++) {
            System.out.println("  " + aListOfEntries[i].getClass().getName());
            dump(aListOfEntries[i]);
        }
    }

    public static void dump(Entry anEntry) {
        Field[] myFields = anEntry.getClass().getFields();

        for (int i = 0; i < myFields.length; i++) {

            try {
                Object myValue = myFields[i].get(anEntry);

                System.out.println(myFields[i].getType().getName() + " " +
                                   myFields[i].getName() + " = " + myValue);
            } catch (IllegalAccessException anE) {
                System.out.println(myFields[i].getType().getName() + " " +
                                   myFields[i].getName() + " = IllegalAccessException");
            }
        }
    }

    public static void dump(ServiceMatches aMatches) {
        System.out.println("Found " + aMatches.totalMatches);

        for (int i = 0; i < aMatches.totalMatches; i++) {
            System.out.println("ServiceID: " +
                               aMatches.items[i].serviceID + ", " +
                               aMatches.items[i].service);
            System.out.print("Interfaces: ");
            dumpInterfaces(aMatches.items[i].service.getClass());
        }
    }

    public static void dump(ServiceItem anItem) {
            System.out.println("ServiceID: " +
                               anItem.serviceID + ", " + anItem.service);
            System.out.print("Interfaces: ");
            dumpInterfaces(anItem.service.getClass());
    }

    public static boolean hasInterface(Object anObject, Class anInterface) {

        Class myCurrentClass = anObject.getClass();

        while (myCurrentClass != null) {
            Class[] myInterfaces = myCurrentClass.getInterfaces();

            for (int j = 0; j < myInterfaces.length; j++) {
                if (myInterfaces[j].equals(anInterface))
                    return true;
            }

            myCurrentClass = myCurrentClass.getSuperclass();
        }

        return false;
    }

    public static void dumpInterfaces(Object anObject) {
        dumpInterfaces(anObject.getClass());
    }

    public static void dumpInterfaces(Class aClass) {

        Class[] myInterfaces = aClass.getInterfaces();
            
        for (int j = 0; j < myInterfaces.length; j++) {
            System.out.print(myInterfaces[j].getName() +
                    "(" + myInterfaces[j].getClassLoader() + ")" + " ");
        }

        if (aClass.getSuperclass() != null)
            dumpInterfaces(aClass.getSuperclass());
        else
            System.out.println();
    }
}
