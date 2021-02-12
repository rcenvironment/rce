/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;

/**
 * Utility class for service call handlings.
 * 
 * @author Doreen Seider
 */
public final class CallbackUtils {

    private static final Log LOGGER = LogFactory.getLog(CallbackUtils.class);

    private CallbackUtils() {}

    /**
     * Handles an object that could be an object which needs to be proxied before it is sent to a
     * remote platform.
     * 
     * @param o The object to check and to proxy.
     * @param cs The {@link CallbackService} to use.
     * @param cps The {@link CallbackProxyService} to use.
     * @return the proxied object or <code>o</code> if there is no need for proxying.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object handleCallbackProxy(Object o, CallbackService cs, CallbackProxyService cps) {

        Object newObject = o;
        if (o instanceof CallbackProxy) {
            String objectIdentifier = ((CallbackProxy) o).getObjectIdentifier();
            Object object = cs.getCallbackObject(objectIdentifier);
            StatsCounter.countClass("Callback proxy parameter", object); // not expected in live operation
            if (object != null) {
                newObject = object;
            } else {
                Object proxy = cps.getCallbackProxy(objectIdentifier);
                if (proxy != null) {
                    newObject = proxy;
                } else {
                    cps.addCallbackProxy((CallbackProxy) o);
                }
            }
        } else if (o instanceof Collection) {
            List<Object> newParam = new ArrayList<Object>();
            Iterator iterator = ((Collection) o).iterator();
            while (iterator.hasNext()) {
                newParam.add(newParam.size(), handleCallbackProxy(iterator.next(), cs, cps));
            }
            try {
                ((Collection) o).clear();
                ((Collection) o).addAll(newParam);
            } catch (UnsupportedOperationException e) {
                LOGGER.debug("Handling callback objects failed for this collection because it is immutable: " + o.toString());
            }

        }

        return newObject;
    }

    /**
     * Handles an object that could be a proxy and which needs to be replaced by an exiting one.
     * 
     * @param o The object to check and to replace.
     * @param pi The {@link InstanceNodeSessionId} of the remote platform.
     * @param cs The {@link CallbackService} to use.
     * @return the proxied object or <code>o</code> if there is no need for proxying.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object handleCallbackObject(Object o, InstanceNodeSessionId pi, CallbackService cs) {

        Object newObject = o;
        if (o instanceof CallbackObject) {
            String identifier = cs.getCallbackObjectIdentifier(o);
            if (identifier == null) {
                identifier = cs.addCallbackObject(o, pi);
            }
            newObject = cs.createCallbackProxy((CallbackObject) o, identifier, pi);
        } else if (o instanceof Collection) {
            List<Object> newParam = new ArrayList<Object>();
            Iterator iterator = ((Collection) o).iterator();
            while (iterator.hasNext()) {
                newParam.add(newParam.size(), handleCallbackObject(iterator.next(), pi, cs));
            }
            try {
                ((Collection) o).clear();
                ((Collection) o).addAll(newParam);
            } catch (UnsupportedOperationException e) {
                LOGGER.debug("handling callback objects failed for this collection because it is immutable: " + o.toString());
            }

        }

        return newObject;
    }
}
