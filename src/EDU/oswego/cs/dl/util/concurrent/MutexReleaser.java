package EDU.oswego.cs.dl.util.concurrent;

public class MutexReleaser implements LockReleaser {
    private Mutex theMutex;

    public MutexReleaser(Mutex aMutex) {
        theMutex = aMutex;
    }

    public void release() {
        theMutex.release();
    }
}
