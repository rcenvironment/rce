/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.jetty;

/**
 * Service for the deployment of Web services in Jetty and building client instances to call a Web
 * service.
 * 
 * @author Tobias Menden
 */
public interface JettyService {

    /**
     * Deploys a Web service. If Jetty isn't already running, it will be started.
     * 
     * @param webService An instance of the Web service implementation.
     * @param address The deployment address.
     */
    void deployWebService(Object webService, String address);

    /**
     * Undeploys the Web service with the given address. Jetty keeps running.
     * 
     * @param address The address from the server which is going to be undeployed.
     */
    void undeployWebService(String address);

    /**
     * Creates a client instance to call a Web service.
     * 
     * @param webServiceInterface The interface of the Web service.
     * @param address The address of the Web service to call.
     * @return client A client instance for calling the Web service.
     */
    Object createWebServiceClient(Class<?> webServiceInterface, String address);
}
