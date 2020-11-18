/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.io.IOException;
import java.util.Optional;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionClientSideSetup;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionEventHandler;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSession;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;

/**
 * Represents a single uplink session in which local tools can be published and remote tools can be executed. Typically corresponds to the
 * lifetime of a {@link UplinkConnection}, which in turn typically corresponds to an established network connection. Sessions are typically
 * long-lived, especially when publishing tools, and can be used for multiple tool executions.
 *
 * @author Robert Mischke
 */
public interface ClientSideUplinkSession extends UplinkSession {

    /**
     * Performs the initial protocol handshake and runs the incoming message loop. This should typically be run in a separate thread. Note
     * that this method should typically be called once the underlying {@link UplinkConnection} has been established, and that it blocks
     * while the logical protocol connection is still alive. If the underlying {@link UplinkConnection} breaks down, this method will also
     * eventually terminate.
     * 
     * @return true if the session ended cleanly; false if the initial handshake failed, the connection was refused, a fatal error occurred
     *         during the session, or the connection was closed unexpectedly
     */
    boolean runSession();

    /**
     * Updates (ie, replaces) the list of tools to publish via this uplink connection.
     * <p>
     * Note the current simple approach may be replaced by something more bandwidth-conserving in future implementations. This may either be
     * handled automatically by this API's implementation, or require API changes.
     * 
     * @param update the update to publish
     * @throws IOException on failure
     */
    void publishToolDescriptorListUpdate(ToolDescriptorListUpdate update) throws IOException;

    /**
     * Sends a {@link ToolExecutionRequest} based on the given {@link ToolExecutionClientSideSetup} to the uplink server, handles input file
     * upload when requested, and forwards events and output files to the provided {@link ToolExecutionEventHandler}.
     * 
     * @param setup the object providing execution parameters and any input files
     * @param eventHandler a callback interface to receive life-cycle events and output files
     * @return a {@link ToolExecutionHandle} for potentially canceling the tool execution, or nothing if the tool execution could not be
     *         initialized (e.g. because the providing side refused the request; in that case, an error callback should have been triggered)
     */
    Optional<ToolExecutionHandle> initiateToolExecution(ToolExecutionClientSideSetup setup, ToolExecutionEventHandler eventHandler);

    /**
     * Retrieves a binary block of documentation data that was previously referenced as part of a {@link ToolDescriptor}'s data. Typically,
     * this binary block represents an archive file containing the actual documentation files.
     * 
     * @param destinationId the opaque source id referencing the remote machine to fetch the documentation data from
     * @param docReferenceId the reference id as contained in the {@link ToolDescriptor}
     * @return the data source to read the retrieved binary documentation data from, or an empty {@link Optional} if no documentation data
     *         was available for the given parameters
     */
    Optional<SizeValidatedDataSource> fetchDocumentationData(String destinationId, String docReferenceId);

}
