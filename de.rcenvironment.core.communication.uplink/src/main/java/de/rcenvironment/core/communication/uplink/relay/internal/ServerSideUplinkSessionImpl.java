/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.internal;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
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
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLog.LogEntry;
import de.rcenvironment.core.utils.common.AuditLogIds;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Default {@link ServerSideUplinkSession} implementation.
 *
 * @author Robert Mischke
 */
public class ServerSideUplinkSessionImpl extends AbstractUplinkSessionImpl implements ServerSideUplinkSession {

    private static final boolean HEARTBEAT_LOGGING_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.heartbeat");

    private final String sessionContextInfoString; // provided by network connection layer

    private final String localSessionId;

    private final CommonUplinkLowLevelProtocolWrapper protocolWrapper;

    private final ServerSideUplinkEndpointService serverSideUplinkEndpointService;

    private final UplinkProtocolMessageConverter messageConverter;

    private String loginAccountName;

    private Random random = new Random(); // thread safe

    private final class ServerSideUplinkLowLevelEventHandlerImpl implements UplinkConnectionLowLevelEventHandler {

        private final ServerSideUplinkEndpointService serverSideUplinkEndpointService;

        private final String clientInformationString;

        private ServerSideUplinkLowLevelEventHandlerImpl(ServerSideUplinkEndpointService serverSideUplinkEndpointService,
            String clientInformationString) {
            this.serverSideUplinkEndpointService = serverSideUplinkEndpointService;
            this.clientInformationString = clientInformationString;
        }

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
            // make sure the received protocol version offer is not mirrored, as this may be confusing
            outgoingData.remove(UplinkProtocolConstants.HANDSHAKE_KEY_PROTOCOL_VERSION_OFFER);

