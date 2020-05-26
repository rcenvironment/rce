/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.api.RemotableCallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheck;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheckHasAnnotation;

/**
 * Implementation of {@link CallbackService} (including {@link RemotableCallbackService}).
 * 
 * @author Doreen Seider
 */
public class CallbackServiceImpl implements CallbackService, RemotableCallbackService {

    // the callback that verifies the presence of @AllowRemoteAccess annotations
    private static final MethodPermissionCheck METHOD_PERMISSION_CHECK = new MethodPermissionCheckHasAnnotation(AllowRemoteAccess.class);

    private Map<String, WeakReference<Object>> objects = Collections.synchronizedMap(new HashMap<String, WeakReference<Object>>());

    private Map<String, InstanceNodeSessionId> remotePlatforms = Collections.synchronizedMap(new HashMap<String, InstanceNodeSessionId>());

    private Map<String, Long> ttls = Collections.synchronizedMap(new HashMap<String, Long>());

    private PlatformService platformService;

    protected void activate(BundleContext context) {
        CleanJob.scheduleJob(CallbackService.class, objects, ttls, remotePlatforms);
    }

    protected void deactivate(BundleContext context) {
        CleanJob.unscheduleJob(CallbackService.class);
    }

    /**
     * Bind method called by OSGi-DS.
     * 
     * @param newPlatformService Service to bind.
     */
    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    @Override
    public String addCallbackObject(Object callbackObject, InstanceNodeSessionId nodeId) {
        String identifier = null;
        synchronized (objects) {
            for (String id : objects.keySet()) {
                if (objects.get(id).get() == callbackObject) {
                    return id;
                }
            }
        }

        identifier = UUID.randomUUID().toString();
        objects.put(identifier, new WeakReference<Object>(callbackObject));
        remotePlatforms.put(identifier, nodeId);
        ttls.put(identifier, new Date(System.currentTimeMillis() + CleanJob.TTL_MSEC).getTime());

        return identifier;
    }

    @Override
    public Object getCallbackObject(String objectIdentifier) {
        WeakReference<Object> weakRef = objects.get(objectIdentifier);
        if (weakRef != null) {
            return weakRef.get();
        } else {
            return null;
        }
    }

    @Override
    @AllowRemoteAccess
    public Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters)
        throws RemoteOperationException {

        WeakReference<Object> weakRef = objects.get(objectIdentifier);
        if (weakRef != null) {
            Object objectToCall = weakRef.get();
            if (objectToCall != null) {
                // LOGGER.debug("Received method call: " + methodName + "@" +
                // objectToCall.toString());
                try {
                    return MethodCaller.callMethod(objectToCall, methodName, parameters, METHOD_PERMISSION_CHECK);
                } catch (InvocationTargetException e) {
                    // TODO (p2) review (was _FIXME 7.0.0)
                    throw new RemoteOperationException("Callback method threw an exception: " + e.toString());
                }
            }
        }
        throw new RemoteOperationException("The object to call back is not reachable anymore, requested method: " + methodName + "(...)");
    }

    @Override
    @AllowRemoteAccess
    public void setTTL(String objectIdentifier, Long ttl) {
        ttls.put(objectIdentifier, ttl);
    }

    @Override
    public Object createCallbackProxy(CallbackObject callbackObject, String objectIdentifier,
        InstanceNodeSessionId proxyHome) {

        InvocationHandler handler = new CallbackInvocationHandler(callbackObject, objectIdentifier,
            platformService.getLocalInstanceNodeSessionId(), proxyHome);
        Object proxy = Proxy.newProxyInstance(CallbackProxy.class.getClassLoader(),
            new Class[] { callbackObject.getInterface(), CallbackProxy.class }, handler);
        return proxy;
    }

    @Override
    public String getCallbackObjectIdentifier(Object callbackObject) {
        synchronized (objects) {
            for (String id : objects.keySet()) {
                if (objects.get(id).get() != null) {
                    if (objects.get(id).get() == callbackObject) {
                        return id;
                    }
                }
            }
        }
        return null;
    }

}
