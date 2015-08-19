/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.jetty.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import de.rcenvironment.core.jetty.JettyService;

/**
 * Implementation of {@link JettyService}.
 * 
 * The server starts with the first deployment.
 * 
 * @author Tobias Menden
 */
public class JettyServiceImpl implements JettyService {

    private static final Log LOGGER = LogFactory.getLog(JettyServiceImpl.class);
    
    private static final int TIMEOUT = 600000;
    
    private Map<String, Server> allDeployedServer = new HashMap<String, Server>();

    @Override
    public void deployWebService(Object webService, String address) {
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setAddress(address);
        svrFactory.setServiceBean(webService);
        Server server = svrFactory.create();
        allDeployedServer.put(address, server);
    }

    @Override
    public void undeployWebService(String address) {
        Server serverInstance = (Server) allDeployedServer.get(address);
        if (serverInstance != null) {
            serverInstance.stop();            
        } else {
            LOGGER.warn("No Web service deployed with the given address: " + address);
        }
    }

    @Override
    public Object createWebServiceClient(Class<?> webServiceInterface, String address) {
        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(webServiceInterface);
        clientFactory.setAddress(address);
        
        Object clientObject = clientFactory.create();
        
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(TIMEOUT);
        httpClientPolicy.setReceiveTimeout(TIMEOUT);

        Client client = ClientProxy.getClient(clientObject);
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        httpConduit.setClient(httpClientPolicy);
        
        return clientObject;
    }

}
