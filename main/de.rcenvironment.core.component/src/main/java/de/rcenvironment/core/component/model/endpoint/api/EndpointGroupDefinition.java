/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

/**
 * Describes an input group which can contain inputs and other input groups.
 * 
 * @author Doreen Seider
 */
public interface EndpointGroupDefinition {

    /**
     * Logic operation.
     * 
     * @author Doreen Seider
     */
    enum LogicOperation {
        
        Or,
        
        And;
    }
    
    /**
     * @return endpoint group name or <code>null</code> if it is a dynamic endpoint group
     */
    String getName();

    /**
     * @return endpoint group identifier or <code>null</code> if it is a static endpoint group
     */
    String getIdentifier();
    
    /**
     * @return logic operation
     */
    LogicOperation getLogicOperation();

    /**
     * @return name of the parent group or <code>null</code> if it has none
     */
    String getParentGroupName();

}
