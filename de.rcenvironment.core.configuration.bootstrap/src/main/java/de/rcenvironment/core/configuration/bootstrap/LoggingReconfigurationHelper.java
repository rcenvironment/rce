/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Helper class for reconfiguring the pax logging service.
 *
 * @author Tobias Brieden
 * @author Robert Mischke
 */
public final class LoggingReconfigurationHelper {

    private static final String STAGE_TWO_LOG_APPENDER_CLASS = "de.rcenvironment.core.configuration.logging.EarlyLogInsertingAppender";

    private static final String ERROR_CONFIGURATION_ADMIN_SERVICE_IS_NOT_AVAILABLE =
        "Error while reconfiguring the logging: The Configuration Admin Service is not available.";

    private static final String PROPERTY_KEY_WARNINGS_LOG_DESTINATION = "log4j.appender.WARNINGS_LOG.File";

    private static final String PROPERTY_KEY_DEBUG_LOG_DESTINATION = "log4j.appender.DEBUG_LOG.File";

    private static final Log LOG = LogFactory.getLog(LoggingReconfigurationHelper.class);

    private LoggingReconfigurationHelper() {};

    /**
     * Reconfigures the PAX logging service. The content of the current log files is moved to the new location. All new log messages will be
     * appended to this new log file.
     * 
     * @param basePath Path to the location where the new logfiles should be created.
     * @param logfilesPrefix Prefix that will be added to the name of the logfiles.
     */
    public static void reconfigure(File basePath, String logfilesPrefix) {

        try {

            // We cannot use DS here, since that would delegate the construction of an instance of this class to DS.
            Bundle bundle = FrameworkUtil.getBundle(LoggingReconfigurationHelper.class);
            ConfigurationAdmin configurationAdmin;
            // may be null if executed in a JUnit test
            if (bundle == null) {
                LOG.error(ERROR_CONFIGURATION_ADMIN_SERVICE_IS_NOT_AVAILABLE);
                return;
            } else {
                final BundleContext bundleContext = bundle.getBundleContext();
                // may be null if executed in a Surefire test
                if (bundleContext == null) {
                    LOG.error(ERROR_CONFIGURATION_ADMIN_SERVICE_IS_NOT_AVAILABLE);
                    return;
                }
                String configurationAdminName = ConfigurationAdmin.class.getName();
                ServiceReference<?> ref = bundleContext.getServiceReference(configurationAdminName);
                if (ref == null) {
                    LOG.error(ERROR_CONFIGURATION_ADMIN_SERVICE_IS_NOT_AVAILABLE);
                    return;
                } else {
                    configurationAdmin = (ConfigurationAdmin) bundleContext.getService(ref);
                }
            }

            // use the ConfigurationAdmin to reconfigure the logging
            Configuration configuration = configurationAdmin.getConfiguration("org.ops4j.pax.logging");
            @SuppressWarnings("unchecked") Dictionary<String, Object> properties = configuration.getProperties();

            // determine the location of the startup/early logs for attempting to delete them
            String earlyDebugLogLocation = StrSubstitutor.replaceSystemProperties(properties.get(PROPERTY_KEY_DEBUG_LOG_DESTINATION));
            String earlyWarningsLogLocation = StrSubstitutor.replaceSystemProperties(properties.get(PROPERTY_KEY_WARNINGS_LOG_DESTINATION));

            if (earlyDebugLogLocation == null && earlyWarningsLogLocation == null) {
                // prevent follow-up errors if console-only logging is configured by deleting the log file entries
                LOG.info("Profile log file writing is disabled; only logging to standard output");
                return;
            }

            if (earlyDebugLogLocation == null || earlyWarningsLogLocation == null) {
                // unusual case; may be useful in very specific setups, but is not actively supported at the moment
                LOG.warn("Standard log file writing is partially disabled (warning or debug file, but not both)");
                return;
            }

            // determine the location of the final logs
            String logfilesBasePath = basePath.getAbsolutePath();
            String finalDebugLogLocation = StringUtils.format("%s/%sdebug.log", logfilesBasePath, logfilesPrefix);
            String finalWarningsLogLocation = StringUtils.format("%s/%swarnings.log", logfilesBasePath, logfilesPrefix);

            // reconfigure the logging to append to the renamed startup log files
            properties.put("log4j.appender.DEBUG_LOG", STAGE_TWO_LOG_APPENDER_CLASS);
            properties.put("log4j.appender.DEBUG_LOG.EarlyLogFileLocation", earlyDebugLogLocation); // used to delete the "early" file
            properties.put(PROPERTY_KEY_DEBUG_LOG_DESTINATION, finalDebugLogLocation);
            properties.put("log4j.appender.WARNINGS_LOG", STAGE_TWO_LOG_APPENDER_CLASS);
            properties.put("log4j.appender.WARNINGS_LOG.EarlyLogFileLocation", earlyWarningsLogLocation); // used to delete the "early" file
            properties.put(PROPERTY_KEY_WARNINGS_LOG_DESTINATION, finalWarningsLogLocation);
            configuration.update(properties);

            LOG.debug("Reconfigured the log system for writing to profile-specific log files");
        } catch (IOException e) {
            // If this exception is thrown the logging couln't be reconfigured. Therefore, the startup log is still used to log to.
            LOG.error("Error while switching from early log capture to profile-specific log files", e);
        }
    }
}
