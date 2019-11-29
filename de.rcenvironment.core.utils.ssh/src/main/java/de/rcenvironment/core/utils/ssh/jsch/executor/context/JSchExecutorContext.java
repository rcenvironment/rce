/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch.executor.context;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.context.spi.ExecutorContext;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchCommandLineExecutor;

/**
 * {@link ExecutorContext} implementation using the {@link JSchCommandLineExecutor}.
 * 
 * @author Robert Mischke
 */
public class JSchExecutorContext implements ExecutorContext {
    
    private Session jschSession;

    private SshSessionConfiguration staticConfiguration;

    private RemoteTempDirFactory tempDirFactory;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Constructor that uses the default root temp directory.
     * 
     * @param staticConfiguration the SSH configuration to use
     */
    public JSchExecutorContext(SshSessionConfiguration staticConfiguration) {
        this(staticConfiguration, "/tmp/rce-tmp/");
    }

    /**
     * Constructor that allows setting a custom root temp directory.
     * 
     * @param staticConfiguration the SSH configuration to use
     * @param tempDirRoot the root temp directory to use
     */
    public JSchExecutorContext(SshSessionConfiguration staticConfiguration, String tempDirRoot) {
        this.staticConfiguration = staticConfiguration;
        this.tempDirFactory = new RemoteTempDirFactory(tempDirRoot);
    }

    @Override
    public void setUpSession() throws IOException, ValidationFailureException {
        try {
            jschSession = setupJSchSession();
            log.info("SSH connection established");
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SshParameterException e) {
            throw new ValidationFailureException(e.getMessage());
        }
    }

    @Override
    public void tearDownSession() throws IOException {
        if (jschSession == null) {
            throw new IllegalStateException("session not set up");
        }
        log.info("Closing SSH connection");
        jschSession.disconnect();
        // TODO delete temporary directories created via #createUniqueTempDir()?
    }

    @Override
    public CommandLineExecutor setUpSandboxedExecutor() throws IOException {
        if (jschSession == null) {
            throw new IllegalStateException("no session set up");
        }
        String sandboxDir = tempDirFactory.createTempDirPath("sandbox", "-");
        log.debug("Setting SSH sandbox to " + sandboxDir);
        // create sandbox
        CommandLineExecutor executor = new JSchCommandLineExecutor(jschSession, tempDirFactory.getRootDir());
        String sandboxDirName = new File(sandboxDir).getName();
        try {
            executor.start("mkdir -p " + sandboxDirName);
            executor.waitForTermination();
        } catch (InterruptedException e) {
            log.warn("Interrupted while setting up remote SSH sandbox", e);
        }
        return new JSchCommandLineExecutor(jschSession, sandboxDir);
    }

    @Override
    public void tearDownSandbox(CommandLineExecutor executor) throws IOException {
        if (jschSession == null) {
            throw new IllegalStateException("no session set up");
        }
        String sandboxPath = executor.getWorkDirPath();
        log.debug("Cleaning SSH sandbox at " + sandboxPath);
        // intentionally using the full path, and no -f or -r option for safety;
        // as a consequence, this is not able to delete subdirectories for now -- misc_ro
        try {
            executor.start("rm " + sandboxPath + "/*");
            executor.waitForTermination();
            executor.start("rmdir " + sandboxPath);
            executor.waitForTermination();
        } catch (InterruptedException e) {
            log.warn("Interrupted while cleaning up remote SSH sandbox", e);
        }
    }

    @Override
    public String createUniqueTempDir(String contextHint) throws IOException {
        String tempDirPath = tempDirFactory.createTempDirPath(contextHint, "-");
        log.debug("Generated temp directory path " + tempDirPath);
        return tempDirPath;
    }

    private Session setupJSchSession() throws JSchException, SshParameterException {
        String host = staticConfiguration.getDestinationHost();
        int port = staticConfiguration.getPort();
        String user = staticConfiguration.getSshAuthUser();
        String keyfileLocation = staticConfiguration.getSshKeyFileLocation();
        String authPhrase = staticConfiguration.getSshAuthPhrase();

        // TODO rewrite to use JschSessionFactory factory method?
        Logger jschLogger = new Logger() {

            @Override
            public boolean isEnabled(int arg0) {
                return true;
            }

            @Override
            public void log(int arg0, String arg1) {
                if (log.isTraceEnabled()) {
                    log.trace("JSch Log [Level " + arg0 + "]: " + arg1);
                }
            }
        };

        return JschSessionFactory.setupSession(host, port, user, keyfileLocation, authPhrase, jschLogger);
    }
}
