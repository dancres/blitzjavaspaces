package org.dancres.blitz.config;

/**
   <p>Fifo constraint causes searching of Entry's to be done in approximately
   oldest first order.</p>

   <p>FIFO ordering is, essentially, cache defeating in that it
   demands loading the <em>least</em> recently used Entry's rather than
   favouring <em>most recently</em> used Entry's.  Therefore, enabling
   FIFO will reduce overall performance of Blitz which may be countered
   by assumptions you can make in your JavaSpaces code</p>

   <p>Note that FIFO is <em>not</em> a requirement of the JavaSpaces spec and
   you cannot assume all JavaSpaces implementations provide FIFO support.
   Thus, by using the Blitz FIFO facility, you are potentially locking
   yourself in.</p>

   <p>Note that the number of pending dirty writes can significantly affect
   FIFO performance.  Normally, one configures Blitz with a large pending
   writes buffer to reduce IO and increase performance.  When FIFO is enabled,
   large write buffers introduce significant amounts of additional processing
   thus you are advised to reduce the size of <code>desiredPendingWrites</code>
   to of the order of <code>100</code>.</p>
 */
public class Fifo implements EntryConstraint {
    private static final String TYPE = "FIFO";

    public Fifo() {
    }

    public int hashCode() {
        return TYPE.hashCode();
    }

    public boolean equals(Object anObject) {
        return (anObject instanceof Fifo);
    }
}