package org.dancres.blitz.meta;

import java.io.IOException;

import org.dancres.blitz.disk.DiskTxn;

/**
   <p><code>Registry</code> is a container for a related set of meta data.</p>

   <p>Various pieces of ad-hoc information need to be retained within Blitz.
   Chances are that some of this information belongs in a logical group hence
   we allow for creation of more than one <code>Registry</code> each of which is identified
   by a unique name. <P>

   <p>Just like all the other storage within Blitz, the content of <code>Registry</code>
   instances has to be kept sync'd (the user may also generate log information
   depending on whether they choose to in-line update or log and sync). Thus,
   most users of <code>Registry</code> will implement <code>Syncable</code>.
   </p>

   <p>All modifications to <code>Registry</code> content are achieved through
   a <code>RegistryAccessor</code>.

   @see org.dancres.blitz.disk.Syncable
   @see org.dancres.blitz.meta.RegistryAccessor
 */
public interface Registry {
    /**
       Obtain an instance of an accessor which will read or make modifications
       in the current context transaction.
     */
    public RegistryAccessor getAccessor() throws IOException;

    /**
       Obtain an instance of an accessor which will read or make modifications
       in the specified transaction.context.
     */
    public RegistryAccessor getAccessor(DiskTxn aTxn) throws IOException;

    public void close() throws IOException;
}
