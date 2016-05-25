/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A factory for configuration-based creation of JSch {@link Session}s. Supported authentication methods are password and SSH keyfile (with
 * an optional passphrase).
 * 
 * @author Robert Mischke
 */
public final class JschSessionFactory {

    private static Log log = LogFactory.getLog(JschSessionFactory.class);

    private JschSessionFactory() {
        // prevent instantiation
    }

    /**
     * Simple adapter from com.jcraft.jsch.Logger to Apache Commons Logging.
     * 
     * @author Robert Mischke
     * @author Brigitte Boden
     */
    private static final class ACLDelegate implements Logger {

        private final Log apacheCommonsLogger;

        private final int minLevel;

        private ACLDelegate(Log apacheCommonsLogger, int minLevel) {
            this.apacheCommonsLogger = apacheCommonsLogger;
            this.minLevel = minLevel;
        }

        @Override
        public void log(int level, String arg1) {
            if (level >= minLevel) {
                final String logMessage = "JSch connection log: " + level + ": " + arg1;
                if (level == 0 || level == 1) {
                    // Debug and info messages are both logged as debug, as the "info" output is quite verbose.
                    apacheCommonsLogger.debug(logMessage);
                } else if (level == 2) {
                    apacheCommonsLogger.warn(logMessage);
                } else {
                    apacheCommonsLogger.error(logMessage);
                }
            }
        }

        @Override
        public boolean isEnabled(int level) {
            return level >= minLevel;
        }
    }

    /**
     * A stub class that provides pseudo user responses for password authentication.
     * 
     * @author Robert Mischke
     */
    private static final class UserInfoAdapter implements UserInfo, UIKeyboardInteractive {

        private String pw;

        UserInfoAdapter(String pw) {
            this.pw = pw;
        }

        @Override
        public String getPassphrase() {
            // not expected to be called
            log.warn("SSH called getPassphrase() unexpectedly");
            return null;
        }

        @Override
        public String getPassword() {
            // not expected to be called
            log.warn("SSH called getPassword() unexpectedly");
            return pw;
        }

        @Override
        public boolean promptPassphrase(String arg0) {
            // not expected to be called
            log.warn("SSH login sent a passphrase prompt (answered with no passphrase): " + arg0);
            return false;
        }

        @Override
        public boolean promptPassword(String arg0) {
            log.debug("SSH login sent a password prompt: " + arg0);
            return true;
        }

        @Override
        public boolean promptYesNo(String arg0) {
            // not expected to be called
            log.warn("SSH login sent a yes/no prompt (answered 'no'): " + arg0);
            return false;
        }

        @Override
        public void showMessage(String arg0) {
            log.debug("SSH login sent a message: " + arg0);
        }

        @Override
        public String[] promptKeyboardInteractive(String arg0, String arg1, String arg2, String[] arg3, boolean[] arg4) {
            if (log.isDebugEnabled()) {
                log.debug(StringUtils
                    .format("Simulating keyboard-interactive login; display parameters: %s, %s, %s, %d", arg0, arg1, arg2, arg3.length));
            }
            return new String[] { pw };
        }
    }

    /**
     * @param host the target host to connect to
     * @param port the port number
     * @param user the login username
     * @param keyfileLocation the path of the local keyfile (optional); if null or empty, password auth is used. For convenience, "~" is
     *        automatically resolved to ${user.home}. (NB: This is one of the reasons why a String is used instead of a {@link File}.)
     * @param authPhrase the authentication phrase; if a keyfile is set, this is taken as the keyfile passphase; otherwise, it is used as
     *        the login password
     * @param connectionLogger the JSch-internal logger instance to use
     * @return the initialized JSch session on success
     * @throws SshParameterException on invalid parameters
     * @throws JSchException on SSH errors
     */
    public static Session setupSession(String host, int port, String user, String keyfileLocation, String authPhrase,
        Logger connectionLogger) throws JSchException, SshParameterException {

        // TODO provide a constructor that accepts a SSHSessionConfiguration directly?

        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(user, host, port);
        jschSession.setConfig("StrictHostKeyChecking", "no");

        // sanitize & trim keyfile location
        keyfileLocation = normalizeKeyfilePath(keyfileLocation);

        // validate parameters
        if (host.length() == 0) {
            throw new SshParameterException("The host name or address cannot be empty");
        }
        if (port < 0) {
            throw new SshParameterException("The port must be greater than zero");
        }
        if (user.length() == 0) {
            throw new SshParameterException("The user name cannot be empty");
        }

        if (keyfileLocation.length() == 0) {
            // use password authentication
            if (authPhrase.length() == 0) {
                throw new SshParameterException("The authentication phrase cannot be empty");
            }
            if (log.isDebugEnabled()) {
                log.debug("Setting up JSCH/SSH connection, password authentication, host='"
                    + host + "', user='" + user + "'");
            }
            UserInfo ui = new UserInfoAdapter(authPhrase);
            jschSession.setUserInfo(ui);
        } else {
            // use keyfile authentication
            keyfileLocation = resolveAndVerifyKeyfilePath(keyfileLocation);
            if (log.isDebugEnabled()) {
                log.debug("Setting up JSCH/SSH connection, keyfile authentication, host='"
                    + host + "', user='" + user + "', keyfile='" + keyfileLocation + "'");
            }
            jsch.addIdentity(keyfileLocation, authPhrase);
        }

        JSch.setLogger(connectionLogger);

        jschSession.connect();
        return jschSession;
    }

    /**
     * Creates an SSH {@link Logger} from a provided Apache Commons Logging instance.
     * 
     * @param apacheCommonsLogger the ACL instance to delegate to
     * @return a new SSH logger delegate
     */
    public static Logger createDelegateLogger(final Log apacheCommonsLogger) {
        return new ACLDelegate(apacheCommonsLogger, Logger.INFO);
    }

    private static String normalizeKeyfilePath(String location) throws SshParameterException {
        // "null" is valid input; treat it like an empty string
        if (location == null) {
            location = "";
        }
        return location.trim();
    }

    private static String resolveAndVerifyKeyfilePath(String location) throws SshParameterException {
        // resolve "~" with home dir
        location = location.replace("~", System.getProperty("user.home"));
        // normalize path
        File sshKeyFile = new File(location);
        location = sshKeyFile.getAbsolutePath();
        // check file existence
        if (!sshKeyFile.isFile()) {
            throw new SshParameterException("SSH keyfile '" + location + "' does not exist");
        }
        return location;
    }
}
