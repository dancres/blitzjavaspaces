package org.prevayler;

import java.io.Serializable;

/**
   Instances of this interface provide additional state to be saved in the
   snapshot record generated at checkpoint.
 */
public interface SnapshotContributor {
    public Serializable getContribution();
}