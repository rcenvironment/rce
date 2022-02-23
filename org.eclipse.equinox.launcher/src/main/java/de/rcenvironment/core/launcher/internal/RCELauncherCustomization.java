/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.launcher.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import de.rcenvironment.core.launcher.api.RCELauncherConstants;

/**
 * As we want to minimize the code changes which have to be performed in the copied org.eclipse.equinox.launcher code, this class is
 * intended to contain as much of the functionality as possible to patch the org.eclipse.equinox.launcher.Main.
 *
 * @author Tobias Rodehutskors
 * @author Robert Mischke
 */
public final class RCELauncherCustomization {

    private static final String CONFIGURATION_FILE_NAME = "logging.xml";

    /**
     * Feature flag: Configure all log4j2 loggers to be asynchronous.
     */
    private static final boolean CONFIGURE_ALL_LOG4J2_LOGGERS_AS_ASYNCHRONOUS = true;

    // non-private for unit test access
    static final String SYSTEM_PROPERTY_KEY_BUNDLE_CONFIGURATION_LOCATION = "bundles.configuration.location";

    // non-private for unit test access
    static final String SYSTEM_PROPERTY_KEY_OSGI_INSTALL_AREA = "osgi.install.area";

    private static final String SYSTEM_PROPERTY_KEY_RCE_INSTANCE_RUN_ID = "rce.instanceRunId";

    private static final String FILE_SCHEMA = "file:";

    // RCE: This system property is used to make sure that the custom RCE launcher was used.
    @Deprecated // testing for the instance run id should cover this already
    private static final String PROP_RCE_LAUNCHER = "de.rcenvironment.launcher";

    // RCE: This system property is used to mark the current launch uniquely. It will be used as a prefix for the startup debug and
    // warnings log.

    /**
     * Error message to be shown if osgi.install.area is configured to an invalid value.
     */
    private static final String ERROR_MESSAGE_OSGI_INSTALL_AREA_MISCONFIGURED = "osgi.install.area is not configured correctly: ";

    // note: assuming all launcher execution is single-threaded, so no synchronization
    private static RCELauncherCustomization RCE_LAUNCHER_CONTEXT;

    // non-static state fields below

    private boolean verboseOutputEnabled = false;

    private final String instanceRunId;

    /**
     * Private constructor to avoid external instantiation.
     */
    private RCELauncherCustomization(String[] args) {
        for (String arg : args) {
            if ("--rce.debug.launcher".equals(arg)) {
                verboseOutputEnabled = true;
                continue;
            }
        }
        instanceRunId = Long.toString(System.currentTimeMillis())
            + "-" + Long.toString(new Random().nextLong() & Long.MAX_VALUE);
    }

    /**
     * @param args the command-line arguments as provided to the main method
     */
    public static void initialize(String[] args) {
        if (RCE_LAUNCHER_CONTEXT != null) {
            throw new IllegalArgumentException(); // prevent double initialization
        }
        RCE_LAUNCHER_CONTEXT = new RCELauncherCustomization(args);

        setRceLauncherMarkerSystemProperty();
        setLauncherVersionAsSystemProperty();
        setInstanceRunIdAsSystemProperty(RCE_LAUNCHER_CONTEXT.instanceRunId);
    }

    public static void hookAfterInitialConfigurationProcessing() {
        // make the bundle.configuration.location absolute AFTER osgi.install.area was set by processing the configuration
        Optional<File> configurationLocation = RCELauncherCustomization.resolveAndStandardizeConfigurationLocation();
        if (configurationLocation.isPresent()) {
            File configurationLocationFile = configurationLocation.get();
            if (verboseOutputEnabled()) {
                System.err.println("Resolved configuration location: " + configurationLocationFile);
            }

            // System.setProperty("user.dir", configurationLocationFile.getAbsolutePath());

            configurePaxLogging(configurationLocationFile);
        } else {
            System.err.println("Failed to resolve configuration location; logging will not be configured properly");
        }

        if (verboseOutputEnabled()) {
            // dump the final system properties after all modifications
            dumpFinalSystemProperties();
        }
    }

