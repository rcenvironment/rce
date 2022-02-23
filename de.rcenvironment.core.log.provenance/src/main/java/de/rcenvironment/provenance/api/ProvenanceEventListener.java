/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.provenance.api;

import java.io.File;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * This interface represents the Provenance API. Plugins interested in recording provenance can implement this interface.
 * 
 * @author Alexander Weinert
 *
 */
public interface ProvenanceEventListener {

    void workflowRunStarted(String workflowExecutionIdentifier, String fileName, String execLoc, String comment);

    void workflowRunFinished(String workflowExecutionIdentifier);

    void workflowNodeExecutionStarted(String workflowExecutionIdentifier, String workflowNodeName, String workflowNodeIdentifier,
        String componentExecutionIdentifier, String workflowNodeExecution);

    void workflowNodeExecutionFinished(String componentExecutionIdentifier);

    void datumForwarded(String componentExecutionIdentifierStart, String outputName, String componentExecutionIdentifierEnd,
        String inputName, TypedDatum datum);

    void workflowFileLoaded(String executionIdentifier, File file);

    void inputRead(String componentExecutionIdentifier, String inputName, TypedDatum inputValue, String constraint, String handling);

    void outputWritten(String componentExecutionIdentifier, String outputName, TypedDatum outputValue);

    void toolRunStarted(String componentExecutionIdentifier, String toolName, String toolVersion, String toolRevision);

    void toolRunFinished(String componentExecutionIdentifier);
}
