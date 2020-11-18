/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionClientSideSetup;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionEventHandler;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolExecutionHandle;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.communication.uplink.entities.ChannelCreationRequest;
import de.rcenvironment.core.communication.uplink.entities.ChannelCreationResponse;
import de.rcenvironment.core.communication.uplink.entities.ToolDocumentationRequest;
import de.rcenvironment.core.communication.uplink.network.channel.api.ChannelEndpoint;
import de.rcenvironment.core.communication.uplink.network.channel.internal.AbstractChannelEndpoint;
import de.rcenvironment.core.communication.uplink.network.channel.internal.DocumentationChannelInitiatorEndpoint;
import de.rcenvironment.core.communication.uplink.network.channel.internal.DocumentationChannelProviderEndpoint;
import de.rcenvironment.core.communication.uplink.network.channel.internal.ToolExecutionChannelInitiatorEndpoint;
import de.rcenvironment.core.communication.uplink.network.channel.internal.ToolExecutionChannelProviderEndpoint;
import de.rcenvironment.core.communication.uplink.network.internal.ClientSideUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.CommonUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkConnectionLowLevelEventHandler;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkConnectionRefusedException;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSessionState;
import de.rcenvironment.core.communication.uplink.session.internal.AbstractUplinkSessionImpl;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.BlockingResponseMapper;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * The default (client-side) {@link ClientSideUplinkSession} implementation. Besides maintaining session state, this class is also
 * responsible for converting local method calls into outgoing network messages, and incoming messages into callback events.
 * <p>
 * Note that for now, most of this class is a placeholder and/or test implementation that "mirrors" all published tools and all execution
 * requests back to the session's listeners.
 * 
 * @author Robert Mischke
 */
public class ClientSideUplinkSessionImpl extends AbstractUplinkSessionImpl implements ClientSideUplinkSession {

    private static final int CHANNEL_REQUEST_RESULT_TIMEOUT = 10000; // adapt if necessary

    private static final int DOCUMENTATION_REQUEST_RESULT_TIMEOUT = 10000; // adapt if necessary

    private static final AtomicInteger sharedSessionIdGenerator = new AtomicInteger(0);

    private final String localSessionId;

    private final ClientSideUplinkSessionParameters sessionParameters;

    private final ClientSideUplinkSessionEventHandler sessionEventHandler;

    private final ClientSideUplinkLowLevelProtocolWrapper lowLevelProtocolWrapper;

    private final UplinkProtocolMessageConverter messageConverter;

    private final BlockingResponseMapper<String, Object> responseMapper;

    private final Map<Long, ChannelEndpoint> channelEndpointMap = Collections.synchronizedMap(new HashMap<>());

    private final DefaultChannelClientEndpoint defaultChannelEndpoint;

    private final AtomicInteger requestIdCounter = new AtomicInteger(0);

    /**
     * A handler for connection-level events, especially incoming message blocks.
     *
     * @author Robert Mischke
     */
    private final class ClientSideUplinkLowLevelEventHandlerImpl implements UplinkConnectionLowLevelEventHandler {

        @Override
        public void provideOrProcessHandshakeData(Map<String, String> incomingData, Map<String, String> outgoingData) {

            if (incomingData == null && outgoingData != null) {
                // requested to produce the initial client-to-relay handshake data

                // the high-level protocol version for compatibility checking
                outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_PROTOCOL_VERSION_OFFER,
                    UplinkProtocolConstants.DEFAULT_PROTOCOL_VERSION);

                String clientVersionString = sessionParameters.getClientVersionInfo();
                if (clientVersionString != null) {
                    outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_CLIENT_VERSION_INFO, clientVersionString);
                }

                // the "session qualifier"/"client id" allowing multiple logins using the same account while keeping them distinguishable
                final String sessionQualifier =
                    StringUtils.nullSafe(sessionParameters.getSessionQualifier(), UplinkProtocolConstants.SESSION_QUALIFIER_DEFAULT);
                outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_SESSION_QUALIFIER, sessionQualifier);

                // development/test fields
                if (sessionParameters.getCustomHandshakeData() != null) {
                    outgoingData.putAll(sessionParameters.getCustomHandshakeData()); // typically used for testing
                }

