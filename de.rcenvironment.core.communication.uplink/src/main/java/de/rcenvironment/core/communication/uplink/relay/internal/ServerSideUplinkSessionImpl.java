/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.communication.uplink.network.internal.CommonUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.ServerSideUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkConnectionLowLevelEventHandler;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkConnectionRefusedException;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSessionState;
import de.rcenvironment.core.communication.uplink.session.internal.AbstractUplinkSessionImpl;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Default {@link ServerSideUplinkSession} implementation.
 *
 * @author Robert Mischke
 */
public class ServerSideUplinkSessionImpl extends AbstractUplinkSessionImpl implements ServerSideUplinkSession {

    private final String clientInformationString; // provided by network connection layer

    private final String localSessionId;

    private String logDescriptor;

    private final CommonUplinkLowLevelProtocolWrapper protocolWrapper;

    private final ServerSideUplinkEndpointService serverSideUplinkEndpointService;

    private final UplinkProtocolMessageConverter messageConverter;

    public ServerSideUplinkSessionImpl(String clientInformationString, String loginAccountName, InputStream inputStream,
        OutputStream outputStream,
        ServerSideUplinkEndpointService serverSideUplinkEndpointService, ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        super(concurrencyUtilsFactory);
        this.clientInformationString = clientInformationString;
        this.serverSideUplinkEndpointService = serverSideUplinkEndpointService;
        this.localSessionId = serverSideUplinkEndpointService.assignSessionId(ServerSideUplinkSessionImpl.this);
        updateLogDescriptor(); // updated once the namespace id is available
        this.messageConverter = new UplinkProtocolMessageConverter("server session " + localSessionId);
        this.protocolWrapper =
            new ServerSideUplinkLowLevelProtocolWrapper(inputStream, outputStream, new UplinkConnectionLowLevelEventHandler() {

                @Override
                public void provideOrProcessHandshakeData(Map<String, String> incomingData, Map<String, String> outgoingData)
                    throws ProtocolException, UplinkConnectionRefusedException {

                    markClientHandshakeSentOrReceived();

                    // consistency check: this should only be called for processing received client data, and producing the response
                    Objects.requireNonNull(incomingData);
                    Objects.requireNonNull(outgoingData);

                    // for development, simply echo all entries that the client sent; may be reduced or removed in the future.
                    // note that if this is done, the received data should be added first so it cannot override server-side data.
                    outgoingData.putAll(incomingData);

                    // protocol compatibility check (strict equality for now)
                    String clientProtocolVersion = incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_HIGH_LEVEL_PROTOCOL_VERSION);
                    if (StringUtils.isNullorEmpty(clientProtocolVersion)) {
                        refuseConnection(UplinkProtocolErrorType.INVALID_HANDSHAKE_DATA, "Missing handshake version information");
                    }
                    if (!clientProtocolVersion.equals(UplinkProtocolConstants.HIGH_LEVEL_PROTOCOL_VERSION)) {
                        refuseConnection(UplinkProtocolErrorType.PROTOCOL_VERSION_MISMATCH,
                            "The client and server are using incompatible versions of the Uplink protocol (" + clientProtocolVersion
                                + " vs. " + UplinkProtocolConstants.HIGH_LEVEL_PROTOCOL_VERSION
                                + "). Please use a client version matching the server you are connecting to.");
                    }

                    final String effectiveAccountName = determineEffectiveAccountName(loginAccountName);
                    final String effectiveSessionQualifier = determineEffectiveSessionQualifier(loginAccountName, incomingData);

                    // TODO (p1) 11.0: IMPORTANT: to make this secure, account names and session qualifiers/"client ids" MUST be prevented
                    // from ending with the padding character! the current state is sufficient for the threat model of an "experimental"
                    // feature in 10.0, as it does not cover malicious actors with access to valid SSH credentials.
                    final String assignedNamespaceId = deriveAssignedNamespaceId(effectiveAccountName, effectiveSessionQualifier);

                    final boolean namespaceAcquired =
                        serverSideUplinkEndpointService.attemptToAssignNamespaceId(assignedNamespaceId, ServerSideUplinkSessionImpl.this);
                    if (!namespaceAcquired) {
                        refuseConnection(UplinkProtocolErrorType.CLIENT_NAMESPACE_COLLISION,
                            "The combination of account name \"" + effectiveAccountName
                                + "\" and client ID \"" + effectiveSessionQualifier
                                + "\" is already in use. To allow parallel logins, use a different client ID for each client.");
                    }

                    setAssignedNamespaceId(assignedNamespaceId);
                    updateLogDescriptor();
                    outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_ASSIGNED_NAMESPACE_ID, assignedNamespaceId);

                    // check for the presence of certain development/test handshake flags
                    // TODO rename to clarify that this tests a ProtocolException?
                    if (incomingData.containsKey(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE)) {
                        // note: not technically guaranteed that this matches the actual behavior in other cases
                        setSessionState(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR);
                        throw new ProtocolException(incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE));
                    }
                    if (incomingData.containsKey(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_REFUSED_CONNECTION)) {
                        refuseConnection(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR,
                            incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_REFUSED_CONNECTION));
                    }
                    if (incomingData.containsKey(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_RESPONSE_DELAY_ABOVE_TIMEOUT)) {
                        // TODO align with CommonUplinkLowLevelProtocolWrapper#HANDSHAKE_MESSAGE_TIMEOUT; 2 constants for a similar topic
                        try {
                            Thread.sleep(UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC
                                + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);
                        } catch (InterruptedException e) {
                            log.warn("Interrupted while simulating handshake timeout");
                        }
                    }

