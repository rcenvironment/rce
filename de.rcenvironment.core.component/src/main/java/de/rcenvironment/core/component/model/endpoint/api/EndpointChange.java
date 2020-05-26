/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.io.Serializable;

/**
 * Event class to propagate changes of endpoints.
 * 
 * @author Christian Weiss
 */
public final class EndpointChange implements Serializable {

    private static final long serialVersionUID = 9088170162935461299L;

    /**
     * The type of {@link EndpointChange}.
     * 
     * @author Christian Weiss
     */
    public enum Type {
        /** An endpoint was added. */
        Added,
        /** An endpoint was removed. */
        Removed,
        /** An endpoint was changed. */
        Modified;
    }

    private final EndpointChange.Type type;

    private final EndpointDescription endpointDesc;
    
    private final EndpointDescription oldEndpointDesc;
    
    public EndpointChange(EndpointChange.Type type, EndpointDescription endpointDesc, EndpointDescription oldEndpointDesc) {
        
        this.type = type;
        this.endpointDesc = endpointDesc;
        this.oldEndpointDesc = oldEndpointDesc;
    }

    public Type getType() {
        return type;
    }

    public EndpointDescription getEndpointDescription() {
        return endpointDesc;
    }

    public EndpointDescription getOldEndpointDescription() {
        return oldEndpointDesc;
    }

}
