/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package org.eclipse.equinox.launcher;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * As we want to minimize the code changes which have to be performed in the copied org.eclipse.equinox.launcher code, this class is
 * intended to contain as much of the functionality as possible to patch the org.eclipse.equinox.launcher.Main.
 *
 * @author Tobias Rodehutskors
 */
public final class RCELauncherHelper {

    // RCE: This system property is used to make sure that the custom RCE launcher was used.
    private static final String PROP_RCE_LAUNCHER = "de.rcenvironment.launcher";

    private static final String PROP_BUNDLE_CONFIGURATION_LOCATION = "bundles.configuration.location";

    private static final String PROP_OSGI_INSTALL_AREA = "osgi.install.area";

    /**
     * List of commands which implicitly suppress the splash screen.
     */
    private static String[] implicitNoSplashArguments = new String[] { "--headless", "--configure", "--batch", "--exec", "--shutdown" };

    /**
     * Error message to be shown if osgi.install.area is configured to an invalid value.
     */
    private static final String OSGI_INSTALL_AREA_MISCONFIGURED = "osgi.install.area is not configured correctly: ";

    /**
     * Private constructor to avoid instantiation.
     */
    private RCELauncherHelper() {

    }

    /**
     * Returns true, if the the splash screen should not be shown, since the given argument implicitly determines a nosplash/headless
     * start.
     * 
     * @param arg The argument that should be checked.
     * @return True, if arg is an argument which indicates a nosplash/headless start.
     */
    public static boolean implicitNoSplash(String arg) {

        for (String implicitNoSplashArgument : implicitNoSplashArguments) {
            if (implicitNoSplashArgument.equalsIgnoreCase(arg)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This system property is used later to make sure that the custom RCE launcher was used.
     *
     */
    public static void setSystemPropertyToMarkCustomLaunch() {
        System.getProperties().put(PROP_RCE_LAUNCHER, PROP_RCE_LAUNCHER);
    }

    /**
     * As stated on https://ops4j1.jira.com/browse/CONFMAN-12, the bundles.configuration.location path is relative to the execution
     * directory, or absolute. There is no support for it being in relation to one of the osgi locations. This is problematic if RCE was not
     * started from its installation directory, since the configuration folder cannot be resolved correctly in this case.
     * 
     * To fix this problem, this function makes the bundles.configuration.location path absolute. It resolves the relative
     * bundles.configuration.location path against osgi.install.area. If the bundles.configuration.location path is already absolute, or
     * either the bundles.configuration.location property or the osgi.install.area is null, this function does nothing.
     *
     */
    public static void setConfigurationLocationAbsolute() {

        String bundleConfigurationLocation = System.getProperty(PROP_BUNDLE_CONFIGURATION_LOCATION);
        if (bundleConfigurationLocation == null) {
            return;
        }

        // check if bundle.configuration.location is absolute
        boolean absolute = new File(bundleConfigurationLocation).isAbsolute();
        if (!absolute) {

            String osgiInstallArea = System.getProperty(PROP_OSGI_INSTALL_AREA);
            if (osgiInstallArea == null) {
                System.err.println(OSGI_INSTALL_AREA_MISCONFIGURED + osgiInstallArea);
                return;
            }

            URI osgiInstallAreaUri;
            try {
                // the launcher stores osgi.install.area in URI format
                osgiInstallAreaUri = new URI(osgiInstallArea);
                File osgiInstallAreaFile = new File(osgiInstallAreaUri);
                if (osgiInstallAreaFile.exists() && osgiInstallAreaFile.isDirectory()) {
                    System.setProperty(PROP_BUNDLE_CONFIGURATION_LOCATION,
                        new File(osgiInstallAreaFile, bundleConfigurationLocation).getAbsolutePath());
                } else {
                    System.err.println(OSGI_INSTALL_AREA_MISCONFIGURED + osgiInstallArea);
                }
            } catch (URISyntaxException e) {
                System.err.println(OSGI_INSTALL_AREA_MISCONFIGURED + osgiInstallArea);
            }
        }
    }

    /**
     * Replaces the --console option with the -console option.
     * 
     * @param args The argument array.
     * @param i The index of the argument in the argument array that should be checked for the --console option.
     */
    public static void replaceDoubleDashConsoleOption(String[] args, int i) {

        if (args[i].equalsIgnoreCase("--console")) {
            args[i] = "-console";
        }
    }
}
