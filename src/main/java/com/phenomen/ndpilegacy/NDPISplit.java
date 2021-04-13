/***
 * Image/J Plugins
 * Copyright (C) 2012-2014 C. Deroulers
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */
package com.phenomen.ndpilegacy;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import ij.IJ;

/**
 * Launch the ndpisplit command line tool.
 *
 * @author Christophe Deroulers
 * @version $Revision: 1.7.1 $
 */

final class NdpisplitProcess {
    public final String pathToExecutable;
    Process process;

    public NdpisplitProcess(String path, Process p)
    {
        pathToExecutable = path;
        process = p;
    }
}

final class MessagesFromNdpisplit {
    public final String pathToExecutable;
    public final int exitValue;
    final String[] outputFromStdout;
    public final String versionFromStderr;
    public final String versionSuffixFromStderr;

    public MessagesFromNdpisplit(String p, int ev, String[] o, String v,
        String vs)
    {
        pathToExecutable = p;
        exitValue = ev;
        outputFromStdout = o;
        versionFromStderr = v;
        versionSuffixFromStderr = vs;
    }

    public String[] getOutputFromStdout() {
        return outputFromStdout;
    }
}

public class NDPISplit {
    /**
     * Launch the ndpisplit command line tool.
     *
     * @param args Command line arguments.
     * @param log  Should we output diagnostic messages to the log
     *            window?
     * @return  Array of strings printed by ndpisplit on the standard
     *            output.
     * @throws IOException In case of I/O error.
     */
    public static String[] run(String args[], boolean log)
        throws
        IOException, InterruptedException
    {
        MessagesFromNdpisplit res = runAndGetDiagnosis(args, log);
        /*if (res.exitValue != 0)
            return new String[0];*/
        return res.getOutputFromStdout();
    }

    public static MessagesFromNdpisplit       runAndGetDiagnosis(String args[], boolean log)
        throws
        IOException, InterruptedException
    {
        String[] cmd = new String[args.length+1];
        String cmdline = "";
        String executableName;
        String[] executableDirs = {"", "/usr/local/bin", "plugins",
            "macros", "imagej", "current", "image", "startup", "home",
            ""};

        for (int i = 2 ; i < executableDirs.length-1 ; i++)
            executableDirs[i] = ij.IJ.getDirectory(executableDirs[i]);
        executableDirs[executableDirs.length-1] = executableDirs[executableDirs.length-2] + File.separator    + "Desktop";

        String osName = System.getProperty("os.name" );
        if( osName.startsWith( "Windows" ) )  executableName = "ndpisplit.exe" ;
        else  executableName = "ndpisplit" ;

        cmdline = executableName;
        for (int i = 0 ; i < args.length ; i++)
        {
            cmd[i+1] = args[i];
            if (args[i].indexOf(" ") >= 0)
                cmdline += " \"" + args[i] + "\"";
            else
                cmdline += " " + args[i];
            /*System.err.printf("cmd[%d]=\"%s\"\n", i, args[i]);*/
        }

        IJ.log("Running " + cmdline);
 
        NdpisplitProcess ndpisplit = tryToLaunch(cmd, executableDirs, executableName);
        NDPIStreamParser errorParser = new   NDPIStreamParser(ndpisplit.process.getErrorStream(), log);
        NDPIStreamAccumulator outputAccumulator = new NDPIStreamAccumulator(ndpisplit.process.getInputStream());
        errorParser.start();
        outputAccumulator.start();

        int exitVal = ndpisplit.process.waitFor();
        errorParser.join();
        outputAccumulator.join();
        return new MessagesFromNdpisplit(ndpisplit.pathToExecutable,
            exitVal, outputAccumulator.getLines(),
            errorParser.getVersion(),
            errorParser.getVersionSuffix());
    }

    private static NdpisplitProcess tryToLaunch(String[] cmd,
            String[] executableDirs, String executableName)
        throws IOException
    {
        Runtime rt = Runtime.getRuntime();
        String dirlist = "";
        for (int i = 0 ; i < executableDirs.length ; i++)
        {
            String dir = executableDirs[i];

            if (dir == null)
                continue;
            if (! dir.equals(""))
            {
                cmd[0] = dir + File.separator +
                    executableName;
                dirlist = dirlist + "\n" + dir;
            }
            else
            {
                cmd[0] = executableName;
                dirlist = dirlist + "\n[the default PATH]";
            }
            try
            {
                final File workingDirectory = null;
                return new NdpisplitProcess(cmd[0],
                    rt.exec(cmd, null, workingDirectory));
            }
            catch (IOException ex)
            {
                if (ex.getMessage().endsWith(
                        "error=2, No such file or directory"))
                    continue;
            }
        }
        throw new FileNotFoundException("Can't find executable of the "
            + executableName + " program in the following list: " +
            dirlist);
    }

    private static String textFromVersion(String versionAndSuffix)
    {
        int p = versionAndSuffix.indexOf('-');

        if (p < 0)
          return versionAndSuffix + " or greater";

        String version = versionAndSuffix.substring(0, p);
        String suffix = versionAndSuffix.substring(p+1);
        return version + "-x with x at least " + suffix;
    }

