/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.optional.ssh.ScpFromMessage;
import org.apache.tools.ant.taskdefs.optional.ssh.ScpToMessage;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Provides SCP and remote-to-remote copy operations for established JSch sessions.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public final class JschFileTransfer {

    private static final String SLASH = "/";

    /**
     * Arbitrary "short wait" for remote-to-remote copy operations.
     */
    private static final int SHORT_WAIT_MSEC = 100;

    private JschFileTransfer() {
        // prevent instantiation
    }

    /**
     * Uploads a file with SCP.
     * 
     * @param session an established JSch session
     * @param localFile the local {@link File} to upload
     * @param remotePath the remote target path of the file to copy, using a relative path to the initial SSH "home" directory if necessary
     * @throws JSchException on general SSH errors
     * @throws IOException on SCP operation failure
     */
    public static void uploadFile(Session session, File localFile, String remotePath) throws IOException, JSchException {
        ScpToMessage message = new ScpToMessage(session, localFile, remotePath);
        message.execute();
    }
    
    /**
     * Uploads directories with SCP.
     * 
     * @param session an established JSch session
     * @param directory the local directory to upload
     * @param remotePath the remote target path of the directory to copy, using a relative path to the
     *        initial SSH "home" directory if necessary
     * @throws JSchException on general SSH errors
     * @throws IOException on SCP operation failure
     * @throws InterruptedException if creating directory failed
     */
    public static void uploadDirectory(Session session, File directory, String remotePath)
        throws IOException, JSchException, InterruptedException {

        remotePath = remotePath + "/" + directory.getName();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        // NOTE: the provided paths are expected to require no escaping
        channel.setCommand("mkdir -p " + remotePath);
        channel.connect();
        while (!channel.isClosed()) {
            // dir creation is usually fast, so only wait for a short time
            Thread.sleep(SHORT_WAIT_MSEC);
        }
        channel.disconnect();
        if (channel.getExitStatus() != 0) {
            throw new IOException("Creating directory failed: "  + remotePath);
        }
        
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectory(session, file, remotePath);
            } else {
                uploadFile(session, file, remotePath);
            }
        }
    }

    /**
     * Uploads directories with SCP. Expects that parent directories (if not existing) will be created by the server.
     * 
     * @param session an established JSch session
     * @param directory the local directory to upload
     * @param remotePath the remote target path of the directory to copy, using a relative path to the initial SSH "home" directory if
     *        necessary
     * @throws JSchException on general SSH errors
     * @throws IOException on SCP operation failure
     * @throws InterruptedException if creating directory failed
     */
    public static void uploadDirectoryToRCEInstance(Session session, File directory, String remotePath)
        throws IOException, JSchException, InterruptedException {

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectoryToRCEInstance(session, file, remotePath + SLASH + file.getName());
            } else {
                uploadFile(session, file, remotePath + SLASH + file.getName());
            }
        }
    }

    /**
     * Downloads a file with SCP.
     * 
     * @param session an established JSch session
     * @param localFile the local {@link File} to download to
     * @param remotePath the remote source path of the file to copy, using a relative path to the initial SSH "home" directory if necessary
     * @throws JSchException on general SSH errors
     * @throws IOException on SCP operation failure
     */
    public static void downloadFile(Session session, String remotePath, File localFile) throws IOException, JSchException {
        // "false" = not recursive
        ScpFromMessage message = new ScpFromMessage(session, remotePath, localFile, false);
        message.execute();
    }

    /**
     * Downloads a directory with SCP.
     * 
     * @param session an established JSch session
     * @param localDir the local directory ({@link File}) to download to
     * @param remotePath the remote source path of the file to copy, using a relative path to the initial SSH "home" directory if necessary
     * @throws JSchException on general SSH errors
     * @throws IOException on SCP operation failure
     */
    public static void downloadDirectory(Session session, String remotePath, File localDir) throws IOException, JSchException {
        // "true" = recursive
        ScpFromMessage message = new ScpFromMessage(session, remotePath, localDir, true);
        message.execute();
    }

    /**
     * Performs a file copy operation on the remove host through an existing SSH connection.
     * 
     * IMPORTANT: This method uses the standard "mkdir" and "cp" shell commands on the remote system, and will therefore not work on
     * standard Windows hosts.
     * 
     * @param jschSession an established JSch session
     * @param source the source path of the file to copy, using a relative path to the initial SSH "home" directory if necessary. IMPORTANT:
     *        this path is expected to work without shell escaping; in particular, it should not contain spaces or reserved shell
     *        characters.
     * @param target the target path of the file to copy, using a relative path to the initial SSH "home" directory if necessary; if the
     *        containing directory does not exist, it will be created. IMPORTANT: this path is expected to work without shell escaping; in
     *        particular, it should not contain spaces or reserved shell characters.
     * @throws JSchException on general SSH exceptions
     * @throws IOException if the remote copy operation returned a non-zero exit code
     * @throws InterruptedException if the thread is interrupted while waiting for the copy to complete
     */
    public static void remoteToRemoteCopy(Session jschSession, String source, String target) throws JSchException, IOException,
        InterruptedException {

        // generate a "mkdir" command for the target directory, if present
        String mkdirCommandPrefix = "";
        int separatorPos = target.lastIndexOf(SLASH);
        if (separatorPos >= 0) {
            // contains slash -> has directory part
            String directoryPart = target.substring(0, separatorPos);
            mkdirCommandPrefix = "mkdir -p " + directoryPart + " && ";
        }

        ChannelExec channel = (ChannelExec) jschSession.openChannel("exec");
        // NOTE: the provided paths are expected to require no escaping
        String fullCommand = mkdirCommandPrefix + "cp " + source + " " + target;
        LogFactory.getLog(JschFileTransfer.class).debug("Performing remote copy: " + fullCommand);
        channel.setCommand(fullCommand);
        channel.connect();
        while (!channel.isClosed()) {
            // file copy is usually fast, so only wait for a short time
            Thread.sleep(SHORT_WAIT_MSEC);
        }
        channel.disconnect();
        if (channel.getExitStatus() != 0) {
            throw new IOException("Remote copy operation failed: " + source + " -> " + target);
        }
    }
}
