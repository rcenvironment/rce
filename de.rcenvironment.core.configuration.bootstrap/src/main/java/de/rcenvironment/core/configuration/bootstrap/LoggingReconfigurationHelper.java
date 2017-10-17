/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
 */
public final class LoggingReconfigurationHelper {

    private static final String ERROR_CONFIGURATION_ADMIN_SERVICE_IS_NOT_AVAILABLE =
        "Error while reconfiguring the logging: The Configuration Admin Service is not available.";

    private static final String LOG4J_APPENDER_WARNINGS_LOG_FILE = "log4j.appender.WARNINGS_LOG.File";

    private static final String LOG4J_APPENDER_DEBUG_LOG_FILE = "log4j.appender.DEBUG_LOG.File";

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
            Dictionary<String, String> properties = configuration.getProperties();

            // determine the location of the startup logs
            String startupDebugLogLocation = StrSubstitutor.replaceSystemProperties(properties.get(LOG4J_APPENDER_DEBUG_LOG_FILE));
            String startupWarningsLogLocation = StrSubstitutor.replaceSystemProperties(properties.get(LOG4J_APPENDER_WARNINGS_LOG_FILE));

            // determine the location of the final logs
            String logfilesBasePath = basePath.getAbsolutePath();
            String finalDebugLogLocation = StringUtils.format("%s/%sdebug.log", logfilesBasePath, logfilesPrefix);
            String finalWarningsLogLocation = StringUtils.format("%s/%swarnings.log", logfilesBasePath, logfilesPrefix);

            // reconfigure the logging to append to the renamed startup log files
            properties.put("log4j.appender.DEBUG_LOG", "de.rcenvironment.core.configuration.logging.InsertOldLogAppender");
            properties.put("log4j.appender.DEBUG_LOG.OldFile", startupDebugLogLocation);
            properties.put(LOG4J_APPENDER_DEBUG_LOG_FILE, finalDebugLogLocation);
            properties.put("log4j.appender.WARNINGS_LOG", "de.rcenvironment.core.configuration.logging.InsertOldLogAppender");
            properties.put("log4j.appender.WARNINGS_LOG.OldFile", startupWarningsLogLocation);
            properties.put(LOG4J_APPENDER_WARNINGS_LOG_FILE, finalWarningsLogLocation);
            configuration.update(properties);
        } catch (IOException e) {
            // If this exception is thrown the logging couln't be reconfigured. Therefore, the startup log is still used to log to.
            LOG.error("Error while reconfiguring the logging.", e);
        }
    }
}
