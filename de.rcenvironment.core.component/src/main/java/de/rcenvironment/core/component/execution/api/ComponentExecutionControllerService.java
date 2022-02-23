/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
