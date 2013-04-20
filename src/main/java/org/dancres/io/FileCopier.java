package org.dancres.io;

import java.io.*;
import java.nio.channels.*;
import java.nio.*;

public class FileCopier {
    private ByteBuffer theBuffer =
        ByteBuffer.allocateDirect(16 * 1024 * 1024);

    public FileCopier() {
    }

    /**
       Pass file name and directory to copy it to
     */
    public static void main(String args[]) {
        try {
            File myIn = new File(args[0]);
            File myOut = new File(args[1]);

            FileCopier myCopier = new FileCopier();

            long myStart = System.currentTimeMillis();

            myCopier.copy (myIn, myOut);

            long myEnd = System.currentTimeMillis();

            // System.out.println("Total time: " + (myEnd - myStart));

        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    public void copy(File aSource, File aDestDir) throws IOException {
        FileInputStream myInFile = new FileInputStream(aSource);

        FileOutputStream myOutFile =
            new FileOutputStream(new File(aDestDir, aSource.getName()));

        FileChannel myIn = myInFile.getChannel();
        FileChannel myOut = myOutFile.getChannel();

        boolean end = false;

        while (true) {

            int myBytes = myIn.read(theBuffer);

            if (myBytes != -1) {
                theBuffer.flip();

                myOut.write(theBuffer);

                theBuffer.clear();
            } else
                break;
        }

        myIn.close();
        // myOut.force(false);
        myOut.close();
        myInFile.close();
        myOutFile.close();

        long myEnd = System.currentTimeMillis();
    }
}
