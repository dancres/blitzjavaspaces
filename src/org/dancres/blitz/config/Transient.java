package org.dancres.blitz.config;

/**
   <p>Configuring Blitz with this storage model causes it to run like a
   disk-backed cache.  On restart, all state (including join state and service
   id) will be forgotten.  The Blitz instance will appear to be empty.</p>

   <p>There is requirement to guarentee persistent state hence, in this
   configuration, Blitz does not bother logging actions to disk.</p>
 */
public class Transient implements StorageModel {
}
