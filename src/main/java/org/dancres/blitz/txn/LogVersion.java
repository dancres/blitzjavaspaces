package org.dancres.blitz.txn;

import java.io.Serializable;

class LogVersion implements Serializable {
    static final long serialVersionUID = -6949591955686028824L;

    static final LogVersion VERSION = new LogVersion(1, 1);

    private int theMajor;
    private int theMinor;

    private LogVersion(int aMajor, int aMinor) {
        theMajor = aMajor;
        theMinor = aMinor;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof LogVersion) {
            LogVersion myOther = (LogVersion) anObject;

            return ((myOther.theMajor == theMajor) &&
                    (myOther.theMinor == theMinor));
        }

        return false;
    }

    public String toString() {
        return "LogVersion: " + theMajor + "." + theMinor;
    }
}