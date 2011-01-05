package org.dancres.blitz.notify;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BlockingDispatchImpl implements DispatchTask {
    private EventQueue theQueue;
    private QueueEvent theEvent;

    private Lock myLock = new ReentrantLock();
    private Condition myDone = myLock.newCondition();
    private boolean isDone = false;
    private AtomicBoolean isResolvable = new AtomicBoolean(false);
    private AtomicInteger myRemainingDispatches = new AtomicInteger(0);

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
        myLock.lock();
        try {
            while (!isDone)
                myDone.await();
        } finally {
            myLock.unlock();
        }
    }

    public void newDispatch() {
        myRemainingDispatches.incrementAndGet();
    }

    public void dispatched() {
        myRemainingDispatches.decrementAndGet();

        checkAndFire();
    }

    public void enableResolve() {
        isResolvable.set(true);

        checkAndFire();
    }

    private void checkAndFire() {
        if (!isResolvable.get())
            return;

        if (myRemainingDispatches.get() == 0) {
            myLock.lock();
            try {
                isDone = true;
                myDone.signal();
            } finally {
                myLock.unlock();
            }
        }
    }
}
