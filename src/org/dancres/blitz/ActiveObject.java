package org.dancres.blitz;

/**
   Various things in the space implementation require the use of threads
   which need to be started up and shutdown in a co-ordinated fashion.
   Such things should implement this interface and register with
   ActiveObjectRegistry.
*/
public interface ActiveObject {
    /**
       Instructs this instance to startup/activate threads
     */
    public void begin();

    /**
       Instructs this instance to stop/de-activate threads
     */
    public void halt();
}
