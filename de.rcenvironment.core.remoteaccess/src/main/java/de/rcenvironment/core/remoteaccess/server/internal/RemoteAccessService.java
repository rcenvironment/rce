/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.server.internal;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
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
 * @author Brigitte Boden
 */
public interface RemoteAccessService {

    /**
     * Prints information about all tools (components) that match the calling conventions of the "ra run-tool" command.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param format the output format to use: supported values are "csv" and "token-stream"
     * @param includeLoadData true to fetch and include system load data (CPU/RAM) in the generated output
     * @param timeSpanMsec the maximum time span, in milliseconds, to aggregate/average load information over
     * @param timeLimitMsec the maximum time, in milliseconds, to wait for each node's load data
     * @throws TimeoutException on unexpected errors during asynchronous task execution
     * @throws ExecutionException on unexpected errors during asynchronous task execution
     * @throws InterruptedException on interruption while waiting for asynchronous task execution
     */
    void printListOfAvailableTools(TextOutputReceiver outputReceiver, String format, boolean includeLoadData, int timeSpanMsec,
        int timeLimitMsec) throws InterruptedException, ExecutionException, TimeoutException;
    
    /**
     * Prints details (inputs/outputs and their data types) for a single tool.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param toolId the tool id
     * @param toolVersion the tool version
     * @param nodeId the nodeId
     * @param template if true, a template for the inputs.json file will be printed
     */
    void printToolDetails(TextOutputReceiver outputReceiver, String toolId, String toolVersion, String nodeId, boolean template);
    
    /**
     * Prints details (inputs/outputs and their data types) for a single workflow.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param wfId the workflow id
     * @param template if true, a template for the inputs.json file will be printed
     */
    void printWfDetails(TextOutputReceiver outputReceiver, String wfId, boolean template);

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
     * @param parameters Object containing all the parameters for remote tool execution
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * 
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    FinalWorkflowState runSingleToolWorkflow(RemoteComponentExecutionParameter parameters, SingleConsoleRowsProcessor consoleRowReceiver)
        throws IOException,
        WorkflowExecutionException;

    /**
     * Executes a previously published workflow template.
     * 
     * @param workflowId the id of the published workflow template
     * @param sessionToken TODO
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @param uncompressedUpload if the upload should be uncompressed
     * @param simpleDescription if the simple inputs description format should be used
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    FinalWorkflowState runPublishedWorkflowTemplate(String workflowId, String sessionToken, File inputFilesDir, File outputFilesDir,
        SingleConsoleRowsProcessor consoleRowReceiver, boolean uncompressedUpload, boolean simpleDescription)
        throws IOException, WorkflowExecutionException;

    /**
     * Checks if the given workflow file can be used with the "wf-run" console command, and if this check is positive, the workflow file is
     * published under the given id.
     * 
     * @param wfFile the workflow file
     * @param placeholdersFile TODO
     * @param publishId the id by which the workflow file should be made available
     * @param groupName name of the palette group in which the workflow will be shown
     * @param outputReceiver receiver for user feedback
     * @param persistent whether the published workflow (and optionally, its properties file) should be restored after instance restarts
     * @param neverDeleteExecutionData whether the execution data should be kept even for successful workflow runs
     * @throws WorkflowExecutionException on failure to load/parse the workflow file
     */
    void checkAndPublishWorkflowFile(File wfFile, File placeholdersFile, String publishId, String groupName,
        TextOutputReceiver outputReceiver, boolean persistent, boolean neverDeleteExecutionData) throws WorkflowExecutionException;

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

    /**
     * Cancel a running workflow or tool.
     * 
     * @param sessionToken the session token to identify the tool/workflow.
     */
    void cancelToolOrWorkflow(String sessionToken);
    
    
    /**
     * Get list of nodeIds with documentation for given tool.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param toolId the tool id
     */
    void getToolDocumentationList(TextOutputReceiver outputReceiver, String toolId);
    
    /**
     * Downloads the documentation for a tool.
     * 
     * @param outputReceiver the receiver to print the output to
     * @param toolId the tool id
     * @param nodeId the nodeId
     * @param hashValue the documentation hash value
     * @param outputFilePath the file path for the download
     */
    void getToolDocumentation(TextOutputReceiver outputReceiver, String toolId, String nodeId, String hashValue, File outputFilePath);
}
