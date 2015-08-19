/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Class which is transfered to the server during a service call request.
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class ServiceCallRequest implements Serializable {

    private static final long serialVersionUID = -9120629516281659775L;

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"{0}\" must not be null.";

    /**
     * The {@link NodeIdentifier} where the bundle's class/method/package to call is located.
     */
    private final NodeIdentifier piRequested;

    /**
     * The {@link NodeIdentifier} of the calling platform.
     */
    private final NodeIdentifier piCalling;

    /**
     * Full name of the service to call; including package name.
     */
    private final String myService;

    /**
     * The properties of the service.
     */
    private final String myServicePoperties;

    /**
     * Name of the service method to call.
     */
    private final String myServiceMethod;

    /**
     * List of parameters.
     */
    private final List<? extends Serializable> myParameterList;

    /**
     * The number of hops this request already walked to avoid endless loops.
     */
    private Integer myHopCount = 0;

    /**
     * The Constructor to initialize the parameters.
     * 
     * @param piRequested The name of the {@link NodeIdentifier}.
     * @param piCalling The name of the {@link NodeIdentifier}.
     * @param service The name of the remote service (FQN of the interface).
     * @param serviceProperties The desired properties of the service.
     * @param serviceMethod The name of the remote method to call.
     * @param parameterList List of parameters.
     */
    public ServiceCallRequest(NodeIdentifier piRequested, NodeIdentifier piCalling,
        String service, String serviceProperties, String serviceMethod, List<? extends Serializable> parameterList) {

        Assertions.isDefined(piRequested, MessageFormat.format(ERROR_PARAMETERS_NULL, "piRequest"));
        Assertions.isDefined(piCalling, MessageFormat.format(ERROR_PARAMETERS_NULL, "piResponse"));
        Assertions.isDefined(service, MessageFormat.format(ERROR_PARAMETERS_NULL, "service"));
        Assertions.isDefined(serviceMethod, MessageFormat.format(ERROR_PARAMETERS_NULL, "serviceMethod"));

        this.piRequested = piRequested;
        this.piCalling = piCalling;
        myService = service;
        // an empty properties filter means no filter is desired
        // to meet the OSGi API the filter has to be null in this case
        if (serviceProperties != null && serviceProperties.isEmpty()) {
            myServicePoperties = null;
        } else {
            myServicePoperties = serviceProperties;
        }
        myServiceMethod = serviceMethod;
        myParameterList = parameterList;
    }

    public NodeIdentifier getRequestedPlatform() {
        return piRequested;
    }

    public NodeIdentifier getCallingPlatform() {
        return piCalling;
    }

    public String getService() {
        return myService;
    }

    public String getServiceProperties() {
        return myServicePoperties;
    }

    public String getServiceMethod() {
        return myServiceMethod;
    }

    public List<? extends Serializable> getParameterList() {
        return myParameterList;
    }

    /**
     * Increases the hop count and returns the new value.
     * 
     * @return The new value of the hop counter.
     */
    public Integer increaseHopCount() {
        return ++myHopCount;
    }

}
