package org.dancres.blitz;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
   Various components within Blitz require access to certain specific bits
   of information concerning Boot state.  BootContext is responsible for
   holding all these bits of information and making them available to the
   components.
 */
public class BootContext {
    private static List theContext = new LinkedList();

    private static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            synchronized(theContext) {
                theContext.clear();
            }
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }

    public static void add(BootInfo anInfo) {
        synchronized(theContext) {
            theContext.add(anInfo);
        }
    }

    public static BootInfo get(Class aClass) {
        validate(aClass);

        synchronized(theContext) {
            Iterator myInfos = theContext.iterator();

            while (myInfos.hasNext()) {
                BootInfo myInfo = (BootInfo) myInfos.next();

                if (aClass.isInstance(myInfo))
                    return myInfo;
            }
        }

        return null;
    }

    private static void validate(Class aClass) {
        Class[] myInterfaces = aClass.getInterfaces();

        for (int i = 0 ; i < myInterfaces.length; i++) {
            if (myInterfaces[i].equals(BootInfo.class))
                return;
        }

        throw new RuntimeException("Attempt to store object which is not a BootInfo");
    }
}
