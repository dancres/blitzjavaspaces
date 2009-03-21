package org.dancres.blitz.config;

/**
   <p>Implementations of this interface are used to configure the storage model
   that Blitz uses such as caching/transient, persistent, generation
   persistent.</p>

   <p>Each implementation requires different parameters and a different
   internal Blitz configuration.  The work of translating the specified
   <code>StorageModel</code> instance into an appropriate runtime configuration
   is done by a <code>StoragePersonality</code> instance.</p>

   @see org.dancres.blitz.txn.StoragePersonality
*/

public interface StorageModel {
}
