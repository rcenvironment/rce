/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.logging.PaxLoggingService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Helper class for (re-)configuring the pax logging service.
 * 
 * @author Robert Mischke
 */
public final class LogSystemConfigurator {

    /**
     * The system property key that is defined if the log file relocation process was successful. There is no specific value, and it will
     * usually be an empty string. This property is also set if no relocation was actually required, e.g. when file logging is disabled.
     */
    public static final String SYSTEM_PROPERTY_KEY_RELOCATION_SUCCESSFUL = "rce.logFileRelocationSuccessful";

    private static final String RECONFIGURATION_TARGET_CLASS = "de.rcenvironment.core.configuration.logging.SelectableLocationFileAppender";

    private static final String STATIC_RECONFIGURATION_METHOD = "configureFinalLogEnvironment";

    private static final String STATIC_FINALIZATION_METHOD = "finalizeLogRelocationForAllAppenders";

    // hold the BundleContext as a field to allow injection via constructor if necessary
    private final BundleContext bundleContext;

    private Log log = LogFactory.getLog(getClass());

    // default constructor: implicitly fetch the BundleContext for its own class
    public LogSystemConfigurator() {
        bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        if (bundleContext == null) {
            throw new IllegalStateException("Failed to acquire a BundleContext for class " + this.getClass());
        }
    }

    /**
     * Reconfigures the logging setup. The content of the current log files is moved to the new location. All new log messages will be
     * appended to this new log file.
     * 
     * @param baseDirectory the directory that the new log files should be created in
     * @param filenamePrefix a prefix that will be added to the name of the log files
     */
    public void relocateLogFilesToProfileDirectory(File baseDirectory, String filenamePrefix) {

        log.debug("Reconfiguring the log system with final output directory " + baseDirectory.getAbsolutePath()
            + " and log filename prefix '" + filenamePrefix + "'");

        if (!configureFinalLogEnvironmentViaReflection(baseDirectory, filenamePrefix)) {
            throw new IllegalStateException("Failed to reconfigure the log system for writing to profile-specific log files");
        }
        boolean success;
        try {
            success = attemptToTriggerPaxLog4j2Reconfiguration();
        } catch (RuntimeException e) {
            log.error("Error while attempting to reconfigure the log system for writing to profile-specific log files", e);
            return;
        }

        if (success) {
            finalizeRelocationViaReflection();

            // only log this message on success
            log = LogFactory.getLog(getClass()); // should usually not be necessary, but make sure that the logger is current
            log.debug("Successfully reconfigured the log system with final output directory " + baseDirectory.getAbsolutePath()
                + " and log filename prefix '" + filenamePrefix + "'");

            System.setProperty(SYSTEM_PROPERTY_KEY_RELOCATION_SUCCESSFUL, ""); // no value; see JavaDoc
        }
    }