                    markServerHandshakeSentOrReceived();
                }

                private void refuseConnection(UplinkProtocolErrorType errorType, String errorMessage)
                    throws UplinkConnectionRefusedException {
                    setSessionState(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR);
                    throw new UplinkConnectionRefusedException(errorType, errorMessage);
                }

                @Override
                public void onHandshakeComplete() {
                    setSessionState(UplinkSessionState.ACTIVE);
                }

                @Override
                public void onRegularGoodbyeMessage() {
                    closeServerSideSession();
                }

                @Override
                public void onErrorGoodbyeMessage(UplinkProtocolErrorType errorType, String errorMessage) {
                    log.info(StringUtils.format("Received a remote error message from %s, closing the session: %s [type %s]",
                        clientInformationString, errorMessage, errorType.name()));
                    closeServerSideSession();
                }

                @Override
                public void onNonProtocolError(IOException exception) {
                    log.warn(StringUtils.format("Non-protocol error in session %s (will be closed): %s", clientInformationString,
                        exception.toString()));
                    closeServerSideSession();
                }

                private void closeServerSideSession() {
                    markAsCloseRequestedByRemoteEvent();
                }

                @Override
                public void onMessageBlock(long channelId, MessageBlock messageBlock) {
                    // except for the initial handshake, delegate all message processing to the backend service
                    // TODO merge/align this with client side implementation?
                    incomingMessageQueue.enqueue(() -> {
                        try {
                            serverSideUplinkEndpointService.onMessageBlock(ServerSideUplinkSessionImpl.this, channelId, messageBlock);
                        } catch (ProtocolException e) {
                            // TODO actually handle this
                            log.error("Error processing a message received by server session " + getLocalSessionId(), e);
                        }
                    });
                }

            }, "server session protocol wrapper " + localSessionId);
    }

    @Override
    public boolean runSession() {
        log.info("Handling Uplink session for " + clientInformationString);
        try {
            final long startTime = System.currentTimeMillis();
            try {
                protocolWrapper.runSession();
                return true; // normal finish
            } finally {
                // note: namespace releasing is performed when the "active" state of the session is reset
                final long execTimeMsec = System.currentTimeMillis() - startTime;
                log.debug(StringUtils.format(
                    "Uplink session terminated for %s, duration: %d msec", clientInformationString, execTimeMsec));
            }
        } catch (ProtocolException e) {
            log.error(StringUtils.format("Protocol error in Uplink session for %s: %s", clientInformationString, e.getMessage()));
            return false; // abnormal finish
        } catch (IOException e) {
            // not logging the full stacktrace as it is usually irrelevant, and this case happens frequently
            log.warn(StringUtils.format("I/O error in Uplink session - the client (%s) may have closed the connection: %s",
                clientInformationString, e.toString()));
            // TODO provide a general "error channel" mechanism?
            // try {
            // writeToStream(errorStream, "Uplink server error: " + e.toString());
            // } catch (IOException e2) {
            // log.debug("Failed to send final error message to Uplink client: " + e2.toString());
            // }
            return false; // abnormal finish
        }
    }

    @Override
    public void close() {
        markAsCloseRequestedLocally();
        protocolWrapper.closeOutgoingMessageStream();
    }

    public CommonUplinkLowLevelProtocolWrapper getProtocolWrapper() {
        return protocolWrapper;
    }

    @Override
    public String getLocalSessionId() {
        return localSessionId;
    }

    @Override
    protected void onSessionStateChanged(UplinkSessionState oldState, UplinkSessionState newState) {
        if (newState == UplinkSessionState.ACTIVE) {
            serverSideUplinkEndpointService.setSessionActiveState(ServerSideUplinkSessionImpl.this, true);
        }

        // conditions broken down for maintainability
        final boolean abortingPartialHandshake = newState == UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR
            && oldState == UplinkSessionState.CLIENT_HANDSHAKE_REQUEST_READY;
        if (abortingPartialHandshake || newState == UplinkSessionState.PARTIALLY_CLOSED_BY_LOCAL
            || newState == UplinkSessionState.PARTIALLY_CLOSED_BY_REMOTE) {
            // Note: releasing the namespace here is probably not 100% waterproof, i.e. there may be non-security race conditions. To
            // improve this, either only mark it as "being released" instead, or simply don't do it before whole session has ended,
            // and adapt the consistency check to match. -- misc_ro, Nov 2019
            // must be done before setting the active state to "false", as that will trigger a consistency check of no assigned namespace
            releaseNamespaceIdIfPresent();
        }
        if (oldState == UplinkSessionState.ACTIVE) {
            serverSideUplinkEndpointService.setSessionActiveState(ServerSideUplinkSessionImpl.this, false);
        }
        if (newState == UplinkSessionState.PARTIALLY_CLOSED_BY_REMOTE) {
            protocolWrapper.closeOutgoingMessageStream();
            markAsCloseRequestedLocally();
        }

    }

    private void releaseNamespaceIdIfPresent() {
        final Optional<String> assignedNamespaceId = getAssignedNamespaceIdIfAvailable();
        if (assignedNamespaceId.isPresent()) {
            serverSideUplinkEndpointService.releaseNamespaceId(assignedNamespaceId.get(), ServerSideUplinkSessionImpl.this);
        } else {
            log.debug("Session " + this
                + " had no namespace assigned when could have been released; this can occur if a client never sends a handshake request");
        }
    }

    private String determineEffectiveAccountName(String loginAccountName) {
        // warn and truncate if the login name is longer than the significant character count
        final String effectiveAccountName;
        if (loginAccountName.length() > UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS) {
            String truncated =
                loginAccountName.substring(0, UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS);
            log.warn(StringUtils.format(
                "Only the first %d characters of the login name '%s' ('%s') will be used for the client's unique identity; "
                    + "if possible, use login names that do not exceed %d characters",
                UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS, loginAccountName, truncated,
                UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS));
            effectiveAccountName = truncated;
        } else {
            effectiveAccountName = loginAccountName;
        }
        return effectiveAccountName;
    }

    private String determineEffectiveSessionQualifier(String loginAccountName, Map<String, String> incomingData) {
        String clientSessionQualifier = incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_SESSION_QUALIFIER);

        // check and sanitize the session qualifier/"client id"
        final String effectiveSessionQualifier;
        if (StringUtils.isNullorEmpty(clientSessionQualifier)) {
            clientSessionQualifier = UplinkProtocolConstants.SESSION_QUALIFIER_DEFAULT;
            log.debug("An Uplink client using account '" + loginAccountName
                + "' sent an empty client ID; using '" + clientSessionQualifier + "'");
        }
        if (clientSessionQualifier.length() > UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS) {
            String truncated =
                clientSessionQualifier.substring(0, UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS);
            log.warn(StringUtils.format(
                "Truncating client ID '%s' to '%s' as it exceeds the significant character limit (%d)",
                clientSessionQualifier, truncated, UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS));
            effectiveSessionQualifier = truncated;
        } else {
            effectiveSessionQualifier = clientSessionQualifier;
        }
        return effectiveSessionQualifier;
    }

    private String deriveAssignedNamespaceId(final String effectiveAccountName, final String effectiveSessionQualifier) {
        String namespaceId = org.apache.commons.lang3.StringUtils.rightPad(effectiveAccountName,
            UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS,
            UplinkProtocolConstants.DESTINATION_ID_PREFIX_PADDING_CHARACTER)
            + org.apache.commons.lang3.StringUtils.rightPad(effectiveSessionQualifier,
                UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS,
                UplinkProtocolConstants.DESTINATION_ID_PREFIX_PADDING_CHARACTER);
        if (namespaceId.length() != UplinkProtocolConstants.DESTINATION_ID_PREFIX_LENGTH) {
            throw new IllegalStateException(); // consistency error
        }
        return namespaceId;
    }
}
