/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.documentation;

import java.io.IOException;

import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * An interface for providing tool documentation for remote tools.
 *
 * @author Brigitte Boden
 */
public interface ToolDocumentationProvider {

    /**
     * Provide tool documentation for a tool. This method is meant to be called from the ToolDocumentationService.
     * 
     * @param identifier the tool id
     * @param nodeId the nodeId where the tool is located
     * @param hashValue documentation hash value
     * @return ByteStream of documentation document
     * @throws RemoteOperationException on failure
     * @throws IOException on errors retrieving the documentation
     */
    byte[] provideToolDocumentation(String identifier, String nodeId, String hashValue) throws RemoteOperationException, IOException;
}
