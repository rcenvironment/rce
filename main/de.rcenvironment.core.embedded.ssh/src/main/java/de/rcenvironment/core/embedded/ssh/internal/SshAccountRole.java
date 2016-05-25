/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;

/**
 * Class to describe the different roles and their privileges.
 * 
 * @author Sebastian Holtappels
 */
public final class SshAccountRole {

    private String roleName;

    private List<String> allowedCommandPatterns;

    private List<String> allowedScpPath;

    private String allowedCommandRegEx = null;

    public SshAccountRole() {}

    public SshAccountRole(String roleName, List<String> allowedCommandPatterns, List<String> allowedScpPath) {
        this.roleName = roleName;
        this.allowedCommandPatterns = allowedCommandPatterns;
        this.allowedScpPath = allowedScpPath;
    }

    /**
     * 
     * Method to validate the Console Role.
     * 
     * @param logger the logger to be used to log
     * @return true if valid else false
     */
    public boolean validateRole(Log logger) {
        boolean isValid = true;
        // role name is not null
        if (roleName == null) {
            isValid = false;
        }
        // role has privileges
        if (allowedCommandPatterns == null || allowedCommandPatterns.isEmpty()) {
            logger.warn("Allowed command patterns must not be empty. (Role + " + roleName + ")");
            isValid = false;
        } else {
            // no privilege is null or an empty string
            for (String pattern : allowedCommandPatterns) {
                if (pattern == null || pattern.isEmpty()) {
                    logger.warn("Found empty \"allowed command pattern\" for role " + roleName);
                    isValid = false;
                } else {
                    try {
                        Pattern.compile(pattern);
                    } catch (PatternSyntaxException e) {
                        logger.warn("Allowed command pattern " + pattern + " for role " + roleName
                            + " is not a valid regular expression.");
                        isValid = false;
                    }
                }
            }
        }
        if (allowedScpPath != null && !allowedScpPath.isEmpty()) {
            for (String path : allowedScpPath) {
                if (path == null || path.isEmpty()) {
                    logger.warn("Found empty \"allowed scp path\" for role " + roleName);
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getAllowedCommandPatterns() {
        return allowedCommandPatterns;
    }

    public void setAllowedCommandPatterns(List<String> allowedCommandPatterns) {
        this.allowedCommandPatterns = allowedCommandPatterns;
    }

    public List<String> getAllowedScpPath() {
        return allowedScpPath;
    }

    public void setAllowedScpPath(List<String> allowedScpPath) {
        this.allowedScpPath = allowedScpPath;
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
