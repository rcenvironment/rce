/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.communication.rpc.ServiceCallRequest;

/**
 * The result of a local service resolution. If the resolution is successful, this object is intended to be cached to avoid the overhead of
 * repeated lookups. Reflection-based checks are also intended to be cached within this object.
 * 
 * @author Robert Mischke
 */
public class LocalServiceLookupResult {

    private final Object validImplementation;

    private final Set<String> validMethodNames;

    public LocalServiceLookupResult(Object validImplementation, Collection<String> methodNamesToAllow) {
        this.validImplementation = validImplementation;
        this.validMethodNames = new HashSet<>(methodNamesToAllow);
    }

    /**
     * @return an instance that always returns 'false' on {@link #isValidRemotableService()}
     */
    public static LocalServiceLookupResult createInvalidServicePlaceholder() {
        return new LocalServiceLookupResult(null, new ArrayList<String>());
    }

    public boolean isValidRemotableService() {
        return validImplementation != null;
    }

    /**
     * Checks whether the given method name is valid to be called as part of a {@link ServiceCallRequest}.
     * 
     * @param methodName the requested method name
     * @return true if the method is allowed to be called
     */
    public boolean isValidMethodRequest(String methodName) {
        return validMethodNames.contains(methodName);
    }

    public Object getImplementation() {
        return validImplementation;
    }
}
