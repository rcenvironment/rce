/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.util.Optional;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;

/**
 * An interface for handling events that occur as part of a {@link ClientSideUplinkSession}.
 *
 * @author Robert Mischke
 */
public interface ClientSideUplinkSessionEventHandler {

    /**
     * Signals that the session is initialized and ready to use, and provides the server-assigned namespace id (as well as the derived
     * destination id prefix) to listeners.
     * 
     * @param namespaceId the server-assigned namespace id
     * @param destinationIdPrefix the the server-assigned destination id prefix, which is currently equal to the namespace id; added as a
     *        separate parameter as it may be different in the future
     */
    void onSessionActivating(String namespaceId, String destinationIdPrefix);

    /**
     * Signals that the session is either about to shut down or has prematurely ended, and that no further messages should be sent.
     */
    void onActiveSessionTerminating();

    /**
     * Reports an error message either sent by the remote side, or caused by a connection error. This event is <em>informational</em>,
     * typically for logging or presenting the message to the user, and should not be used for flow or state control. To reliably detect the
     * end of an Uplink session, use {@link #onSessionInFinalState()} instead, which is also triggered if no error message is available.
     * <p>
     * Implementation note: Callers should invoke this BEFORE using {@link #setSessionActiveState()} to deactivate the session.
     * 
     * @param errorType the type of the error; note that this enum object also signals whether auto-retry is reasonable for this kind of
     *        error (see {@link UplinkProtocolErrorType#getClientRetryFlag()})
     * @param errorMessage the message received from the remote side or generated locally after an error
     */
    void onFatalErrorMessage(UplinkProtocolErrorType errorType, String errorMessage);

    /**
     * Reports that this session should be considered closed. Before this, the "active" state of the session will have been set to false.
     * 
     * @param reasonableToRetry whether it is reasonable to try reconnecting with the same connection parameters (under the condition that
     *        retry is enabled/desired for this connection in the first place, which is the decision of the code receiving this callback)
     */
    void onSessionInFinalState(boolean reasonableToRetry);

    /**
     * Receive and process an updated list of the published tools of a remote source (identified by an opaque "source id"). An empty list
     * may either mean that that source unpublished all of its tools, or that it has disappeared from the network; this is meant to be
     * indistinguishable for event receivers.
     * 
     * @param update the update object
     */
    void processToolDescriptorListUpdate(ToolDescriptorListUpdate update);

    /**
     * Requests a new {@link ToolExecutionProvider} to be created and set up for handling the given {@link ToolExecutionRequest}.
     * <p>
     * TODO specify failure behavior; what should already be checked by this method?
     * 
     * @param request the received request
     * @return a new {@link ToolExecutionProvider} to handle this request
     */
    ToolExecutionProvider setUpToolExecutionProvider(ToolExecutionRequest request);

    /**
     * Provides a binary block of documentation data that was previously referenced as part of a {@link ToolDescriptor}'s data. Typically,
     * this binary block represents an archive file containing the actual documentation files.
     * 
     * @param destinationId the destination id in the local network to provide the documentation data from
     * @param docReferenceId the reference id as contained in the {@link ToolDescriptor}
     * @return the data source to provide the documentation data stream from, or an empty {@link Optional} if no documentation data is
     *         available for the given parameters
     */
    Optional<SizeValidatedDataSource> provideToolDocumentationData(String destinationId, String docReferenceId);

}
