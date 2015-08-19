/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.discovery.server.DiscoveryServerManagementService;
import de.rcenvironment.core.jetty.JettyService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * SOAP/Jetty implementation of {@link DiscoveryServerManagementService}.
 * 
 * @author Robert Mischke
 */
public class DiscoveryServerManagementServiceImpl implements DiscoveryServerManagementService {

    private JettyService jettyService;

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void startServer(String address, int port) {
        String serviceURL = StringUtils.format(DiscoveryConstants.SOAP_SERVICE_URL_PATTERN, address, port);
        logger.info("Starting discovery service at " + serviceURL);
        jettyService.deployWebService(new RemoteDiscoveryServiceImpl(), serviceURL);
    }

    protected void bindJettyService(JettyService newService) {
        this.jettyService = newService;
    }

}
