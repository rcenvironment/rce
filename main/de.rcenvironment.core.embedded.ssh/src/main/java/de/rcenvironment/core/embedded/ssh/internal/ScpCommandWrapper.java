/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.file.FileSystemAware;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.command.ScpCommand;
import org.apache.sshd.server.command.ScpCommandFactory;

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
 */
public class ScpCommandWrapper implements Command, FileSystemAware {

    private static final int NOT_FOUND_INDEX = -1; // indexOf() result on no match

    private static final String FORWARD_SLASH = "/";

    private static final String BACKSLASH = "\\";

    private String command;

    private InputStream in;

    private OutputStream err;

    private OutputStream out;

    private ExitCallback callback;

    private FileSystemView fileSystemView;

    private ScpCommandFactory scpCommandFactory;

    private ScpContextManager scpContextManager;

    private final Log logger = LogFactory.getLog(getClass());

    public ScpCommandWrapper(String command, ScpContextManager authenticationManager) {
        this.command = command;
        this.scpContextManager = authenticationManager;
        this.scpCommandFactory = new ScpCommandFactory();
    }

    @Override
    public void start(Environment env) throws IOException {
        String username = env.getEnv().get(Environment.ENV_USER);
        try {
            String virtualScpPath = getScpPathOfCommand();
            // if (isValidScpPath(userName)) {
            ScpContext scpContext = scpContextManager.getMatchingScpContext(username, virtualScpPath);
            if (scpContext != null) {
                try {
                    delegateToScp(env, scpContext);
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
    public void destroy() {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.debug(e);
        }
    }

    private void delegateToScp(Environment env, ScpContext scpContext) throws IOException {
        ScpCommand scpCommand = (ScpCommand) scpCommandFactory.createCommand(rewriteCommand(command, scpContext));
        scpCommand.setErrorStream(err);
        scpCommand.setExitCallback(callback);
        scpCommand.setInputStream(in);
        scpCommand.setOutputStream(out);
        scpCommand.setFileSystemView(fileSystemView);
        scpCommand.start(env);
    }

    private String rewriteCommand(String originalCommand, ScpContext scpContext) {
        // TODO make stricter by regexp parsing - misc_ro
        int startOfPath = originalCommand.indexOf(FORWARD_SLASH);
        String originalCommandStart = originalCommand.substring(0, startOfPath);
        String originalPath = originalCommand.substring(startOfPath);

        logger.debug(StringUtils.format("Rewriting access to logical file path '%s' (command prefix: '%s')",
            originalPath, originalCommandStart));
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

        logger.debug("Final SCP mapped path: " + rewrittenPath);
        
        //Create necessary parent directories, if not existing
        File rewrittenPathParentDir = new File(rewrittenPath).getParentFile();
        rewrittenPathParentDir.mkdirs();

        return originalCommandStart + rewrittenPath;
    }

    private String getScpPathOfCommand() throws AuthenticationException {
        // Apache Mina/ SSHD dows not hand over the complete SCP command as entered on Client side.
        // the scp command looks like: scp -t (or -r) PFAD
        int slashPos = command.indexOf(FORWARD_SLASH);
        if (slashPos == NOT_FOUND_INDEX) {
            throw new AuthenticationException("SCP path must contain at least one forward slash");
        }
        if (command.charAt(slashPos - 1) != ' ') {
            throw new AuthenticationException("SCP path must start with a forward slash");
        }
        String path = command.substring(slashPos).trim();
        if (path.contains("..") || path.contains("..")) {
            throw new AuthenticationException("Parent folder traversal (\"..\") is disallowed");
        }
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
    public void setFileSystemView(FileSystemView fileSystemViewParam) {
        this.fileSystemView = fileSystemViewParam;
    }

}
