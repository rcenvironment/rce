/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.Collection;

/**
 * Component-specific {@link RemotableExecutionControllerService}.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionControllerService extends RemotableComponentExecutionControllerService {

    /**
     * @return {@link ComponentExecutionInformation} objects of all active components
     */
    Collection<ComponentExecutionInformation> getComponentExecutionInformations();
    
}
