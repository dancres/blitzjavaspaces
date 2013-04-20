package org.dancres.blitz.notify;

class NonblockingDispatchImpl implements DispatchTask {
    private EventQueue theQueue;
    private QueueEvent theEvent;

    NonblockingDispatchImpl(EventQueue aQueue, QueueEvent anEvent) {
        theQueue = aQueue;
        theEvent = anEvent;
    }

    public void run() {
        theQueue.dispatchImpl(this);
    }

    public QueueEvent getEvent() {
        return theEvent;
    }

    public void block() throws InterruptedException {
    }

    public void newDispatch() {
    }

    public void dispatched() {
    }

    public void enableResolve() {
    }
}
