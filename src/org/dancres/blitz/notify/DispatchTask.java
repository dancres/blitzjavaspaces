package org.dancres.blitz.notify;

/**
 * DispatchTask represents an event processing task for the event queue.
 * The lifecycle of this event is as follows:
 *
 * <ol>
 * <li>The poster of the event invokes block() having enqueued this task.  It may or may
 * not actually block depending on the task's implementation.</li>
 *
 * <li>For each event generated, newDispatch is invoked to indicate an event
 * will be posted.</li>
 *
 * <li>For each event generated, dispatched is invoked to indicate the event has
 * been sent.</li>
 *
 * <li>When the event queue (as to the event senders) has finished generating events it
 * invokes enableResolve which for suitable implementations indicates that the blocker
 * can be released at the appropriate moment. enableResolve will only be called after <em>all</em>
 * requests to newDispatch have been completed.</li>
 * </ol>
 */
public interface DispatchTask extends Runnable {
    public QueueEvent getEvent();
    public void block() throws InterruptedException;
    public void newDispatch();
    public void dispatched();
    public void enableResolve();
}
