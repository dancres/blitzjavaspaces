package org.dancres.util;

import java.util.*;

public class DumpProps {
    public static void main(String args[]) {
        Properties myProps = System.getProperties();

        Enumeration myPropKeys = myProps.propertyNames();

        while (myPropKeys.hasMoreElements()) {
            String myKey = (String) myPropKeys.nextElement();

            System.out.println(myKey + ":");
            System.out.println(myProps.get(myKey));
            System.out.println("----------");
        }
    }
}
