/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Service forgetting the tool documentation from integrated tools.
 * 
 * @author Sascha Zur
 */
public interface ToolIntegrationDocumentationService {

    /**
     * Gets the documentation for the given integrated component.
     * 
     * @param identifier of integrated component
     * @param nodeId of the remote instance
     * @param hashValue of the documentation folder
     * @return documentation folder of component that was zipped
     * @throws RemoteOperationException if remote call fails
     * @throws IOException if opening file fails
     * @throws FileNotFoundException if file could not be opened
     */
    File getToolDocumentation(String identifier, String nodeId, String hashValue)
        throws RemoteOperationException, FileNotFoundException, IOException;

    /**
     * Gets a map where every different hash of component documentation has one remote instance.
     * 
     * @param identifier of the tool to get the documentation from.
     * @return map with all different hashes
     */
    Map<String, String> getComponentDocumentationList(String identifier);
}
