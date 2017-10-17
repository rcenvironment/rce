/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryBootstrapService;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryClientSetup;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryConfiguration;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryServerSetup;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryClientSetup.ServerEntry;
import de.rcenvironment.core.configuration.discovery.client.DiscoveryClientService;
import de.rcenvironment.core.configuration.discovery.server.DiscoveryServerManagementService;

/**
 * Implementation of {@link DiscoveryBootstrapService}.
 * 
 * @author Robert Mischke
 * 
 */
public class DiscoveryBootstrapServiceImpl implements DiscoveryBootstrapService {

    private static String symbolicBundleName;

    private DiscoveryClientService discoveryClientService;

    private DiscoveryServerManagementService discoveryServerManagementService;

    private final Log logger = LogFactory.getLog(getClass());

    public static void setSymbolicBundleName(String symbolicName) {
        DiscoveryBootstrapServiceImpl.symbolicBundleName = symbolicName;
    }

    @Override
    public String getSymbolicBundleName() {
        if (symbolicBundleName == null) {
            throw new NullPointerException();
        }
        return symbolicBundleName;
    }

    @Override
    public Map<String, String> initializeDiscovery(DiscoveryConfiguration configuration) {
        Map<String, String> receivedDiscoveryProperties = new HashMap<String, String>();

        DiscoveryServerSetup discoveryServerSetup = configuration.getRunDiscoveryServer();
        if (discoveryServerSetup != null) {
            startDiscoveryServer(discoveryServerSetup);
        } else {
            logger.info("Not running a discovery server as the relevant setting is empty");
        }

        DiscoveryClientSetup discoveryClientSetup = configuration.getUseDiscovery();
        if (discoveryClientSetup != null) {
            String reflectedClientAddress = getExternalHostAdressFromDiscovery(discoveryClientSetup);
            if (reflectedClientAddress != null) {
                // add the determined client address to the returned property map
                // the default property is wrapped in quotes; add a "raw" property if required
                receivedDiscoveryProperties.put(QUOTED_REFLECTED_CLIENT_ADDRESS_PROPERTY,
                    "\"" + reflectedClientAddress + "\"");
            } else {
                // TODO right now, a failed address reflection triggers the "fallback properties";
                // this should be handled more generally when remote property sending
                // (from the discovery server) is added
                Map<String, String> fallbackProperties = discoveryClientSetup.getFallbackProperties();
                if (fallbackProperties.size() != 0) {
                    logger.info("Failed to determine external address from discovery server; using fallback properties");
                    receivedDiscoveryProperties.putAll(fallbackProperties);
                }
            }
        }

        return receivedDiscoveryProperties;
    }

    protected void bindDiscoveryClientService(DiscoveryClientService newService) {
        this.discoveryClientService = newService;
    }

    protected void bindDiscoveryServerManagementService(DiscoveryServerManagementService newService) {
        this.discoveryServerManagementService = newService;
    }

    private void startDiscoveryServer(DiscoveryServerSetup discoveryServerSetup) {
        try {
            discoveryServerManagementService.startServer(discoveryServerSetup.getBindAddress(), discoveryServerSetup.getPort());
        } catch (RuntimeException e) {
            logger.error("Failed to start local discovery service", e);
        }
    }

    private String getExternalHostAdressFromDiscovery(DiscoveryClientSetup discoveryClientSetup) {
        List<ServerEntry> servers = discoveryClientSetup.getServers();
        if (servers.size() == 0) {
            return null;
        }
        // TODO implement multi-host discovery when needed
        if (servers.size() > 1) {
            logger.warn("There is more than one discovery server configured, "
                + "but multi-host discovery is not implemented yet. Only the first server will be used.");
        }
        ServerEntry server = servers.get(0);

        try {
            final String reflectedExternalAddress =
                discoveryClientService.getReflectedIpFromDiscoveryServer(
                    server.getAddress(), server.getPort());
            logger.info("IP address as reported by discovery service: " + reflectedExternalAddress);
            return reflectedExternalAddress;
        } catch (RuntimeException e) {
            logger.error("External IP auto-detection failed", e);
        }
        return null;
    }

}
