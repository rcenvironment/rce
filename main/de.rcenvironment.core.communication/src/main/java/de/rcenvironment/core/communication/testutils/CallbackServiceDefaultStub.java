/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Default service stub. All methods with a return value respond with the default field value for this type (null, 0, false, '\u0000', ...).
 * 
 * This class (and subclasses of it) is intended for test scenarios where an instance of {@link RemotableFileDataService} is required, but
 * where the exact calls to this instance are not relevant. If they are relevant and should be tested, create a mock instance instead (for
 * example, with the EasyMock library).
 * 
 * @author Robert Mischke
 */
public class CallbackServiceDefaultStub implements CallbackService {

    @Override
    public String addCallbackObject(Object callBackObject, NodeIdentifier nodeId) {
        return null;
    }

    @Override
    public Object getCallbackObject(String objectIdentifier) {
        return null;
    }

    @Override
    public String getCallbackObjectIdentifier(Object callbackObject) {
        return null;
    }

    @Override
    public void setTTL(String objectIdentifier, Long ttl) {}

    @Override
    public Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters)
        throws RemoteOperationException {
        return null;
    }

    @Override
    public Object createCallbackProxy(CallbackObject callbackObject, String objectIdentifier, NodeIdentifier proxyHome) {
        return null;
    }
}
