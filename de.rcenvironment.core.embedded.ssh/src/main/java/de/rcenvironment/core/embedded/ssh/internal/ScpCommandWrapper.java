/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.file.FileSystemAware;
import org.apache.sshd.scp.server.ScpCommand;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.ServerSessionAware;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.embedded.ssh.api.ScpContext;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class for pre-processing SCP commands before handing them over to the native {@link ScpCommand}.
 * 
 * This is necessary because Apache Mina does not delegate an ScpCommand for pre-processing. This is not done in the SshFactory because this
 * the currently active user is necessary to decide if the destination is valid or not.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class ScpCommandWrapper implements Command, FileSystemAware, ServerSessionAware {

    private static final int NOT_FOUND_INDEX = -1; // indexOf() result on no match

    private static final String FORWARD_SLASH = "/";

    private static final String BACKSLASH = "\\";

    private static final Pattern SCP_COMMAND_PATTERN = Pattern.compile("(scp (?:-[rtdf]+ +)+)'?(/ra/[^\\s']+)'?\\s*");

    private String command;

    private InputStream in;

    private OutputStream err;

    private OutputStream out;

    private ExitCallback callback;

    private FileSystem fileSystem;

    private ScpCommandFactory scpCommandFactory;

    private ScpContextManager scpContextManager;

    private ServerSession session;

    private final Log logger = LogFactory.getLog(getClass());

    public ScpCommandWrapper(String command, ScpContextManager authenticationManager) {
        this.command = command;
        this.scpContextManager = authenticationManager;
        this.scpCommandFactory = new ScpCommandFactory();
    }

    @Override
    public void start(ChannelSession channelSession, Environment env) throws IOException {
        String username = env.getEnv().get(Environment.ENV_USER);
        try {
            String virtualScpPath = getScpPathOfCommand();
            // if (isValidScpPath(userName)) {
            ScpContext scpContext = scpContextManager.getMatchingScpContext(username, virtualScpPath);
            if (scpContext != null) {
                try {
                    delegateToScp(channelSession, env, scpContext);
                } catch (IOException e) {
                    logger.warn("Exception in SCP command wrapper", e);
                    throw e;
                }
            } else {
                throw new AuthenticationException(StringUtils.format("No permission to access SCP path \"%s\"", virtualScpPath));
            }
        } catch (AuthenticationException e) {
            logger.warn("Denied SCP access for user " + username + ": " + e.toString());
            // close the connection like ScpCommand#run() does on an IOException
            // TODO sometimes, the pscp client shows "connection lost" instead of this error message; reason unclear - misc_ro
            out.write(2);
            out.write(("ERROR: " + e.getMessage()).getBytes());
            out.write('\n');
            out.flush();
            callback.onExit(0);
        }
    }

    @Override
    public void destroy(ChannelSession channelSession) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.debug(e);
        }
    }

    private void delegateToScp(ChannelSession channelSession, Environment env, ScpContext scpContext) throws IOException {
        ScpCommand scpCommand = (ScpCommand) scpCommandFactory.createCommand(channelSession, rewriteCommand(command, scpContext));
        scpCommand.setErrorStream(err);
        scpCommand.setExitCallback(callback);
        scpCommand.setInputStream(in);
        scpCommand.setOutputStream(out);
        scpCommand.setSession(session);
        scpCommand.setFileSystem(fileSystem);
        scpCommand.start(channelSession, env);
    }

    private String rewriteCommand(String originalCommand, ScpContext scpContext) {

        Matcher m = SCP_COMMAND_PATTERN.matcher(originalCommand);

        if (!m.matches()) {
            throw new IllegalStateException("Unexpected/malformed command for Remote Access SCP (case 2): " + originalCommand);
        }
        String originalCommandStart = m.group(1);
        String originalPath = m.group(2);

        if (!originalPath.startsWith(scpContext.getVirtualScpRootPath())) {
            throw new IllegalStateException("Virtual SCP path '" + originalPath + "' does not start with expected root path '"
                + scpContext.getVirtualScpRootPath() + "'");
        }
        String relativePath = originalPath.substring(scpContext.getVirtualScpRootPath().length());
        if (relativePath.startsWith(FORWARD_SLASH) || relativePath.startsWith(BACKSLASH)) {
            relativePath = relativePath.substring(1);
        }
        String rewrittenPath = new File(scpContext.getLocalRootPath(), relativePath).getAbsolutePath().replace(BACKSLASH, FORWARD_SLASH);

        // needed to make MINA accept the rewritten windows path
        // TODO cross-check on linux
        if (!rewrittenPath.startsWith(FORWARD_SLASH)) {
            rewrittenPath = FORWARD_SLASH + rewrittenPath;
        }

        // maintain trailing slash, if it exists, as they get lost in local file operations
        if (originalPath.endsWith(FORWARD_SLASH) && !rewrittenPath.endsWith(FORWARD_SLASH)) {
            rewrittenPath = rewrittenPath + FORWARD_SLASH;
        }

        logger.debug(StringUtils.format("Mapped logical SCP path to '%s'; original command: %s",
            rewrittenPath, originalCommand));

        // Create necessary parent directories, if not existing
        File rewrittenPathParentDir = new File(rewrittenPath).getParentFile();
        rewrittenPathParentDir.mkdirs();

        return originalCommandStart + rewrittenPath;
    }

    private String getScpPathOfCommand() throws AuthenticationException {
        // Apache Mina/SSHD does not hand over the complete SCP command as entered on client side.
        // SCP commands look like "scp [option set of -r/-t/-f/-d] <path>" ; recent libssh versions
        // seem to always enclose the path in single quotes.

        Matcher m = SCP_COMMAND_PATTERN.matcher(command);
        if (!m.matches()) {
            throw new AuthenticationException("Unexpected/malformed command for Remote Access SCP (case 1): " + command);
        }
        String path = m.group(2).trim();
        if (path.contains("..") || path.contains("..")) {
            throw new AuthenticationException("Parent folder traversal (\"..\") is disallowed");
        }
        logger.debug("Extracted SCP path " + path + " from command " + command);
        return path;

    }

    @Override
    public void setInputStream(InputStream inParam) {
        this.in = inParam;
    }

    @Override
    public void setOutputStream(OutputStream outParam) {
        this.out = outParam;
    }

    @Override
    public void setErrorStream(OutputStream errParam) {
        this.err = errParam;
    }

    @Override
    public void setExitCallback(ExitCallback callbackParam) {
        this.callback = callbackParam;
    }

    @Override
    // TODO review: use this for additional security? - misc_ro
    public void setFileSystem(FileSystem fileSystemParam) {
        this.fileSystem = fileSystemParam;
    }

    @Override
    public void setSession(ServerSession arg0) {
        this.session = arg0;
    }

}