    private boolean attemptToTriggerPaxLog4j2Reconfiguration() {
        boolean success = false;

        // This reflection-based approach is mostly based on inspecting what the activator of the Pax-Logging log4j2
        // bundle (org.ops4j.pax.logging.log4j2.internal.Activator) does do initialize and reconfigure the log4j2
        // backend in the first place. If anything breaks, e.g. after upgrading Pax-Logging, this activator should
        // also be the first place to look for changes. -- misc_ro, Feb 2022

        final String registeredServiceName = PaxLoggingService.class.getName();
        ServiceReference<?>[] refs;
        try {
            refs = bundleContext.getAllServiceReferences(registeredServiceName, null);
            if (refs == null) {
                throw new IllegalStateException("Found no registered OSGi service for " + registeredServiceName
                    + "; was pax-logging properly initialized?");
            }
            for (ServiceReference<?> ref : refs) {
                Object service = bundleContext.getService(ref);

                if (!service.getClass().getName()
                    .equals("org.ops4j.pax.logging.log4j2.internal.PaxLoggingServiceImpl$1ManagedPaxLoggingService")) {
                    log.debug("Ignoring other registered PaxLoggingService service of class " + service.getClass());
                    continue;
                }

                try {
                    // We have received an instance of the inner class "ManagedPaxLoggingService" produced by the
                    // PaxLoggingServiceImpl acting as an OSGi ServiceFactory via its #getService() method. The relevant
                    // method #updated(), however, is not provided by this inner class, but the outer one. Therefore,
                    // we need to fetch the outer instance from the inner instance via its implicit "this$0". Needless to
                    // say, this breaks a lot of encapsulation, and may break when pax-logging code is refactored. The
                    // general approach, however, should be stable as the #update() method is quite essential. -- misc_ro
                    Field outerInstanceField = service.getClass().getDeclaredField("this$0");

                    // This field only has package access, so we need to make it accessible via reflection before fetching
                    outerInstanceField.setAccessible(true);
                    Object outerInstance = outerInstanceField.get(service);

                    // Test the assumption about the outer instance's type
                    final String expectedOuterClassName = "org.ops4j.pax.logging.log4j2.internal.PaxLoggingServiceImpl";
                    final String actualOuterClassName = outerInstance.getClass().getName();
                    if (!actualOuterClassName.equals(expectedOuterClassName)) {
                        throw new IllegalStateException("Expected to find an outer class of type " + expectedOuterClassName + ", but found "
                            + actualOuterClassName + " instead");
                    }

                    // Now call the method of interest, #update(Dictionary<String,?>, via reflection
                    Method method = outerInstance.getClass().getDeclaredMethod("updated", Dictionary.class);
                    // This method also has package access so it must be made accessible as well
                    method.setAccessible(true);
                    // Invoke the method with a "null" argument (like the activator)
                    method.invoke(outerInstance, (Dictionary<String, ?>) null);

                    success = true;
                    break;
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchFieldException e) {
                    log.error("Failed to perform reflection operation", e);
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Failed to fetch the list of OSGi services for " + registeredServiceName, e);
        }
        return success;
    }

    /**
     * Invokes a static method using reflection to trigger the relocation of all log output files.
     * 
     * The reason that reflection is used is that PDE (Eclipse's OSGi tooling) does not allow package imports that were exported from
     * fragment bundle unless the "Eclipse-ExtensibleAPI" header is declared on the host bundle, which is not possible here as this is
     * pax-logging. This is purely an IDE restriction; the OSGi runtime should allow these import-export pairs. However, as this would cause
     * constant compilation errors in the IDE, reflection is used, which is resolved like a normal code reference by the OSGi classloaders,
     * and does not cause such errors, at the cost of being slightly brittle. -- misc_ro, Jan 2022
     * 
     * IMPORTANT: While this class does not use any class from the de.rcenvironment.core.configuration.logging package, that package must
     * still be declared as an OSGi Import-Package entry in this bundle's manifest for access via reflection to work!
     * 
     * @param baseDirectory the base directory that should contain the new log output files
     * @param filenamePrefix an optional prefix for the relocation log file names (can be null as an alias for the empty string)
     * @return true if relocation was successful; false on error (which was already printed to StdErr and attempted to be logged)
     */
    private boolean configureFinalLogEnvironmentViaReflection(File baseDirectory, String filenamePrefix) {
        try {
            Class<?> classProvidingStaticTriggerMethod = Class.forName(RECONFIGURATION_TARGET_CLASS);
            Method staticMethod =
                classProvidingStaticTriggerMethod.getMethod(STATIC_RECONFIGURATION_METHOD, File.class, String.class);
            staticMethod.invoke(null, baseDirectory, filenamePrefix);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
            String baseMessage = "Error while reconfiguring the log system to use profile-specific log files in " + baseDirectory;
            System.err.println(baseMessage + ": " + e.toString());
            // also attempt to document this event in the old log files
            log.error(baseMessage, e);
            return false;
        }
    }

    private boolean finalizeRelocationViaReflection() {
        try {
            Class<?> classProvidingStaticTriggerMethod = Class.forName(RECONFIGURATION_TARGET_CLASS);
            Method staticMethod = classProvidingStaticTriggerMethod.getMethod(STATIC_FINALIZATION_METHOD);
            staticMethod.invoke(null);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
            String baseMessage = "Error while finalizing the reconfiguration from early log files to profile-specific ones";
            System.err.println(baseMessage + ": " + e.toString());
            // also attempt to document this event in the old log files
            log.error(baseMessage, e);
            return false;
        }
    }

}
