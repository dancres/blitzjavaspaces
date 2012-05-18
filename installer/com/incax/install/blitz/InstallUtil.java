package com.incax.install.blitz;

import java.net.URL;
import java.net.URLClassLoader;
import java.io.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.lang.reflect.Field;

import javax.swing.*;

/**
 */
public class InstallUtil {
    InstallUtil() {
    }

    boolean isWindowsPlatform() {
        String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Windows"))
            return true;
        else
            return false;

    }

    private String convertPath(String in) {
        if (isWindowsPlatform()) {
            //make all fwd slashes
            int pos = in.indexOf("\\");
            while (pos != -1) {
                in = in.replace('\\', '/');
                pos = in.indexOf("\\");
            }
        }
        return in;
    }

    String getPathForClass(Class clazz)
        throws Exception {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        String path = url.getPath();
        //if we'return running on windows remove the proceeding /
        if (java.io.File.separator.equals("\\") && path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        //remove any %20 with spaces
        int index = path.indexOf("%20");
        while (index != -1) {
            path = path.substring(0, index) + " " +
                path.substring(index + 3, path.length());
            index = path.indexOf("%20");
        }
        return path;
    }

    void generateScripts(File targetDir, String aJdkHome,
                                 String aJiniHome, String anHttpdPort)
        throws Exception {

        final int PORT = 1;
        final int JINI_HOME = 2;
        final int CONFIG = 4;//pos in vals[]
        final int LIB = 5;

        String [] keys = {"<JDK_HOME>", "<PORT>", "<JINI_HOME>",
            "<INSTALL_DIR>", "<CONFIG>", "<LIB>", "<LIB_DL>"};
        String [] vals = {
            aJdkHome,
            anHttpdPort,
            convertPath(aJiniHome) + "/lib/",
            convertPath(targetDir.getAbsolutePath()) + "/",
            "",//used later and replaced per scipt
            "lib",
            convertPath(aJiniHome) + "/lib-dl/"
        };

        StringBuffer buf = doTemplateSub(keys, vals,
            "resources/start-trans-blitz_with_httpd.config");
        writeFile(targetDir.getAbsolutePath() +
            "/config/start-trans-blitz_with_httpd.config",
            buf.toString().getBytes());
        //other configs

        //write out scripts
        boolean isWin = isWindowsPlatform();
        String scriptTmpl = isWin ? "start-service.txt" : "sh_start-service.txt";
        String targetScript = "start-trans-blitz_with_httpd." + (isWin ? "bat" : "sh");

        vals[CONFIG] = "config/start-trans-blitz_with_httpd.config";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //generate the generic start blitz out-of-box
        vals[CONFIG] = "config/start-blitz.config";
        buf = doTemplateSub(keys, vals, "resources/start-blitz.config");
        writeFile(targetDir.getAbsolutePath() +
            "/config/start-blitz.config", buf.toString().getBytes());

        targetScript = "blitz." + (isWin ? "bat" : "sh");
        scriptTmpl = isWin ? "start-service.txt" : "sh_start-service.txt";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //now generate the transient files
        vals[CONFIG] = "config/start-trans-blitz.config";
        buf = doTemplateSub(keys, vals, "resources/start-trans-blitz.config");
        writeFile(targetDir.getAbsolutePath() +
            "/config/start-trans-blitz.config",
            buf.toString().getBytes());

        targetScript = "start-trans-blitz." + (isWin ? "bat" : "sh");
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //generate HTTPD for transient service
        targetScript = "start-ws-" + anHttpdPort + "." + (isWin ? "bat" : "sh");
        scriptTmpl = (isWin ? "" : "sh_") + "start-ws-lib.txt";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //generate the activation stuff
        scriptTmpl = isWin ? "start-service.txt" : "sh_start-service.txt";
        targetScript = "start-act-blitz." + (isWin ? "bat" : "sh");
        vals[CONFIG] = "config/start-act-blitz.config";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        scriptTmpl = "start-act-blitz.config";
        targetScript = "config/start-act-blitz.config";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //phoenix start up
        //create reggie script
        scriptTmpl = "start-phoenix.config";
        targetScript = "config/start-phoenix.config";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());
        //create reggie start-script
        scriptTmpl = isWin ? "start-service.txt" : "sh_start-service.txt";
        targetScript = "start-phoenix." + (isWin ? "bat" : "sh");
        vals[CONFIG] = "config/start-phoenix.config";

        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //create reggie script
        scriptTmpl = "start-reggie.config";
        targetScript = "config/start-reggie.config";
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());
        //create reggie start-script
        scriptTmpl = isWin ? "start-service.txt" : "sh_start-service.txt";
        targetScript = "start-reggie." + (isWin ? "bat" : "sh");
        vals[CONFIG] = "config/start-reggie.config";

        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //generate httpd for default 8081 for Jini
        targetScript = "start-jini-ws-8081." + (isWin ? "bat" : "sh");
        scriptTmpl = (isWin ? "" : "sh_") + "start-ws.txt";
        //reset values[PORT]
        vals[PORT] = "8081";
        // vals[LIB]=vals[JINI_HOME];
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());

        //write out the read me file
        buf = doTemplateSub(new String[]{},
            new String[]{}, "resources/readme.txt");
        writeFile(targetDir.getAbsolutePath() +
            "/readme.txt", buf.toString().getBytes());

        //write ths dashboard start script
        scriptTmpl = (isWin ? "" : "sh_") + "dashboard.txt";
        targetScript = "dashboard." + (isWin ? "bat" : "sh");
        buf = doTemplateSub(keys, vals, "resources/" + scriptTmpl);
        writeFile(targetDir.getAbsolutePath() + "/" +
            targetScript, buf.toString().getBytes());


    }

    public StringBuffer doTemplateSub(String [] keys, String [] values, String resource)
        throws IOException {

        StringBuffer buf = new StringBuffer();
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ln = br.readLine();
        while (ln != null) {

            for (int i = 0; i < keys.length; i++) {
                ln = parseLine2(ln, keys[i], values[i]);
            }
            buf.append(ln);
            buf.append("\n");
            ln = br.readLine();
        }
        br.close();
        return buf;
    }

    private String parseLine2(String ln, String token, String newToken) {
        String tmpLn = ln;
        int si = tmpLn.indexOf(token);
        boolean modified = false;
        while (si != -1) {
            modified = true;
            //System.out.println("Replacing token "+token+" with "+newToken);

            StringBuffer buf = new StringBuffer();
            buf.append(tmpLn.substring(0, si));
            buf.append(newToken);
            buf.append(tmpLn.substring(si + token.length(), tmpLn.length()));
            tmpLn = buf.toString();

            si = tmpLn.indexOf(token, si + newToken.length());
        }
        return modified ? tmpLn : ln;
    }

    private void writeFile(String fname, byte bytes[])
        throws IOException {

        //System.out.println("writeFile "+fname);

        FileOutputStream fos = new FileOutputStream(fname);
        fos.write(bytes);
        fos.close();

    }

    void extractZip(File zipFile, File targetDir, JProgressBar aBar)
        throws Exception {


        ZipFile jar = new ZipFile(zipFile);
        long size = zipFile.length();

        if (aBar != null)
            aBar.setMaximum((int) size);
        long progress = 0;

        //first make the directory structure
        for (Enumeration iter = jar.entries(); iter.hasMoreElements();) {
            ZipEntry e = (ZipEntry) iter.nextElement();
            String eName = e.getName();
            int pos = eName.lastIndexOf("/");
            if (pos != -1) {
                String dir = eName.substring(0, pos);

                if (dir.indexOf("extract") != -1) {
                    int spos = dir.indexOf("extract/");

                    String subDir = "";
                    if (spos != pos) {
                        subDir = dir.substring(spos + "extract/".length(), pos);
                    }
                    File fileDir = new File(targetDir.getAbsolutePath() +
                        File.separator + subDir);

                    //System.out.println("mkdir "+fileDir.getAbsolutePath());


                    fileDir.mkdirs();
                    if (e.isDirectory() == false) {
                        String name = eName.substring(pos);
                        //System.out.println("file="+targetDir.getAbsolutePath() + File.separator + subDir+"/"+name);
                        FileOutputStream os =
                            new FileOutputStream(fileDir.getAbsolutePath() +
                                File.separator + name);
                        InputStream fis = jar.getInputStream(e);
                        byte b[] = new byte[2048];
                        for (int nBytes = fis.read(b); nBytes != -1; nBytes = fis.read(b))
                            os.write(b, 0, nBytes);

                        fis.close();
                        os.close();
                    }

                }
                progress += e.getSize();

                if (aBar != null)
                    aBar.setValue((int) progress);
            }
        }
    }

    boolean validateJiniHome(String jiniHomeDir, JPanel anInstallPanel) {
        if (Boolean.getBoolean("blitz.nocheck"))
            return true;

        if (jiniHomeDir.length() == 0) {
            if (anInstallPanel != null)
                JOptionPane.showMessageDialog(anInstallPanel,
                    "Please Select where Jini 2.1 is installed on your computer");
            else
                System.err.println("No Jini 2.1 directory specified");
            return false;
        }
        try {
            File f = new File(jiniHomeDir);
            if (!f.exists()) {
                if (anInstallPanel != null)
                    JOptionPane.showMessageDialog(anInstallPanel,
                        "The selected directory for Jini 2.1 does not exist");
                else
                    System.err.println("The selected directory for Jini 2.1 does not exist");

                return false;
            }
            //check that prebuilt-outrigger-logstore.jar exists as this is from jini 2.1 upwards

            //now try to load the VersionsConstant class
            String versionsJar = jiniHomeDir + "/lib/sun-util.jar";
            f = new File(versionsJar);
            if (!f.exists()) {
                if (anInstallPanel != null)
                    JOptionPane.showMessageDialog(anInstallPanel,
                        "The selected directory for Jini 2.1 is incorrect");
                else
                    System.err.println("The selected directory doesn't appear to contain a Jini 2.1 install");

                return false;
            }
            URLClassLoader cl = new URLClassLoader(new URL[]{f.toURL()});

            Class c = cl.loadClass("com.sun.jini.constants.VersionConstants");
            Field version = c.getField("SERVER_VERSION");
            String jiniVersion = (String) version.get(null);
            if (jiniVersion.startsWith("2.1")) {
                f = new File(jiniHomeDir + "/lib/prebuilt-outrigger-logstore.jar");
                if (!f.exists()) {
                    if (anInstallPanel != null)
                        JOptionPane.showMessageDialog(anInstallPanel,
                            "Missing files in Jini 2.1 directory\nPlease check that your Jini 2.1 install is complete\nAlso please that you are not using the Jini 2.1 beta");
                    else
                        System.err.println("Missing files in Jini 2.1 directory\nPlease check that your Jini 2.1 install is complete\nAlso please that you are not using the Jini 2.1 beta");
                    return false;
                }
                return true;
            }

            if (anInstallPanel != null)
                JOptionPane.showMessageDialog(anInstallPanel,
                    "Incorrect Jini version\nFound " + jiniVersion +
                        "\nBlitz requires version 2.1");
            else
                System.err.println("Incorrect Jini version\nFound " + jiniVersion +
                    "\nBlitz requires version 2.1");
        } catch (Exception ex) {
            if (anInstallPanel != null)
                JOptionPane.showMessageDialog(anInstallPanel,
                    "Unable to verify Jini 2.1: reason\n" + ex);
            else {
                System.err.println("Unable to verify Jini 2.1: reason\n" + ex);
                ex.printStackTrace(System.err);
            }
        }
        return false;
    }
}
