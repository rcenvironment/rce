/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Class with utility methods for the SSH console test classes.
 * 
 * @author Sebastian Holtappels
 */
public final class SshTestUtils {

    private static final int DEFAULT_TEST_PORT = 31005;

    private static final String NOT_RESTRICTED_ROLE = "no_restrictions";

    private static final String RESTRICTED_ROLE = "restricted!";

    private SshTestUtils() {};

    /**
     * 
     * Get a valid configuration for test cases.
     * 
     * @return a valid configuration
     */
    public static SshConfiguration getValidConfig() {
        SshConfiguration configuration = new SshConfiguration();
        configuration.setPort(DEFAULT_TEST_PORT);
        configuration.setEnabled(true);

        // add user
        configuration.setAccounts(getValidUsers());

        // add roles
        configuration.setRoles(getValidRoles());

        return configuration;
    }

    /**
     * 
     * Get a list of valid roles...
     * 
     * @return a list of valid roles
     */
    public static List<SshAccountRole> getValidRoles() {
        List<SshAccountRole> roles = new ArrayList<SshAccountRole>();
        List<String> allowedCommandPatterns = new ArrayList<String>();
        List<String> allowedScpPath = new ArrayList<String>();

        // add not restricted role
        allowedCommandPatterns.add(".*");
        allowedScpPath.add("/tmp/");
        allowedScpPath.add("/etc/");
        roles.add(new SshAccountRole(NOT_RESTRICTED_ROLE, allowedCommandPatterns, allowedScpPath));

        // add restricted role
        allowedCommandPatterns = new ArrayList<String>();
        allowedScpPath = new ArrayList<String>();

        allowedCommandPatterns.add("stats");
        allowedCommandPatterns.add("tasks");
        allowedCommandPatterns.add("net( .+)?");
        allowedScpPath.add("/tmp/rce/");
        roles.add(new SshAccountRole(RESTRICTED_ROLE, allowedCommandPatterns, allowedScpPath));
        return roles;
    }

    public static SshAccountRole getValidRole() {
        return getValidRoles().get(0);
    }

    /**
     * 
     * Get a list of valid users...
     * 
     * @return a list of valid users
     */
    public static List<SshAccountImpl> getValidUsers() {
        List<SshAccountImpl> users = new ArrayList<SshAccountImpl>();
        users.add(new SshAccountImpl("admin", "adminadmin", null, null, NOT_RESTRICTED_ROLE));
        users.add(new SshAccountImpl("developer", "developerdeveloper", null, null, NOT_RESTRICTED_ROLE));
        users.add(new SshAccountImpl("user", "useruser", null, null, RESTRICTED_ROLE));
        // Testing some special characters and numbers
        users.add(new SshAccountImpl("_1€üöä()!.:,;_-<>[]{}", "_2$1()!.:,;_€üöä-<>[]{}", null, null, RESTRICTED_ROLE));
        users.add(new SshAccountImpl("publicKeyTester", "", null, "1267576523765125612", RESTRICTED_ROLE));
        users.add(new SshAccountImpl("default", "default", null, null, RESTRICTED_ROLE));
        return users;
    }

    public static SshAccountImpl getValidUser() {
        return getValidUsers().get(0);
    }
}
