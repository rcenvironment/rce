/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

/**
 * Describes an input group which can contain inputs and other input groups.
 * @author Doreen Seider
 */
public interface EndpointGroupDefinition {

    /**
     * Scheduling type.
     * 
     * @author Doreen Seider
     */
    enum Type {
        
        Or,
        
        And;
    }
    
    /**
     * @return identifier of the group
     */
    String getIdentifier();

    /**
     * @return scheduling type
     */
    Type getType();

    /**
     * @return gat name of the group
     */
    String getGroupName();

}
