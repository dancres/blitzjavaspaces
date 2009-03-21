package org.dancres.blitz.remote.test;

import net.jini.core.entry.Entry;

/**
 */
public class StressReapItem implements Entry {
    public String m_key;
    public String m_value;

    public StressReapItem() {
        this("", 10);
    }

    public StressReapItem(String key, int bytesToSnarfUp) {
        StringBuffer buf = new StringBuffer(bytesToSnarfUp);
        for (int i = 0; i < bytesToSnarfUp; i++)
            buf.append('a' + ((char) (Math.random() * 26)));

        m_value = buf.toString();
    }
}
