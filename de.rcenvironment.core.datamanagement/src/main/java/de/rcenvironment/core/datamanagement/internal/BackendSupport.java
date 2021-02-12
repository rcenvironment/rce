/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.utils.common.ServiceUtils;

/**
 * Utility class returning backend services.
 * 
 * @author Doreen Seider
 * @author Juergen Klein
 */
public final class BackendSupport {

    private static final String CLOSE_BRACKET = ")";

    private static final String OPEN_BRACKET = "(";

    private static final String EQUALS_SIGN = "=";

    private static final Log LOGGER = LogFactory.getLog(BackendSupport.class);

    private static DataManagementConfiguration dmConfig;

    private static BundleContext bundleContext;

    private static ConfigurationService configurationService;

    private static DataBackend dataBackendService;

    protected void activate(BundleContext context) {
        bundleContext = context;
        // note: disabled for 6.0.0 because at this point, there are no alternative providers anyway
        // dmConfig = configurationService.getConfiguration(context.getBundle().getSymbolicName(), DataManagementConfiguration.class);
        // TODO using default values until reworked or removed
        dmConfig = new DataManagementConfiguration();
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    protected void unbindConfigurationService(ConfigurationService oldConfigurationService) {
        configurationService = ServiceUtils.createFailingServiceProxy(ConfigurationService.class);
    }

    protected void bindDataBackendService(DataBackend newDataBackendService) {
        dataBackendService = newDataBackendService;
    }

    /**
     * @return the configured {@link MetaDataBackendService} to use.
     */
    public static MetaDataBackendService getMetaDataBackend() {

        String metadataProvider = dmConfig.getMetaDataBackend();
        MetaDataBackendService metaDataBackend = null;
        String filterString = OPEN_BRACKET + MetaDataBackendService.PROVIDER + EQUALS_SIGN + metadataProvider + CLOSE_BRACKET;
        ServiceReference<?>[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences(MetaDataBackendService.class.getName(), filterString);
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Failed to get a metadata backend. Invalid protocol filter syntax.");
        }
        if (serviceReferences != null && serviceReferences.length > 0) {
            metaDataBackend = (MetaDataBackendService) bundleContext.getService(serviceReferences[0]);
            if (metaDataBackend == null) {
                throw new IllegalStateException("The configured metadata backend is not available: " + metadataProvider);
            }
        } else {
            throw new IllegalStateException("The configured metadata backend is not available: " + metadataProvider);
        }
        return metaDataBackend;
    }

    /**
     * @param dataURI The {@link URI} of the data the {@link DataBackend} should handle.
     * @return the configured {@link DataBackend} responsible handling the given {@link URI}.
     */
    public static DataBackend getDataBackend(URI dataURI) {

        String scheme = dataURI.getScheme();
        DataBackend dataBackend = null;
        String filterString = OPEN_BRACKET + DataBackend.SCHEME + EQUALS_SIGN + scheme + CLOSE_BRACKET;
        ServiceReference<?>[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences(DataBackend.class.getName(), filterString);
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Failed to get a cdata backend. Invalid protocol filter syntax.");
        }
        if (serviceReferences != null && serviceReferences.length > 0) {
            dataBackend = (DataBackend) bundleContext.getService(serviceReferences[0]);
            if (dataBackend == null) {
                throw new IllegalStateException("A data backend for this scheme is not available: " + scheme);
            }
        } else {
            throw new IllegalStateException("A data backend for this scheme is not available: " + scheme);
        }
        return dataBackend;
    }

    /**
     * @return the configured {@link DataBackend} responsible handling the given {@link DataReferenceType}.
     */
    public static DataBackend getDataBackend() {

        // NOTE:
        // dynamic handling of file data back end removed due to issues on shutting down the data management backend
        // while asynchronously deleting workflow runs. Injected data backend service instead to let osgi handle the stop order.
        //
        // String dataBackendProvider = null;
        //
        // dataBackendProvider = dmConfig.getFileDataBackend();
        //
        // DataBackend dataBackend = null;
        // String filterString = OPEN_BRACKET + DataBackend.PROVIDER + EQUALS_SIGN + dataBackendProvider + CLOSE_BRACKET;
        // ServiceReference<?>[] serviceReferences = null;
        // try {
        // serviceReferences = bundleContext.getServiceReferences(DataBackend.class.getName(), filterString);
        // } catch (InvalidSyntaxException e) {
        // LOGGER.error("Failed to get a data backend. Invalid protocol filter syntax.");
        // }
        // if (serviceReferences != null && serviceReferences.length > 0) {
        // dataBackend = (DataBackend) bundleContext.getService(serviceReferences[0]);
        // if (dataBackend == null) {
        // throw new IllegalStateException("The configured data backend is not available: " + dataBackendProvider);
        // }
        // } else {
        // throw new IllegalStateException("The configured data backend is not available: " + dataBackendProvider);
        // }
        return dataBackendService;
    }

}
