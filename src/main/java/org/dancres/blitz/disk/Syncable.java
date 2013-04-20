package org.dancres.blitz.disk;

/**
   Those involved in storage management should implement this interface and
   register with Disk.  This facilitates, checkpointing and clean shutdown
   of Blitz.
 */
public interface Syncable {
    /**
       When called, the instance should synchronize all "dirtied" state to
       disk - i.e. if no further operations were carried out and the JVM
       was stopped, recovery wouldn't be necessary because the disk contains
       an accurate copy of the memory state at the point when sync() was
       invoked.
     */
    public void sync() throws Exception;

    /**
       When called, the instance should close and release any files it is
       currently using.
     */
    public void close() throws Exception;
}
