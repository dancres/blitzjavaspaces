package org.dancres.blitz.txn.batch;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;
import org.prevayler.implementation.PrevaylerCore;
import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.Snapshotter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimisticBatcher implements SnapshotPrevayler {
    private PrevaylerCore _prevayler;
    private boolean _writing = false;
    private ArrayList<WriteRequest> _writes = new ArrayList<WriteRequest>();

    // private AtomicInteger _inCount = new AtomicInteger();
    // private AtomicInteger _outCount = new AtomicInteger();

    public OptimisticBatcher(PrevaylerCore aPrevayler) {
        _prevayler = aPrevayler;
    }

    public PrevalentSystem system() {
        return _prevayler.system();
    }

    public Serializable executeCommand(Command aCommand) throws Exception {
        return write(aCommand, true);
    }

    public Serializable executeCommand(Command aCommand, boolean sync)
        throws Exception {
        return write(aCommand, sync);
    }

    private Serializable write(Command aComm, boolean sync) throws Exception {
        boolean someoneWriting = false;
        WriteRequest myReq = null;

        synchronized(this) {
            someoneWriting = _writing;

            // If someone is already writing, we add to their queue of work
            //
            if (_writing) {
                myReq = new WriteRequest(aComm);
                _writes.add(myReq);
            } else {
                _writing = true;
            }
        }

        // If we are waiting on someone's queue
        //
        if (someoneWriting) {
            // If we want to wait until the log is flushed
            //
            if (sync)
                myReq.await();

            return aComm.execute(_prevayler.system());
        } else {
            // We are handling the write queue, write our stuff now
            //
            _prevayler.logCommand(aComm, false);

            ArrayList<WriteRequest> myAllWrites = new ArrayList<WriteRequest>();

            // While there other writes scoop them up and write them
            //
            ArrayList<WriteRequest> myBuffer = new ArrayList<WriteRequest>();

            while (haveWrites()) {
                synchronized(this) {
                    myBuffer.clear();
                    myBuffer.addAll(_writes);
                    _writes.clear();
                }

                Iterator<WriteRequest> myWrites = myBuffer.iterator();
                while (myWrites.hasNext())
                    _prevayler.logCommand(myWrites.next().getCommand(), false);

                myAllWrites.addAll(myBuffer);
            }

            // Now dispatch execution of all logged commands - execute our own first
            //
            try {
                return aComm.execute(_prevayler.system());
            } finally {
                Iterator<WriteRequest> myTargets = myAllWrites.iterator();
                while (myTargets.hasNext()) {
                    // _outCount.incrementAndGet();
                    myTargets.next().dispatch();
                }

                // System.out.println("Logger: incount = " + _inCount + " outcount = " + _outCount);
            }
        }
    }

    private boolean haveWrites() throws Exception {
        synchronized(this) {
            if (_writes.size() > 0)
                return true;
            else {
                _writing = false;
                _prevayler.flush();
                
                return false;
            }
        }
    }
    
    public Snapshotter takeSnapshot() throws IOException {
        return _prevayler.takeSnapshot();
    }

    private class WriteRequest {
        private Command _comm;
        private Object _lock = new Object();
        private boolean _exit = false;

        WriteRequest(Command aComm) {
            // _inCount.incrementAndGet();
            _comm = aComm;
        }

        Command getCommand() {
            return _comm;
        }

        void dispatch() {
            synchronized(_lock) {
                _exit = true;
                _lock.notify();
            }
        }

        void await() {
            synchronized(_lock) {
                while (! _exit) {
                    try {
                        _lock.wait();
                    } catch (InterruptedException anIE) {

                    }
                }
            }
        }
    }
}
