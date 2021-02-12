/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Maps between user-friendly external tool references and internal component/tool ids. See method JavaDocs for examples.
 *
 * @author Robert Mischke
 */
public interface UserComponentIdMappingService {

    /**
     * Maps from an external tool representation to the internal id.
     * <p>
     * Examples:
     * 
     * <pre>
     * rce/CPACS Writer -> de.rcenvironment.cpacswriter
     * common/MyTool -> de.rcenvironment.integration.common.MyTool
     * </pre>
     * 
     * @param input the external representation
     * @return the internal id
     * @throws OperationFailureException on invalid id input
     */
    String fromExternalToInternalId(String input) throws OperationFailureException;

    /**
     * Maps from an internal tool id to the external representation.
     * <p>
     * Examples:
     * 
     * <pre>
     * de.rcenvironment.cpacswriter -> rce/CPACS Writer
     * de.rcenvironment.integration.common.MyTool -> common/MyTool
     * </pre>
     * 
     * @param input the internal id
     * @return the external representation
     * @throws OperationFailureException on invalid id input
     */
    String fromInternalToExternalId(String input) throws OperationFailureException;

    /**
     * Registers a build-in component for name/id mapping.
     * 
     * @param intId the full internal id, e.g. "de.rcenvironment.cpacswriter"
     * @param name the display name, e.g. "CPACS Writer"
     */
    void registerBuiltinComponentMapping(String intId, String name);

}
