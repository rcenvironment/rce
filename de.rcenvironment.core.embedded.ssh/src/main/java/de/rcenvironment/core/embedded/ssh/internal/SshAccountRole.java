/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

/**
 * Class to describe the different roles and their privileges.
 * 
 * @author Sebastian Holtappels
 * @author Brigitte Boden
 */
public final class SshAccountRole {

    private String roleName;

    private List<String> allowedCommandPatterns;

    private List<String> disallowedCommandPatterns;

    private String allowedCommandRegEx = null;

    private String disallowedCommandRegEx = null;
    
    private boolean allowedToOpenShell = true;
    
    private boolean allowedToUseUplink = false;

    public SshAccountRole(String roleName) {
        this.roleName = roleName;
        this.allowedCommandPatterns = new ArrayList<>();
        this.disallowedCommandPatterns = new ArrayList<>();

        // Set allowed command patterns for role name
        switch (roleName) {
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_USER:
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_USER_ALIAS:
            // Note: the space is important for security, as it prevents the user from executing ra-admin - do not remove it!
            allowedCommandPatterns.add("ra .*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_SYSMON);
            allowedToUseUplink = true;
            break;
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_ADMIN:
            allowedCommandPatterns.add("ra.*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_SYSMON);
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_COMPONENTS);
            break;
        case SshConstants.ROLE_NAME_WORKFLOW_OBSERVER:
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_COMPONENTS);
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_NET_INFO);
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_SYSMON);
            allowedCommandPatterns.add("wf list");
            allowedCommandPatterns.add("wf details.*");
            allowedCommandPatterns.add("wf");
            break;
        case SshConstants.ROLE_NAME_WORKFLOW_ADMIN:
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_COMPONENTS);
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_NET_INFO);
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_SYSMON);
            allowedCommandPatterns.add("wf.*");
            break;
        case SshConstants.ROLE_NAME_LOCAL_ADMIN:
            allowedCommandPatterns.add("cn.*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_COMPONENTS);
            allowedCommandPatterns.add("mail.*");
            allowedCommandPatterns.add("net.*");
            allowedCommandPatterns.add("restart");
            allowedCommandPatterns.add("shutdown");
            allowedCommandPatterns.add("stop");
            allowedCommandPatterns.add("stats");
            allowedCommandPatterns.add("tasks.*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_AUTH);
            break;
        case SshConstants.ROLE_NAME_IM_ADMIN:
            allowedCommandPatterns.add("im.*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_AUTH);
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_NET_INFO);
            break;
        case SshConstants.ROLE_NAME_IM_DELEGATE:
            allowedCommandPatterns.add("cn.*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_COMPONENTS);
            allowedCommandPatterns.add("net.*");
            allowedCommandPatterns.add("restart");
            allowedCommandPatterns.add("shutdown");
            allowedCommandPatterns.add("stop");
            allowedCommandPatterns.add("stats");
            allowedCommandPatterns.add("tasks.*");
            allowedCommandPatterns.add("wf.*");
            allowedCommandPatterns.add("ra-admin.*");
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_AUTH);
            break;
        case SshConstants.ROLE_NAME_DEVELOPER:
            allowedCommandPatterns.add(".*");
            break;
        case SshConstants.ROLE_NAME_UPLINK_CLIENT:
            allowedToOpenShell = false;
            allowedToUseUplink = true;
            break;
        case SshConstants.ROLE_NAME_DEFAULT:
            break;
        default:
            this.roleName = SshConstants.ROLE_NAME_DEFAULT;
            LogFactory.getLog(getClass()).warn("Tried to create a role with a name that is not allowed: " + roleName);
        }

        // allow any user to execute the "dummy" command (e.g. for testing connectivity from scripts)
        allowedCommandPatterns.add("dummy");

        // Prevent all users except developers to execute "wf open" remotely, as this may influence the GUI of the instance the remote user
        // is connected to.
        if (!SshConstants.ROLE_NAME_DEVELOPER.equals(roleName)) {
            disallowedCommandPatterns.add("wf open.*");
        }
    }

    
    public boolean isAllowedToUseUplink() {
        return allowedToUseUplink;
    }

    public String getRoleName() {
        return roleName;
    }

    /**
     * Due to the special status of wf open (which may only be executed by developers), it is insufficient to check the given command
     * against the regular expression returned by this method in order to determine whether the role indeed allows the user to execute the
     * command. One furthermore has to check against the ``black list'' of commands that the user's role explicitly bars them from
     * executing, which can be obtained via getDisallowedCommandRegEx().
     * 
     * @return A regular expression that matches a command only if the given role may execute the given command.
     */
    public String getAllowedCommandRegEx() {
        if (allowedCommandRegEx == null) {
            final StringBuilder regExBuilder = new StringBuilder(SshConstants.DEFAULT_COMMANDS);
            for (String pattern : allowedCommandPatterns) {
                regExBuilder.append(String.format("|(%s)", pattern.trim()));
            }
            allowedCommandRegEx = regExBuilder.toString();
        }
        return allowedCommandRegEx;
    }

    /**
     * Due to the special status of wf open (which may only be executed by developers), it is insufficient to check the given command
     * against the regular expression returned by this method in order to determine whether the role indeed allows the user to execute the
     * command. One furthermore has to check against the ``white list'' of commands that the user's role explicitly allows them to execute,
     * which can be obtained via getAllowedCommandRegEx().
     * 
     * @return A regular expression that matches a command only if the given role may not execute the given command.
     */
    public String getDisallowedCommandRegEx() {
        if (disallowedCommandRegEx == null) {
            final StringBuilder regExBuilder = new StringBuilder();
            for (String pattern : disallowedCommandPatterns) {
                regExBuilder.append(String.format("|(%s)", pattern.trim()));
            }
            disallowedCommandRegEx = regExBuilder.toString();
        }
        return disallowedCommandRegEx;
    }
    
    public boolean isAllowedToOpenShell() {
        return allowedToOpenShell;
    }

    @Override
    public int hashCode() {
        return roleName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SshAccountRole) {
            SshAccountRole other = (SshAccountRole) obj;
            return roleName.equals(other.getRoleName());
        } else {
            return false;
        }
    }

}
