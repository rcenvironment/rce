/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.documentation;

import java.util.Map;

import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * 
 * Remote api for the {@link ToolIntegrationService}.
 * 
 * @author Brigitte Boden
 */
@RemotableService
public interface RemoteToolIntegrationDocumentationService {

    /**
     * Gets the documentation for the given integrated component.
     * 
     * @param identifier of integrated component
     * @param nodeId nodeId to load from
     * @param hashValue the documentation hash value
     * @return documentation folder of component that was zipped
     * @throws RemoteOperationException standard {@link RemotableService} exception
     */
    byte[] loadToolDocumentation(String identifier, String nodeId, String hashValue) throws RemoteOperationException;
    
    /**
     * Gets list of nodes with tool documentation for remote access tools.
     * 
     * @param identifier identifier of integrated component
     * @return  @return map with all different hashes
     * @throws RemoteOperationException standard {@link RemotableService} exception
     */
    Map<String, String> getComponentDocumentationListForRemoteAccessTools(String identifier) throws RemoteOperationException;
    
}