                markClientHandshakeSentOrReceived(); // not actually sent yet, but will be immediately
            } else if (incomingData != null && outgoingData == null) {
                // requested to process the relay's response
                markServerHandshakeSentOrReceived();

                // extract destination id prefix from response
                String serverAssignedNamespaceId = incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_ASSIGNED_NAMESPACE_ID);
                if (StringUtils.isNullorEmpty(serverAssignedNamespaceId)) {
                    serverAssignedNamespaceId = "<error: handshake data did not include a namespace id>/";
                } else {
                    setAssignedNamespaceId(serverAssignedNamespaceId);
                    updateLogDescriptor();
                }
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public void onHandshakeComplete() {
            markHandshakeSuccessful();
        }

        @Override
        public void onHandshakeFailedOrConnectionRefused(UplinkConnectionRefusedException e) {
            sessionEventHandler.onFatalSessionError(e.getType(), e.getRawMessage());
            markHandshakeFailed(e);
        }

        @Override
        public void onMessageBlock(long channelId, MessageBlock messageBlock) {
            incomingMessageQueue.enqueue(() -> {
                try {
                    if (channelId == UplinkProtocolConstants.DEFAULT_CHANNEL_ID) {
                        defaultChannelEndpoint.processMessage(messageBlock);
                    } else {
                        ChannelEndpoint channelEndpoint = channelEndpointMap.get(channelId);
                        if (channelEndpoint == null) {
                            log.error(StringUtils.format(
                                "%s Received a message of type %s for channel %d but found no registered endpoint to handle it",
                                logPrefix, messageBlock.getType(), channelId));
                            return;
                        }
                        channelEndpoint.processMessage(messageBlock);
                    }
                } catch (IOException e) {
                    // TODO add actual session closing
                    log.error(logPrefix + "Error while processing incoming message of type " + messageBlock.getType() + ", closing session",
                        e);
                }
            });
        }

        @Override
        public void onRegularGoodbyeMessage() {
            handleRegularRemoteGoodbyeMessage();
        }

        @Override
        public void onErrorGoodbyeMessage(UplinkProtocolErrorType errorType, String errorMessage) {
            handleFatalError(errorType, errorMessage);
        }

        @Override
        public void onIncomingStreamClosedOrEOF() {
            handleIncomingStreamClosedOrEOF();
        }

        @Override
        public void onStreamReadError(IOException e) {
            log.error("Error reading from stream " + getLogDescriptor() + ": " + e.toString());
            handleFatalError(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR, e.toString());
        }

        @Override
        public void onStreamWriteError(IOException e) {
            handleStreamWriteError(e);
        }

        @Override
        public void onNonProtocolError(Exception exception) {
            handleFatalError(UplinkProtocolErrorType.INTERNAL_CLIENT_ERROR, exception.toString());
        }
    }

    /**
     * The client-side end of a default channel. Default channels are used for general communication, for example the publication of tool
     * descriptors and the management of non-default channels.
     *
     * @author Robert Mischke
     */
    public class DefaultChannelClientEndpoint extends AbstractChannelEndpoint {

        public DefaultChannelClientEndpoint(ClientSideUplinkSession session) {
            super(session, session.getLocalSessionId(), UplinkProtocolConstants.DEFAULT_CHANNEL_ID);
        }

        @Override
        protected boolean processMessageInternal(MessageBlock messageBlock) throws IOException {

            MessageType messageType = messageBlock.getType();
            switch (messageType) {
            case TOOL_DESCRIPTOR_LIST_UPDATE:
                ToolDescriptorListUpdate update = messageConverter.decodeToolDescriptorListUpdate(messageBlock);
                sessionEventHandler.processToolDescriptorListUpdate(update);
                return true;
            case CHANNEL_INIT:
                ChannelCreationRequest request = messageConverter.decodeChannelCreationRequest(messageBlock);
                // always accept channels for now; simply send the response
                final long relayProvidedChannelId = request.getChannelId();
                final String channelType = request.getType();
                switch (channelType) {
                case "docs":
                    // register an endpoint for the expected incoming documentation request on this channel
                    channelEndpointMap.put(relayProvidedChannelId,
                        new DocumentationChannelProviderEndpoint(ClientSideUplinkSessionImpl.this, relayProvidedChannelId,
                            sessionEventHandler, request.getDestinationId()));
                    break;
                case "exec":
                    // register an endpoint for the expected incoming tool execution request on this channel
                    channelEndpointMap.put(relayProvidedChannelId,
                        new ToolExecutionChannelProviderEndpoint(ClientSideUplinkSessionImpl.this, relayProvidedChannelId,
                            sessionEventHandler, request.getDestinationId()));
                    break;
                default:
                    log.error("Ignoring channel request for invalid type " + channelType);
                    // TODO send refusal response
                    return true;
                }
                log.debug("Accepting offered message channel " + relayProvidedChannelId + " of type '" + channelType + "'");
                // note: the request id must be mirrored back to allow association at the initiating client
                // TODO security: rule out any possibility of associating with other channel requests here; unlikely as it is, though
                final ChannelCreationResponse responseToSend =
                    new ChannelCreationResponse(relayProvidedChannelId, request.getRequestId(), true);
                enqueueMessageBlockForSending(messageConverter.encodeChannelCreationResponse(responseToSend));
                return true;
            case CHANNEL_INIT_RESPONSE:
                ChannelCreationResponse receivedResponse = messageConverter.decodeChannelCreationResponse(messageBlock);
                responseMapper.registerResponse(receivedResponse.getRequestId(), receivedResponse);
                return true;
            default:
                log.warn("Ignoring message of unhandled type " + messageType);
                return true;
            }
        }

        @Override
        public void dispose() {}

    }

    /**
     * Creates the client side of a logical uplink session. Note that the session is actually initiated and run by calling
     * {@link #runSession()}, typically from a separate thread.
     * 
     * @param inputStream the outgoing data stream of the underlying connection
     * @param outputStream the incoming data stream of the underlying connection
     * @param eventHandler the {@link ClientSideUplinkSessionEventHandler}
     * @param concurrencyUtilsFactory the {@link ConcurrencyUtilsFactory}
     */
    public ClientSideUplinkSessionImpl(StreamConnectionEndpoint connectionEndpoint, ClientSideUplinkSessionParameters sessionParameters,
        ClientSideUplinkSessionEventHandler eventHandler, ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        super(concurrencyUtilsFactory);
        this.localSessionId = "c" + Integer.toString(sharedSessionIdGenerator.incrementAndGet());
        this.messageConverter = new UplinkProtocolMessageConverter(localSessionId);
        updateLogDescriptor(); // will be updated again once the namespace id is available
        ClientSideUplinkLowLevelEventHandlerImpl lowLevelEventHandler = new ClientSideUplinkLowLevelEventHandlerImpl();
        this.sessionParameters = sessionParameters;
        this.sessionEventHandler = eventHandler;
        this.responseMapper = concurrencyUtilsFactory.createBlockingResponseMapper();
        this.lowLevelProtocolWrapper =
            new ClientSideUplinkLowLevelProtocolWrapper(connectionEndpoint, lowLevelEventHandler, getLocalSessionId());
        this.defaultChannelEndpoint = new DefaultChannelClientEndpoint(this);
    }

    @Override
    public boolean runSession() {
        lowLevelProtocolWrapper.runSession();
        return getState() == UplinkSessionState.CLEAN_SHUTDOWN;
    }

    @Override
    public void publishToolDescriptorListUpdate(ToolDescriptorListUpdate update) throws IOException {
        final MessageBlock messageBlock = messageConverter.encodeToolDescriptorListUpdate(update);
        enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, messageBlock);
    }

    @Override
    public Optional<ToolExecutionHandle> initiateToolExecution(ToolExecutionClientSideSetup executionSetup,
        ToolExecutionEventHandler executionEventHandler) {

        Optional<ChannelCreationResponse> result;
        try {
            result = performChannelCreationRequest(executionSetup.getDestinationId(), "exec", "tool execution");
        } catch (IOException | InterruptedException | ExecutionException e) {
            // TODO clarify exceptions vs. returning empty Optional
            log.error("Error opening a tool execution channel: " + e.toString());
            return Optional.empty();
        }
        if (!result.isPresent()) {
            log.debug("Creation of a tool execution channel to " + executionSetup.getDestinationId()
                + " failed; see above log message for details");
            return Optional.empty();
        }
        final long relayAssignedChannelId = result.get().getChannelId();

        // register an endpoint for performing the tool execution message exchange
        final ToolExecutionChannelInitiatorEndpoint initiatorEndpoint =
            new ToolExecutionChannelInitiatorEndpoint(ClientSideUplinkSessionImpl.this, relayAssignedChannelId, sessionEventHandler);
        channelEndpointMap.put(relayAssignedChannelId, initiatorEndpoint);

        try {
            initiatorEndpoint.initiateToolExecution(new ToolExecutionRequest(executionSetup.getExecutionRequest()), executionEventHandler);
        } catch (IOException e) {
            // TODO clarify error behavior
            // TODO also log full exception?
            executionEventHandler.onError("Exception while initiating the tool execution: " + e.toString());
            return Optional.empty();
        }

        return Optional.of(initiatorEndpoint.getExecutionHandle());

    }

    @Override
    public Optional<SizeValidatedDataSource> fetchDocumentationData(String destinationId, String docReferenceId) {
        try {
            final Optional<ChannelCreationResponse> result = performChannelCreationRequest(destinationId, "docs", "documentation fetching");
            if (!result.isPresent()) {
                return Optional.empty(); // switch to different Optional type
            }
            final long relayAssignedChannelId = result.get().getChannelId();

            // register an endpoint for the expected documentation request response
            channelEndpointMap.put(relayAssignedChannelId,
                new DocumentationChannelInitiatorEndpoint(ClientSideUplinkSessionImpl.this, relayAssignedChannelId,
                    optionalDocumentationStream -> responseMapper.registerResponse("channel_" + relayAssignedChannelId,
                        optionalDocumentationStream)));

            // now send the actual documentation request; as it uses the unique channel, no request id is necessary for mapping
            enqueueMessageBlockForSending(relayAssignedChannelId,
                messageConverter.encodeDocumentationRequest(new ToolDocumentationRequest(docReferenceId)));

            final Optional<Object> optionalDocResponse =
                responseMapper.registerRequest("channel_" + relayAssignedChannelId, DOCUMENTATION_REQUEST_RESULT_TIMEOUT).get();

            if (!optionalDocResponse.isPresent()) {
                return Optional.empty();
            }
            return (Optional<SizeValidatedDataSource>) optionalDocResponse.get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            // TODO forward this exception instead?
            log.error("Error retrieving documentation data for id " + docReferenceId + " from " + destinationId, e);
            return Optional.empty();
        }
    }

    @Override
    public CommonUplinkLowLevelProtocolWrapper getProtocolWrapper() {
        return lowLevelProtocolWrapper;
    }

    @Override
    public String getLocalSessionId() {
        return localSessionId;
    }

    private String generateRequestId() {
        return Integer.toString(requestIdCounter.incrementAndGet());
    }

    private Optional<ChannelCreationResponse> performChannelCreationRequest(String destinationId, String channelType, String intention)
        throws IOException, InterruptedException, ExecutionException {

        // send a channel creation request
        final String requestId = generateRequestId();
        final ChannelCreationRequest request =
            new ChannelCreationRequest(channelType, destinationId, UplinkProtocolConstants.UNDEFINED_CHANNEL_ID, requestId);
        enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID,
            messageConverter.encodeChannelCreationRequest(request));
        // wait for the response or timeout
        final Optional<Object> optionalResponse = responseMapper.registerRequest(requestId, CHANNEL_REQUEST_RESULT_TIMEOUT).get();
        if (!optionalResponse.isPresent()) {
            log.warn("Attempted to open a message channel for " + intention + ", but received no response within the given timeout");
            return Optional.empty();
        }

        // on response, register the local endpoint to handle this channel
        ChannelCreationResponse response = (ChannelCreationResponse) optionalResponse.get();
        if (!response.isSuccess()) {
            log.warn("Failed to open a message channel for " + intention + "; if you have access to the relay's log files, "
                + "you may inspect them for details");
            return Optional.empty();
        } else {
            log.debug("Successfully opened channel " + response.getChannelId() + " for " + intention);
            return Optional.of(response);
        }
    }

    @Override
    protected void onSessionStateChanged(UplinkSessionState oldState, UplinkSessionState newState) {
        // TODO add "in terminal state already" sanity check?
        if (newState == UplinkSessionState.ACTIVE) {
            sessionEventHandler.onSessionActivating(getAssignedNamespaceId(), getDestinationIdPrefix());
        }
        if (oldState == UplinkSessionState.ACTIVE) {
            sessionEventHandler.onActiveSessionTerminating();
        }
    }

    @Override
    protected void onTerminalStateReached(UplinkSessionState newState) {
        sessionEventHandler.onSessionInFinalState();
    }

    @Override
    protected void handleFatalError(UplinkProtocolErrorType errorType, String errorMessage) {
        if (getState() != UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR) {
            sessionEventHandler.onFatalSessionError(errorType, "Connection closed by the remote side: " + errorMessage);
        }
        super.handleFatalError(errorType, errorMessage);
    }

    @Override
    public CommonUplinkLowLevelProtocolWrapper getLowLevelProtocolWrapper() {
        return lowLevelProtocolWrapper;
    }

    @Override
    protected String getRemoteSideInformationString() {
        return "Uplink server"; // TODO add host information if available
    }

}
