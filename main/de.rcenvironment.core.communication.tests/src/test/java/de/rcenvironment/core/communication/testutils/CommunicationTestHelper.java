/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.communication.rpc.internal.MethodCallTestInterface;

/**
 * Test constants for the communication tests.
 * 
 * @author Doreen Seider
 */
public final class CommunicationTestHelper {

    /**
     * Bundle symbolic name.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.communication";

    /**
     * Test protocol.
     */
    public static final String RMI_PROTOCOL = "de.rcenvironment.rce.communication.rmi";

    /**
     * Test service.
     */
    public static final String SERVICE = MethodCallTestInterface.class.getCanonicalName();

    /**
     * Test name of the host.
     */
    public static final String LOCALHOST = "localhost";

    /**
     * Test IP of the host.
     */
    public static final String LOCALHOST_IP = "127.0.0.1";

    /**
     * Test IP of the host.
     */
    public static final String REMOTE_HOST_IP = "192.168.0.1";

    /**
     * Test name of the instance.
     */
    public static final int INSTANCE = 0;

    /**
     * NodeIdentifier.
     */
    public static final NodeIdentifier LOCAL_PLATFORM = NodeIdentifierFactory.fromHostAndNumber(LOCALHOST_IP, INSTANCE);

    /**
     * NodeIdentifier.
     */
    public static final NodeIdentifier REMOTE_PLATFORM = NodeIdentifierFactory.fromHostAndNumber(REMOTE_HOST_IP, INSTANCE);

    /**
     * Test RMI port.
     */
    public static final int RMI_PORT = 1099;

    /**
     * Test method.
     */
    public static final String METHOD = "getString";

    /**
     * Test return value.
     */
    public static final String RETURN_VALUE = MethodCallTestInterface.DEFAULT_RESULT_OR_MESSAGE_STRING;

    /**
     * Test parameter.
     */
    public static final List<? extends Serializable> PARAMETER_LIST = new ArrayList<Serializable>();

    /**
     * Test communication contact.
     */
    public static final NetworkContact SERVICE_CONTACT = new NetworkContact(LOCALHOST_IP, RMI_PROTOCOL, RMI_PORT);

    /**
     * Test communication contact.
     */
    public static final NetworkContact REMOTE_CONTACT = new NetworkContact(REMOTE_HOST_IP, RMI_PROTOCOL, RMI_PORT);

    /**
     * Test communication request.
     */
    public static final ServiceCallRequest REQUEST = new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, SERVICE,
        METHOD, PARAMETER_LIST);

    /**
     * Test communication request.
     */
    public static final ServiceCallRequest REMOTE_REQUEST = new ServiceCallRequest(REMOTE_PLATFORM, LOCAL_PLATFORM, SERVICE,
        METHOD, PARAMETER_LIST);

    /**
     * Test communication request.
     */
    public static final ServiceCallResult RESULT = ServiceCallResultFactory.wrapReturnValue(RETURN_VALUE);

    /**
     * Test communication request.
     */
    public static final String URI = "file://" + LOCALHOST_IP + ":" + INSTANCE + "/src/test/resources/ping.txt";

    private CommunicationTestHelper() {}

}
