package org.dancres.blitz.remote;

import java.io.IOException;

import java.rmi.Remote;
import java.rmi.RemoteException;

import net.jini.core.transaction.TransactionException;

/**
   Blitz specific admin operations are available on this interface
 */
public interface BlitzAdmin extends Remote {
    /**
       <p>Run a hot backup - whilst <code>requestSnapshot</code> requires one
       to suspend access to the Blitz instance and simply checkpoints to the
       database directory, this method can be used to produce a separate
       snapshot (in another directory) whilst activity continues.  The output
       is suitable for restoration under circumstances of failure.</p>

       <p>Note, an Entry's lease may expire before the backup is restored
       in which case it is deemed to have been remove from the space thus
       archives created with this method will only restore state that is
       not yet expired.</p>

       <p>Note also that transactions active during the backup will
       <em>not</em> be reflected in the backup.  This is a necessity as
       otherwise it would be impossible to finish the backup under some
       circumstances.</p>

       <p>This method may supercede <code>requestSnapshot</code> some time
       in the future.</p>

       <p><b>WARNING: Experimental at this stage</b></p>

       @param aBackupDir is the directory in which to deposit the backup.
       Ensure that the directory is empty before invoking this method.
       Note that the directory will be created if it doesn't already exist.
     */
    public void backup(String aBackupDir) throws RemoteException, IOException;

    /**
       <p>Blocks the caller, issues a checkpoint and returns on completion.
       Note, the caller should ensure that there's no current activity within
       the Blitz instance such as active transactions.  If Blitz is active,
       the checkpoint will not contain *any* of the actions of these
       transactions.  Prepared (but not commited or aborted) transactions are
       also problematic as they will be saved in the checkpoint but cannot,
       for example, be restored to another Blitz instance and commited there
       (unless you've used Activatable references in a suitable failover
       configuration).</p>

       @throws IOException if the checkpoint failed.
       @throws TransactionException if there are active/prepared transactions
       which cannot be guarenteed to be recovered from the saved snapshot at
       restore time.
     */
    public void requestSnapshot() throws RemoteException, 
                                         TransactionException, IOException;

    /**
       Causes Blitz to shutdown cleanly whilst retaining state.  This can
       also be achieved by calling DestroyAdmin::destroy() so long as the
       configuration variable <code>compatDestroy</code> is set to <code>
       false</code>.
     */
    public void shutdown() throws RemoteException, IOException;

    /**
       Clean up all Entry's, abort outstanding matches etc so the JavaSpace
       is good as new.
     */
    public void clean() throws RemoteException, IOException;

    /**
       Request a manual reap of lease expired Entry's.  Note,
       <code>LeaseReaper.MANUAL_REAP</code> must be set for the relevant
       lease reap time variable in the configuration file.
     */
    public void reap() throws RemoteException;
}
