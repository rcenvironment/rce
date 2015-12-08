/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote api for the {@link ToolIntegrationService}.
 * 
 * @author Sascha Zur
 */
@RemotableService
public interface RemoteToolIntegrationService {

    /**
     * Gets the documentation for the given integrated component.
     * 
     * @param identifier of integrated component
     * @return documentation folder of component that was zipped
     * @throws RemoteOperationException standard {@link RemotableService} exception
     */
    byte[] getToolDocumentation(String identifier) throws RemoteOperationException;

}
