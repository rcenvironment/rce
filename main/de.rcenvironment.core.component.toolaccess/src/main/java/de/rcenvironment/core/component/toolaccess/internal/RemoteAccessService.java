/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.toolaccess.internal;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.component.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A service providing the background operations for the "Remote Access" feature.
 * 
 * The main goal of the "Remote Access" feature is to allow the execution of single tools or complete workflows via console commands. This
 * is especially useful when these commands are sent via a network interface, like the embedded SSH server. Effectively, this allows other
 * applications (or scripts) to remote control parts of an RCE instance or network.
 * 
 * @author Robert Mischke
 */
public interface RemoteAccessService {

    /**
     * Prints information about all tools (components) that match the calling conventions of the "ra run-tool" command.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param format the output format to use: supported values are "csv" and "token-stream"
     */
    void printListOfAvailableTools(TextOutputReceiver outputReceiver, String format);

    /**
     * Prints information about all published workflows available for the "ra run-wf" command.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param format the output format to use: supported values are "csv" and "token-stream"
     */
    void printListOfAvailableWorkflows(TextOutputReceiver outputReceiver, String format);

    /**
     * Creates a workflow file from an internal template and the given parameters, and executes it.
     * 
     * @param toolId the id of the integrated tool to run (see CommonToolIntegratorComponent)
     * @param toolVersion the version of the integrated tool to run
     * @param nodeId the already-validated node id where the tool should be run; must not be null
     * @param parameterString an optional string containing tool-specific parameters
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    FinalWorkflowState runSingleToolWorkflow(String toolId, String toolVersion, String nodeId, String parameterString,
        File inputFilesDir, File outputFilesDir, SingleConsoleRowsProcessor consoleRowReceiver) throws IOException,
        WorkflowExecutionException;

    /**
     * Executes a previously published workflow template.
     * 
     * @param workflowId the id of the published workflow template
     * @param parameterString an optional string containing tool-specific parameters
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    FinalWorkflowState runPublishedWorkflowTemplate(String workflowId, String parameterString, File inputFilesDir,
        File outputFilesDir, SingleConsoleRowsProcessor consoleRowReceiver) throws IOException, WorkflowExecutionException;

    /**
     * Checks if the given workflow file can be used with the "wf-run" console command, and if this check is positive, the workflow file is
     * published under the given id.
     * 
     * @param wfFile the workflow file
     * @param placeholdersFile TODO
     * @param publishId the id by which the workflow file should be made available
     * @param outputReceiver receiver for user feedback
     * @param persistent whether the published workflow (and optionally, its properties file) should be restored after instance restarts
     * @throws WorkflowExecutionException on failure to load/parse the workflow file
     */
    void checkAndPublishWorkflowFile(File wfFile, File placeholdersFile, String publishId, TextOutputReceiver outputReceiver,
        boolean persistent) throws WorkflowExecutionException;

    /**
     * Makes the published workflow with the given id unavailable for remote invocation. If no such workflow exists, a text warning is
     * written to the output receiver.
     * 
     * @param publishId the id of the workflow to unpublish
     * @param outputReceiver the receiver for user feedback
     * @throws WorkflowExecutionException on validation failure
     */
    void unpublishWorkflowForId(String publishId, TextOutputReceiver outputReceiver) throws WorkflowExecutionException;

    /**
     * Prints human-readable information about all published workflows.
     * 
     * @param outputReceiver the receiver for the generated output
     */
    void printSummaryOfPublishedWorkflows(TextOutputReceiver outputReceiver);

    /**
     * @param toolVersion the given tool version
     * @param toolId the given tool id
     * @param nodeId the given node id, or null if unspecified
     * @return the node id to use
     * @throws WorkflowExecutionException if no tool matching the given parameters exists
     */
    String validateToolParametersAndGetFinalNodeId(String toolId, String toolVersion, String nodeId) throws WorkflowExecutionException;

}