            // protocol compatibility check (strict equality for now)
            String clientProtocolVersion = incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_PROTOCOL_VERSION_OFFER);
            if (StringUtils.isNullorEmpty(clientProtocolVersion)) {
                // remote error message
                throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.INVALID_HANDSHAKE_DATA,
                    "Missing handshake version information", true);
            }
            if (clientProtocolVersion.equals(UplinkProtocolConstants.DEFAULT_PROTOCOL_VERSION)) {
                sessionState.setProtocolVersion(clientProtocolVersion);
            } else if (clientProtocolVersion.equals(UplinkProtocolConstants.LEGACY_PROTOCOL_VERSION_0_1_CLIENT_VALUE)) {
                sessionState.setProtocolVersion(UplinkProtocolConstants.LEGACY_PROTOCOL_VERSION_0_1); // rewrite for consistent parsing
            } else {
                // remote error message
                throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.PROTOCOL_VERSION_MISMATCH,
                    "The client and server are using incompatible versions of the Uplink protocol (" + clientProtocolVersion
                        + " vs. " + UplinkProtocolConstants.DEFAULT_PROTOCOL_VERSION
                        + "). Please use a client version matching the server you are connecting to.",
                    true);
            }
            // send the effective protocol version back to the client to support future protocol version negotiation
            outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_EFFECTIVE_PROTOCOL_VERSION, sessionState.getProtocolVersion());

            // store the received value as-is; in the long run, this may benefit from some sanity checks (reasonable length etc)
            sessionState.setClientVersionInfo(incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_CLIENT_VERSION_INFO));

            final String effectiveAccountName = determineEffectiveAccountName(loginAccountName);
            final String effectiveSessionQualifier = determineEffectiveSessionQualifier(loginAccountName, incomingData);

            synchronized (sessionState) {
                sessionState.setEffectiveAccountName(effectiveAccountName);
                sessionState.setEffectiveSessionQualifier(effectiveSessionQualifier);
            }

            // TODO (p1) 11.0: IMPORTANT: to make this secure, account names and session qualifiers/"client ids" MUST be prevented
            // from ending with the padding character! the current state is sufficient for the threat model of an "experimental"
            // feature in 10.0, as it does not cover malicious actors with access to valid SSH credentials.
            final String assignedNamespaceId = deriveAssignedNamespaceId(effectiveAccountName, effectiveSessionQualifier);

            final boolean namespaceAcquired =
                serverSideUplinkEndpointService.attemptToAssignNamespaceId(assignedNamespaceId, ServerSideUplinkSessionImpl.this);
            if (!namespaceAcquired) {
                // remote error message
                throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.CLIENT_NAMESPACE_COLLISION,
                    "The combination of account name \"" + effectiveAccountName
                        + "\" and client ID \"" + effectiveSessionQualifier
                        + "\" is already in use. To allow parallel logins, use a different client ID for each client.",
                    true);
            }

            setAssignedNamespaceId(assignedNamespaceId);
            updateLogDescriptor();
            outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_ASSIGNED_NAMESPACE_ID, assignedNamespaceId);

            // check for the presence of certain development/test handshake flags
            // TODO rename to clarify that this tests a ProtocolException?
            if (incomingData.containsKey(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE)) {
                // note: not technically guaranteed that this matches the actual behavior in other cases
                throw new ProtocolException(incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE));
            }
            if (incomingData.containsKey(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_REFUSED_CONNECTION)) {
                // for now, using the "namespace collision" error type for simulation; adapt as needed
                throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.CLIENT_NAMESPACE_COLLISION,
                    incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_REFUSED_CONNECTION), true);
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

        @Override
        public void onHandshakeComplete() {
            writeAuditLogEntryOnSessionActivating();
            markHandshakeSuccessful();
        }

        @Override
        public void onHandshakeFailedOrConnectionRefused(UplinkConnectionRefusedException e) {
            writeAuditLogEntryOnSessionRefused(e);
            markHandshakeFailed(e);
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
            handleFatalError(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR, e.toString());
        }

        @Override
        public void onStreamWriteError(IOException e) {
            log.warn("Stream write error: " + e.toString());
            handleStreamWriteError(e);
        }

        @Override
        public void onNonProtocolError(Exception exception) {
            handleFatalError(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR, exception.toString());
        }

        @Override
        public void onMessageBlock(long channelId, MessageBlock messageBlock) {
            // except for the initial handshake, delegate all message processing to the backend service
            if (messageBlock.getType() == MessageType.HEARTBEAT_RESPONSE && channelId == UplinkProtocolConstants.DEFAULT_CHANNEL_ID) {
                sessionState.markHeartbeatResponseReceived();
                // for now, stop default heartbeat response handling here, as endpoint handlers do not make use of them; could be changed
                return;
            }

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
    }

    public ServerSideUplinkSessionImpl(StreamConnectionEndpoint connectionEndpoint, String loginAccountName,
        String sessionContextInfoString, ServerSideUplinkEndpointService serverSideUplinkEndpointService,
        ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        super(concurrencyUtilsFactory);
        this.loginAccountName = loginAccountName;
        this.sessionContextInfoString = sessionContextInfoString;
        this.serverSideUplinkEndpointService = serverSideUplinkEndpointService;
        this.localSessionId = serverSideUplinkEndpointService.assignSessionId(ServerSideUplinkSessionImpl.this);
        updateLogDescriptor(); // updated once the namespace id is available
        this.messageConverter = new UplinkProtocolMessageConverter("server session " + localSessionId);

        UplinkConnectionLowLevelEventHandler serverSideUplinkLowLevelEventHandler =
            new ServerSideUplinkLowLevelEventHandlerImpl(serverSideUplinkEndpointService, sessionContextInfoString);

        this.protocolWrapper =
            new ServerSideUplinkLowLevelProtocolWrapper(connectionEndpoint, serverSideUplinkLowLevelEventHandler, localSessionId);
    }

    @Override
    public boolean runSession() {
        log.debug(StringUtils.format("%sStarting Uplink session for %s (%s)", logPrefix, loginAccountName, sessionContextInfoString));
        final long startTime = System.currentTimeMillis();
        try {
            protocolWrapper.runSession();
            return getState() == UplinkSessionState.CLEAN_SHUTDOWN;
        } finally {
            // note: namespace releasing is performed when the "active" state of the session is reset
            final UplinkSessionState finalState = getState();
            final long execTimeMsec = System.currentTimeMillis() - startTime;
            // whatever led to the end of the session, always make sure the session is terminated
            validateProperSessionRelease();
            log.debug(
                StringUtils.format("%sUplink session for user \"%s\" (%s) terminated in final state %s, duration: %d msec", logPrefix,
                    loginAccountName, sessionContextInfoString, finalState.name(), execTimeMsec));
            if (!finalState.isTerminal()) {
                log.warn(logPrefix + "Session terminated in non-terminal state " + finalState.name());
            }
        }
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
            if (sessionState.isHeartbeatSendingEnabled()) {
                scheduleHeartbeatSendTrigger(); // schedule the first heartbeat; on success, it will schedule the next one
            }
        }

        if (newState == UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR || oldState == UplinkSessionState.ACTIVE) {
            // must be done before setting the active state to "false", as that will trigger a consistency check of no assigned namespace
            releaseNamespaceIdIfPresent(oldState == UplinkSessionState.ACTIVE);
        }

        if (oldState == UplinkSessionState.ACTIVE) {
            serverSideUplinkEndpointService.setSessionActiveState(ServerSideUplinkSessionImpl.this, false);
        }

    }

    private void scheduleHeartbeatSendTrigger() {
        final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();
        // note: "random" is thread-safe
        int delay = UplinkProtocolConstants.SERVER_TO_CLIENT_HEARTBEAT_SEND_INVERVAL_AVERAGE
            - UplinkProtocolConstants.SERVER_TO_CLIENT_HEARTBEAT_SEND_INVERVAL_SPREAD / 2
            + random.nextInt(UplinkProtocolConstants.SERVER_TO_CLIENT_HEARTBEAT_SEND_INVERVAL_SPREAD);
        asyncTaskService.scheduleAfterDelay("Send Uplink heartbeat after delay", () -> {
            if (enqueueHeartbeatMessage()) {
                // success -> schedule the next
                scheduleHeartbeatSendTrigger();
            }
        }, delay);
    }

    private boolean enqueueHeartbeatMessage() {
        if (getState() == UplinkSessionState.ACTIVE) {
            if (!sessionState.validateHeartbeatResponseIfExpected()) {
                // if validation failed, do not send another heartbeat, as the session will shut down
                return false;
            }
            try {
                if (HEARTBEAT_LOGGING_ENABLED) {
                    log.debug(logPrefix + "Enqueueing heartbeat message");
                }
                enqueueMessageBlockForSending(0, new MessageBlock(MessageType.HEARTBEAT));
                sessionState.markHeartbeatSent();
                return true;
            } catch (IOException e) {
                // typically not relevant for the end user, so log this as DEBUG
                log.debug(logPrefix + "Error while scheduling an Uplink heartbeat: " + e.toString());
                return false;
            }
        } else {
            if (HEARTBEAT_LOGGING_ENABLED) {
                log.debug(logPrefix + "Stopping hearbeat sending as session is in state " + getState().name());
            }
            return false;
        }
    }

    @Override
    protected void onTerminalStateReached(UplinkSessionState newState) {
        writeAuditLogEntryOnSessionTerminating(newState);
    }

    @Override
    protected void handleFatalError(UplinkProtocolErrorType errorType, String errorMessage) {
        super.handleFatalError(errorType, errorMessage);
    }

    @Override
    protected CommonUplinkLowLevelProtocolWrapper getLowLevelProtocolWrapper() {
        return protocolWrapper;
    }

    @Override
    protected String getRemoteSideInformationString() {
        // TODO pre-convert to field for slightly better efficiency; can be used in several other places, too
        return StringUtils.format("user \"%s\" (%s)", loginAccountName, sessionContextInfoString);
    }

    private void releaseNamespaceIdIfPresent(boolean sessionWasActive) {
        final Optional<String> assignedNamespaceId = getAssignedNamespaceIdIfAvailable();
        if (assignedNamespaceId.isPresent()) {
            serverSideUplinkEndpointService.releaseNamespaceId(assignedNamespaceId.get(), ServerSideUplinkSessionImpl.this);
            sessionState.setNamespaceIdReleased();
            updateLogDescriptor();
        } else {
            if (sessionWasActive) {
                log.warn(logPrefix + "Session had no namespace assigned when leaving the ACTIVE state");
            }
        }
    }

    private String determineEffectiveAccountName(String accountNameInput) {
        // warn and truncate if the login name is longer than the significant character count
        final String effectiveAccountName;
        if (accountNameInput.length() > UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS) {
            String truncated =
                accountNameInput.substring(0, UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS);
            log.warn(StringUtils.format(
                "Only the first %d characters of the login name '%s' ('%s') will be used for the client's unique identity; "
                    + "if possible, use login names that do not exceed %d characters",
                UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS, accountNameInput, truncated,
                UplinkProtocolConstants.SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS));
            effectiveAccountName = truncated;
        } else {
            effectiveAccountName = accountNameInput;
        }
        return effectiveAccountName;
    }

    private String determineEffectiveSessionQualifier(String accountNameInput, Map<String, String> incomingData) {
        String clientSessionQualifier = incomingData.get(UplinkProtocolConstants.HANDSHAKE_KEY_SESSION_QUALIFIER);

        // check and sanitize the session qualifier/"client id"
        final String effectiveSessionQualifier;
        if (StringUtils.isNullorEmpty(clientSessionQualifier)) {
            clientSessionQualifier = UplinkProtocolConstants.SESSION_QUALIFIER_DEFAULT;
            log.debug(logPrefix + "An Uplink client using account '" + accountNameInput
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

    protected void validateProperSessionRelease() {
        final Optional<String> assignedNamespaceId = getAssignedNamespaceIdIfAvailable();
        if (assignedNamespaceId.isPresent() && !sessionState.isNamespaceIdReleased()) {
            log.error(logPrefix
                + "Found attached namespace " + assignedNamespaceId.get() + " when it should already have been released; "
                + "please report this error and provide the related debug.log file, if possible");
        }
    }

    private void writeAuditLogEntryOnSessionActivating() {
        LogEntry entry = AuditLog.newEntry(AuditLogIds.UPLINK_SESSION_START);
        addCommonSessionStartInfoToAuditLogEntry(entry);
        addVersionInfoToAuditLogEntry(entry);
        AuditLog.append(entry);
    }

    private void writeAuditLogEntryOnSessionRefused(UplinkConnectionRefusedException e) {
        LogEntry entry = AuditLog.newEntry(AuditLogIds.UPLINK_SESSION_REFUSE)
            .set(AUDIT_LOG_KEY_REASON, e.getMessage());
        addCommonSessionStartInfoToAuditLogEntry(entry);
        addVersionInfoToAuditLogEntry(entry);
        AuditLog.append(entry);
    }

    private void addCommonSessionStartInfoToAuditLogEntry(LogEntry entry) {
        String effectiveAccountName = sessionState.getEffectiveAccountName();
        entry
            .set(AUDIT_LOG_KEY_SESSION_ID, localSessionId)
            .set(AUDIT_LOG_KEY_CONTEXT, sessionContextInfoString)
            .set(AUDIT_LOG_KEY_EFFECTIVE_LOGIN_NAME, effectiveAccountName)
            .set(AUDIT_LOG_KEY_EFFECTIVE_CLIENT_ID, sessionState.getEffectiveSessionQualifier());
        Optional<String> assignedNamespaceIdIfAvailable = sessionState.getAssignedNamespaceIdIfAvailable();
        if (assignedNamespaceIdIfAvailable.isPresent()) {
            entry.set(AUDIT_LOG_KEY_NAMESPACE, assignedNamespaceIdIfAvailable.get());
        }
        if (!effectiveAccountName.equals(loginAccountName)) {
            entry.set(AUDIT_LOG_KEY_ORIGINAL_LOGIN_NAME, loginAccountName);
        }
    }

    private void writeAuditLogEntryOnSessionTerminating(UplinkSessionState newState) {
        AuditLog.append(AuditLog.newEntry(AuditLogIds.UPLINK_SESSION_CLOSE)
            .set(AUDIT_LOG_KEY_SESSION_ID, localSessionId)
            .set(AUDIT_LOG_KEY_CONTEXT, sessionContextInfoString)
            .set(AUDIT_LOG_KEY_FINAL_STATE, newState.name()));
    }

    private void addVersionInfoToAuditLogEntry(LogEntry entry) {
        synchronized (sessionState) {
            String clientVersionInfo = sessionState.getClientVersionInfo();
            if (clientVersionInfo != null) {
                entry.set(AUDIT_LOG_KEY_CLIENT_VERSION_INFO, clientVersionInfo);
            }
            String protocolVersion = sessionState.getProtocolVersion();
            if (protocolVersion != null) {
                entry.set(AUDIT_LOG_KEY_PROTOCOL_VERSION, protocolVersion);
            }
        }
    }

}
