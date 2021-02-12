/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple static API for querying whether the current runtime is a live OSGi application environment, or a test execution environment.
 * When necessary, a distinction between OSGi integration test environments, or a plain Java environments could be added, too. For now, the
 * latter is always assumed to be non-OSGi JUnit execution, so it is considered a test environment too.
 * 
 * @author Robert Mischke
 *
 */
public final class RuntimeDetection {

    private static final String ECLIPSE_COMMANDS_SYSTEM_PROPERTY = "eclipse.commands";

    private static final String ECLIPSE_APPLICATION_SYSTEM_PROPERTY = "eclipse.application";

    private static final String SUREFIRE_APPLICATION_IDENTIFIER = "org.eclipse.tycho.surefire.osgibooter.headlesstest";

    // I didn't want to introduce an Enum class for such a small feature, and enum safety is not relevant here -- misc_ro

    private static final String APPLICATION = "application (default)";

    private static final String OSGI_SUREFIRE_TEST = "OSGi Tycho/Surefire test";

    private static final String ECLIPSE_PLUGIN_TEST = "Eclipse \"JUnit Plug-In\" test";

    private static final String PLAIN_JAVA_TEST = "plain Java (assuming JUnit)";

    // the default singleton
    private static final RuntimeDetection INSTANCE = new RuntimeDetection();

    private final String environment = detectEnvironment();

    private boolean simulatedServiceActivationAllowed; // synchronized on #INSTANCE

    private RuntimeDetection() {
        getTransientLogger().debug("Detected " + environment + " environment");
    }

    private String detectEnvironment() {

        final EclipseLaunchParameters launchParameters = EclipseLaunchParameters.getInstance();

        if (launchParameters.containsToken("-testproperties")) { // clear indicator of tycho-surefire via Maven
            // consistency check
            String applicationParameter;
            try {
                applicationParameter = launchParameters.getNamedParameter("-application", "-application");
            } catch (ParameterException e) {
                applicationParameter = "<" + e.toString() + ">";
            }
            // TODO sort out "-application" token filtering, then re-enable
            if (false && !SUREFIRE_APPLICATION_IDENTIFIER.equals(applicationParameter)) {
                getTransientLogger()
                    .warn("Detected " + OSGI_SUREFIRE_TEST + " environment, but found application property value " + applicationParameter);
            }
            return OSGI_SUREFIRE_TEST;
        }

        if (launchParameters.containsToken("-testLoaderClass")) {
            return ECLIPSE_PLUGIN_TEST;
        }

        if (System.getProperty(ECLIPSE_COMMANDS_SYSTEM_PROPERTY) != null) {
            return APPLICATION;
        } else {
            return PLAIN_JAVA_TEST;
        }
    }

    /**
     * @return true if either running in an OSGi (Tycho-Surefire) integration test environment, or in a plain Java Unit environment
     */
    public static boolean isTestEnvironment() {

        return INSTANCE.environment != APPLICATION;
    }

    public static void allowSimulatedServiceActivation() {
        synchronized (INSTANCE) {
            INSTANCE.simulatedServiceActivationAllowed = true;
        }
    }

    /**
     * @return true if {@link #isTestEnvironment()} is true AND mock service activation has not been allowed via
     *         {@link #allowSimulatedServiceActivation()} yet.
     */
    public static boolean isImplicitServiceActivationDenied() {
        if (INSTANCE.environment == APPLICATION) {
            return false;
        } else {
            synchronized (INSTANCE) {
                return !INSTANCE.simulatedServiceActivationAllowed;
            }
        }
    }

    private Log getTransientLogger() {
        // do not keep a reference
        return LogFactory.getLog(getClass());
    }
}