    public static String[] rewriteCommandLineArguments(String[] args) {
        if (verboseOutputEnabled()) {
            // dump list of parameters (pre-rewriting)
            System.err.println("Command-line parameters as seen by the launcher JAR:");
            dumpArgumentsArray(args);
        }

        // Define the set of arguments that implicitly suppress the splash screen;
        // created here instead of a constant to slightly reduce memory usage.
        // Created as a Set as looking up each argument should be slightly more efficient, and the code is also cleaner.
        // TODO use Java 11 syntax once available
        final Set<String> implicitNoSplashArguments = new HashSet<>(); // default capacity 16 fits well
        implicitNoSplashArguments.add("--headless");
        implicitNoSplashArguments.add("--server"); // alias for "headless"; added >10.3.0
        implicitNoSplashArguments.add("--configure");
        implicitNoSplashArguments.add("--batch");
        implicitNoSplashArguments.add("--exec");
        implicitNoSplashArguments.add("--shutdown");
        implicitNoSplashArguments.add("--version"); // preparation for future argument; added >10.3.0

        // main rewrite loop
        final List<String> rewrittenArgs = new ArrayList<>(args.length);
        for (String arg : args) {

            // map the custom "--console" alias to the Equinox "-console" flag
            if (arg.equals("--console")) { // note: not case insensitive anymore (changed >10.3.0)
                rewrittenArgs.add("-console");
                continue;
            }

            // to simplify setting RCE system properties, allow setting them as "--rce.xyz=value" (added >10.3.0)
            if (arg.startsWith("--rce.")) {
                String argWithoutDashes = arg.substring(2);
                convertCompositeArgumentIntoSystemProperty(argWithoutDashes);
                continue;
            }

            if (implicitNoSplashArguments.contains(arg)) {
                // Add the Equinox "-nosplash" flag but do NOT exit the flow to preserve the triggering argument.
                // Note: This may result in multiple "-nosplash" arguments, from the launcher code, this should be fine.
                rewrittenArgs.add("-nosplash");
            }

            // no rewrite rule matched -> transfer unmodified
            rewrittenArgs.add(arg);
        }

        // replace args array
        args = rewrittenArgs.toArray(new String[rewrittenArgs.size()]);

        if (verboseOutputEnabled()) {
            // dump list of parameters (post-rewriting)
            System.err.println("Command-line parameters after rewriting:");
            dumpArgumentsArray(args);
        }

        // TODO currently doing nothing; rework the old methods
        return args;
    }

    private static void convertCompositeArgumentIntoSystemProperty(String withoutDashes) {
        String[] parts = withoutDashes.split("=", 2);
        if (parts.length == 2) {
            // key/value pair
            if (verboseOutputEnabled()) {
                System.err.println(
                    "Converted RCE command-line parameter to system property '" + parts[0] + "' with value '" + parts[1] + "'");
            }
            System.setProperty(parts[0], parts[1]);
        } else {
            // flag only
            if (verboseOutputEnabled()) {
                System.err.println(
                    "Converted RCE command-line flag to system property '" + parts[0] + "' with empty value");
            }
            System.setProperty(parts[0], "");
        }
    }

    private static void dumpArgumentsArray(String[] args) {
        for (String arg : args) {
            System.err.println("| " + arg);
        }
    }

    private static void dumpFinalSystemProperties() {
        // convert properties to a sorted map
        @SuppressWarnings({ "unchecked", "rawtypes" }) TreeMap<String, String> sortedMap =
            new TreeMap<>((Map) System.getProperties());
        System.err.println("Final System Properties:");
        sortedMap.forEach((key, value) -> {
            System.err.println("| " + key + " = \"" + value + "\"");
        });
    }

    /**
     * This system property is used later to make sure that the custom RCE launcher was used.
     */
    private static void setRceLauncherMarkerSystemProperty() {
        // TODO this doesn't seem to be used anymore; verify, and potentially remove
        System.getProperties().put(PROP_RCE_LAUNCHER, PROP_RCE_LAUNCHER);
    }

    /**
     * Sets the launcher version as a system property.
     */
    private static void setLauncherVersionAsSystemProperty() {
        // We need to store this property as a String, since PAX Logging crashes if there is a single property that cannot be cast to
        // String.
        System.getProperties().put(RCELauncherConstants.SYSTEM_PROPERTY_KEY_RCE_LAUNCHER_VERSION,
            Integer.toString(RCELauncherConstants.RCE_LAUNCHER_VERSION));
    }

