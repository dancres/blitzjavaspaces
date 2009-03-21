package org.dancres.blitz.remote;

/**
   Simple skeleton to bring up a Blitz server and see if it has a proxy.
   Blitz, of course, will advertise itself to JINI LUS instances but that's
   okay and another means of checking things work.
 */
public class Test {
    public static void main(String anArgs[]) {
        try {
            BlitzServiceImpl myServer = new BlitzServiceImpl(anArgs, null);

            System.out.println("Stub: " + myServer.getProxy());
            System.out.println("Proxy: " + myServer.getServiceProxy());

            Object myLock = new Object();

            synchronized(myLock) {
                myLock.wait();
            }

        } catch (Exception anE) {
            System.out.println("Ooops");
            anE.printStackTrace(System.out);
        }
    }
}
