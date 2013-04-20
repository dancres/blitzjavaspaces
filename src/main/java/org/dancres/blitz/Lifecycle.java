package org.dancres.blitz;

/**
 * Lifecycle implementations are invoked during startup and post shutdown of Blitz core. Any implementation registered
 * after startup and before shutdown will be <code>init</code>'d immediately.
 */
public interface Lifecycle {
    /**
     * Called to set up initial state prior to first use
     */
    public void init();

    /**
     * Called once Blitz has stopped all threads and ceased processing.
     */
    public void deinit();
}
