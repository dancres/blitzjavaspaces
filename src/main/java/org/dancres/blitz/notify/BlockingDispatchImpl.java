package org.dancres.blitz.notify;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BlockingDispatchImpl implements DispatchTask {
    private EventQueue _queue;
    private QueueEvent _event;

    private Lock _lock = new ReentrantLock();
    private Condition _condition = _lock.newCondition();
    private boolean _done = false;
    private AtomicBoolean _resolvable = new AtomicBoolean(false);
    private AtomicInteger _remainingDispatches = new AtomicInteger(0);

    BlockingDispatchImpl(EventQueue aQueue, QueueEvent anEvent) {
        _queue = aQueue;
        _event = anEvent;
    }

    public void run() {
        _queue.dispatchImpl(this);
    }

    public QueueEvent getEvent() {
        return _event;
    }

    public void block() throws InterruptedException {
        _lock.lock();
        try {
            while (!_done)
                _condition.await();
        } finally {
            _lock.unlock();
        }
    }

    public void newDispatch() {
        _remainingDispatches.incrementAndGet();
    }

    public void dispatched() {
        _remainingDispatches.decrementAndGet();

        checkAndFire();
    }

    public void enableResolve() {
        _resolvable.set(true);

        checkAndFire();
    }

    private void checkAndFire() {
        if (!_resolvable.get())
            return;

        if (_remainingDispatches.get() == 0) {
            _lock.lock();
            try {
                _done = true;
                _condition.signal();
            } finally {
                _lock.unlock();
            }
        }
    }
}
