/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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

    private String allowedCommandRegEx = null;

    public SshAccountRole(String roleName) {
        this.roleName = roleName;
        this.allowedCommandPatterns = new ArrayList<String>();

        // Set allowed command patterns for role name
        switch (roleName) {
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_USER:
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_USER_ALIAS:
            allowedCommandPatterns.add("ra .*"); // Note: the space is important for security - do not remove it!
            allowedCommandPatterns.add(SshConstants.COMMAND_PATTERN_SYSMON);
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
            allowedCommandPatterns.add("wf details");
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
            break;
        case SshConstants.ROLE_NAME_IM_ADMIN:
            allowedCommandPatterns.add("im.*");
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
            break;
        case SshConstants.ROLE_NAME_DEVELOPER:
            allowedCommandPatterns.add(".*");
            break;
        case SshConstants.ROLE_NAME_DEFAULT:
            break;
        default:
            this.roleName = SshConstants.ROLE_NAME_DEFAULT;
            LogFactory.getLog(getClass()).warn("Tried to create a role with a name that is not allowed: " + roleName);
        }

        // allow any user to execute the "dummy" command (e.g. for testing connectivity from scripts)
        allowedCommandPatterns.add("dummy");
    }

    public String getRoleName() {
        return roleName;
    }

    /**
     * 
     * Getter for allowedCommandRegEx.
     * 
     * @return An regular expression for allowed command
     */
    public String getAllowedCommandRegEx() {
        if (allowedCommandRegEx == null) {
            allowedCommandRegEx = SshConstants.DEFAULT_COMMANDS;
            for (String pattern : allowedCommandPatterns) {
                allowedCommandRegEx = allowedCommandRegEx + "|(" + pattern.trim() + ")";
            }
        }
        return allowedCommandRegEx;
    }

    @Override
    public int hashCode() {
        return roleName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj != null) {
            if (obj instanceof SshAccountRole) {
                SshAccountRole other = (SshAccountRole) obj;
                result = roleName.equals(other.getRoleName());
            }
        }
        return result;
    }

}
