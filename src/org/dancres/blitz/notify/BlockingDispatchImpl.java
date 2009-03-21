package org.dancres.blitz.notify;

class BlockingDispatchImpl implements DispatchTask {
    private EventQueue theQueue;
    private QueueEvent theEvent;

    private boolean isDone = false;
    private boolean isResolvable = false;
    private int myTotalDispatches = 0;
    private int myCompletedDispatches = 0;

    BlockingDispatchImpl(EventQueue aQueue, QueueEvent anEvent) {
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
        synchronized(this) {
            while (! isDone) {
                try {
                    wait();
                } catch (InterruptedException anIE) {
                }
            }
        }
    }

    public void newDispatch() {
        synchronized(this) {
            ++myTotalDispatches;
        }
    }

    public void dispatched() {
        synchronized(this) {
            ++myCompletedDispatches;
        }

        checkAndFire();
    }

    public void enableResolve() {
        synchronized(this) {
            isResolvable = true;
        }

        checkAndFire();
    }

    private void checkAndFire() {
        synchronized(this) {
            if (!isResolvable)
                return;
            
            if (myTotalDispatches == myCompletedDispatches) {
                isDone = true;
                notify();
            }
        }
    }
}