    /**
     * Return a non-null string -- the version of the available 
     * ndpisplit -- if and only if this version is more recent than at 
     * least one of the versions in the provided list. Issue a message 
     * listing acceptable versions (if any) if it's older.
     *
     * @param minimalAcceptedVersions List of accepted versions 
     *                               (strings, e.g. "1.4.4-1")
     * @param title Title of message window
     */
    public static String getAcceptableVersion(
            String[] minimalAcceptedVersions, String title,
            String functionality)
    {
        String[] args = new String[1];
        args[0] = "-K";
        MessagesFromNdpisplit d;
        try
        {
            d = runAndGetDiagnosis(args, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = "Error while getting version of ndpisplit.\n \n";
            msg += (ex.getMessage() == null) ? ex.toString() : ex.getMessage();
            IJ.showMessage(title, msg);
            return null;
        }
        String[] res = d.getOutputFromStdout();

        String version = null;
        String versionSuffix = "";
        for (int i = 0 ; i < res.length ; i++)
        {
            int pos = res[i].indexOf(':');
            if (pos < 0)
                continue;
            if (res[i].startsWith("Ndpisplit version:"))
            {
                version = res[i].substring(pos+1);
                int p = version.indexOf('-');
                if (p >= 0)
                {
                    versionSuffix = version.substring(p+1);
                    version = version.substring(0, p);
                }
                break;
            }
        }
        if (version == null)
        {
            version = d.versionFromStderr;
            versionSuffix = d.versionSuffixFromStderr;
        }

        if (version != null)
            for (int i = 0 ; i < minimalAcceptedVersions.length ; i++)
            {
                String acceptedVersion = minimalAcceptedVersions[i];
                String minimalAcceptedSuffix = "";
                int p = acceptedVersion.indexOf('-');
                if (p >= 0)
                {
                    minimalAcceptedSuffix = acceptedVersion.substring(p+1);
                    acceptedVersion = acceptedVersion.substring(0, p);
                    if (version.compareTo(acceptedVersion) == 0 &&
                        versionSuffix.compareTo(minimalAcceptedSuffix) >= 0)
                        return version + "-" + versionSuffix;
                } else {
                    if (version.compareToIgnoreCase(acceptedVersion) >= 0)
                        return version +
                            (versionSuffix.equals("") ? "" : "-" + versionSuffix);
                }
            }

        if (minimalAcceptedVersions.length > 0)
        {
            String msg = "Version of ndpisplit " +
                (version == null ? "not found" :
                "too old" +
                (functionality.equals("") ? "" :
                 " (for " + functionality + ")" ) +
                ": " + version) +
                ".\n(ndpisplit program found as\n \"" +
                d.pathToExecutable + "\").\n \n" +
                "Please replace it with one of the following versions:\n" +
                textFromVersion(minimalAcceptedVersions[0]);
            for (int i = 1 ; i < minimalAcceptedVersions.length ; i++)
                msg += ", " + textFromVersion(minimalAcceptedVersions[i]);
            IJ.showMessage(title, msg);
            IJ.showStatus("");
        }

        return null;
    }
}

class NDPIStreamAccumulator extends Thread
{
    InputStream is;
    ArrayList<String> lines;

    NDPIStreamAccumulator(InputStream is)
    {
        this.is = is;
        this.lines = new ArrayList<String>();
    }

    public String[] getLines()
    {
        return lines.toArray(new String[0]);
    }

    public void run()
    {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while (true)
        {
            try
            {
                line = br.readLine();
                if (line == null)
                    break;
                lines.add(line);
            }
            catch (IOException ioe)
            {
            }
        }
    }
}

class NDPIStreamParser extends Thread
{
    InputStream is;
    boolean log;
    boolean inProgress;
    int maxCount;
    String foundVersion;
    String foundVersionSuffix;
    final static Pattern patternProgression = Pattern.compile(
        ".*cpStrips2Tiles remaining lines:\\s*([0-9]+)\\D*");
    final static Pattern patternVersion = Pattern.compile(
        "^ndpisplit version\\s+([0-9]+(?:\\.[0-9])*(?:-([0-9a-zA-Z_-]+))?)" +
        "(?:\\s+license.*|)$");

    NDPIStreamParser(InputStream is, boolean log)
    {
        this.is = is;
        this.log = log;
    }

    public void run()
    {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        inProgress = false;
        while (true)
        {
            try
            {
                line = br.readLine();
                if (line == null)
                    break;
                /*System.err.printf("Got \"%s\"\n", line);*/

                java.util.regex.Matcher mVersion =
                    patternVersion.matcher(line);
                if (mVersion.matches())
                {
                    foundVersion = mVersion.group(1);
                    foundVersionSuffix = mVersion.group(2);
                    if (foundVersionSuffix == null)
                        foundVersionSuffix = "";
                }

                java.util.regex.Matcher mProgression =
                    patternProgression.matcher(line);
                if (mProgression.matches())
                {
                    int remaining =
                        java.lang.Integer.decode(mProgression.group(1));
                    if (inProgress == false)
                        maxCount = remaining;
                    inProgress = true;
                    IJ.showProgress(1-1.*remaining/maxCount);
                }
                else
                {
                    inProgress = false;
                    IJ.showProgress(1.0);
                    if (log)
                        IJ.log(line);
                }
            }
            catch (IOException ioe)
            {
                IJ.showProgress(1.0);
            }
        }
    }

    public String getVersion()
    {
        return foundVersion;
    }

    public String getVersionSuffix()
    {
        return foundVersionSuffix;
    }
}