    /**
     * This system property is used to mark the current instance uniquely. It will be used as a prefix for the startup debug and warnings
     * log.
     * 
     * @param instanceRunId the custom, internal launch id; should be unique for each run
     */
    private static void setInstanceRunIdAsSystemProperty(String instanceRunId) {
        System.getProperties().put(SYSTEM_PROPERTY_KEY_RCE_INSTANCE_RUN_ID, instanceRunId);
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
     * @return
     *
     */
    private static Optional<File> resolveAndStandardizeConfigurationLocation() {

        String bundleConfigurationLocation = System.getProperty(SYSTEM_PROPERTY_KEY_BUNDLE_CONFIGURATION_LOCATION);
        if (bundleConfigurationLocation == null) {
            return Optional.empty(); // TODO document: when does this happen? should an error be printed?
        }

        // check if bundle.configuration.location is absolute
        File bundleConfigurationLocationFile = new File(bundleConfigurationLocation);
        boolean absolute = bundleConfigurationLocationFile.isAbsolute();
        if (absolute) {
            return Optional.of(bundleConfigurationLocationFile);
        } else {
            String osgiInstallArea = System.getProperty(SYSTEM_PROPERTY_KEY_OSGI_INSTALL_AREA);
            if (osgiInstallArea == null) {
                System.err.println(ERROR_MESSAGE_OSGI_INSTALL_AREA_MISCONFIGURED + osgiInstallArea);
                return Optional.empty();
            }

            // the launcher stores osgi.install.area as an unescaped file URL format
            File osgiInstallAreaFile;
            if (osgiInstallArea.startsWith(FILE_SCHEMA)) {
                osgiInstallAreaFile = new File(osgiInstallArea.substring(FILE_SCHEMA.length()));
            } else {
                osgiInstallAreaFile = new File(osgiInstallArea);
            }

            if (osgiInstallAreaFile.exists() && osgiInstallAreaFile.isDirectory()) {
                File resolvedBundleConfigurationLocation = new File(osgiInstallAreaFile, bundleConfigurationLocation).getAbsoluteFile();
                System.setProperty(SYSTEM_PROPERTY_KEY_BUNDLE_CONFIGURATION_LOCATION,
                    resolvedBundleConfigurationLocation.getAbsolutePath());
                return Optional.of(resolvedBundleConfigurationLocation);
            } else {
                System.err.println(ERROR_MESSAGE_OSGI_INSTALL_AREA_MISCONFIGURED + osgiInstallArea);
                return Optional.empty();
            }
        }
    }

    private static void configurePaxLogging(File configurationLocationDir) {
        if (!verboseOutputEnabled()) {
            // standard behavior: suppress irrelevant "Enabling XY support" messages on default INFO level;
            // also suppresses the "Error parsing URI" warnings for the "...property.file" value set below
            System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "ERROR");
        } else {
            // verbose behavior: lower threshold from INFO to DEBUG
            System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "DEBUG");
        }

        // define the location in the "common" folder to place temporary startup log files
        File startupLogsDir = new File(System.getProperty("user.home"), ".rce/common/startup_logs");
        startupLogsDir.mkdirs();
        if (!startupLogsDir.isDirectory() && startupLogsDir.canWrite()) {
            throw new IllegalStateException("Startup log directory is not writable: " + startupLogsDir.getAbsolutePath());
        }
        // store this location as a property to be used in the log4j config
        System.setProperty("rce.startupLogsPath", startupLogsDir.getAbsolutePath());

        // set the location of the log4j XML configuration file, relative to the installation directory
        // TODO (p3): evaluate whether this could be loaded from a common place in stanalone and IDE mode
        System.setProperty("org.ops4j.pax.logging.property.file",
            new File(configurationLocationDir, CONFIGURATION_FILE_NAME).getAbsolutePath());

        // Sets the workaround property for CVE-2021-44228 ("log4shell"). While this setting is irrelevant for our
        // own code, as we don't use a vulnerable version of log4j, this protects against (fairly unlikely) situations
        // like a user-installed plugin embedding a vulnerable version of log4j.
        System.setProperty("log4j2.formatMsgNoLookups", "true");

        if (CONFIGURE_ALL_LOG4J2_LOGGERS_AS_ASYNCHRONOUS) {
            // TODO experimental; verify that this causes no problems, especially when switching output locations
            System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        }
    }

    private static boolean verboseOutputEnabled() {
        return RCE_LAUNCHER_CONTEXT.verboseOutputEnabled;
    }
}
