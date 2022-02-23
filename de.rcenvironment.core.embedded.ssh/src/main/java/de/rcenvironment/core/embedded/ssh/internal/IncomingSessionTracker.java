/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLog.LogEntry;
import de.rcenvironment.core.utils.common.AuditLogIds;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Tracks events related to an abstract network connection and/or session initiated by a remote client. Initially designed for SSH sessions,
 * but intended to be reusable for other connection types with minor modifications. (In that case, it should be moved out of the SSH bundle
 * into a common package.)
 *
 * @param <T> the type of the primary key to use; typically the session object itself
 *
 * @author Robert Mischke
 */
public class IncomingSessionTracker<T> {

    public interface SessionHandle {

        SessionHandle addLogData(Map<String, String> data);

        SessionHandle addLogData(String key, String value);

        /*
         * See {@link AuditLog.LogEntry#set(String, int)} for reasoning and behavior.
         */
        SessionHandle addLogData(String key, int value);

        SessionHandle registerAuthenticationSuccess(String loginName, String method);

        SessionHandle registerAuthenticationFailure(String loginName, String method, String refusalReason);

        SessionHandle registerDataStreamEOF();

        /**
         * Increases or decreases a single counter for custom purposes and returns the new value. Note that no overflow/underflow checking
         * is performed; the new value is simply "old value + delta".
         * 
         * @param delta the value to add
         * @return the new value
         */
        int modifyCustomCounter(int delta);

        int getAuthenticationFailureCount();
    }

    /**
     * Internal data holder. For simplicity, there are no get/set methods or implicit synchronization. Any field access MUST only occur
     * while synchronizing on the holder object.
     *
     * @author misc_ro
     */
    private static class SessionStateHolder implements SessionHandle {

        private static final Log sharedLog = LogFactory.getLog(SessionStateHolder.class);

        private static final String EVENT_LOG_KEY_CONNECTION_TYPE = "type";

        private static final String EVENT_LOG_KEY_INTERNAL_ID = "connection_id";

        private static final String EVENT_LOG_KEY_LOGIN_NAME = "login_name";

        private static final String EVENT_LOG_KEY_CLOSE_REASON = "close_reason";

        private static final String EVENT_LOG_KEY_LAST_PREFIX = "last_"; // for auth method, login name, auth failure reason

        private static final String EVENT_LOG_KEY_AUTH_METHOD = "auth_method";

        private static final String EVENT_LOG_KEY_NUM_AUTH_FAILURES = "auth_failure_count";

        private static final String EVENT_LOG_KEY_AUTH_FAILURE_REASON = "auth_failure_reason";

        private static final String EVENT_LOG_KEY_SESSION_DURATION_MSEC = "duration_msec";

        private final Instant startTime = Instant.now();

        private final Map<String, String> logData = new HashMap<>();

        private boolean hadIncomingEOF;

        private int authenticationFailureCount;

        private String lastAuthLoginName;

        private String lastAuthMethod;

        private String lastAuthFailureReason;

        private boolean hadAuthenticationSuccess;

        private boolean wasRegularlyClosed;

        private String internalSessionId;

        private int customCounter;

        public synchronized void setInternalSessionId(String id) {
            internalSessionId = id;
            storeLogDataKeyValuePair(EVENT_LOG_KEY_INTERNAL_ID, id);
        }

        public synchronized void setConnectionType(String connectionTypeLogId) {
            storeLogDataKeyValuePair(EVENT_LOG_KEY_CONNECTION_TYPE, connectionTypeLogId);
        }

        /**
         * Marks the end of the associated session, and the impending disposal of this holder object.
         * <p>
         * The surrounding code must make sure that this is called exactly once.
         * 
         * @param regularClose whether this session has ended "normally"; false would indicate a fallback close event (e.g. by garbage
         *        collection or on shutdown)
         */
        public synchronized void registerEndOfSession(boolean regularClose) {
            sharedLog.debug("Closing session " + internalSessionId);
            this.wasRegularlyClosed = regularClose;
            writeFinalLogEntry();
        }

        @Override
        public synchronized SessionHandle addLogData(Map<String, String> data) {
            // similar to Map.putAll(), but log replaced entries
            for (Entry<String, String> e : data.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                storeLogDataKeyValuePair(key, value);
            }
            return this;
        }

        @Override
        public synchronized SessionHandle addLogData(String key, String value) {
            storeLogDataKeyValuePair(key, value);
            return this;
        }

        @Override
        public synchronized SessionHandle addLogData(String key, int value) {
            storeLogDataKeyValuePair(key, Integer.toString(value));
            return this;
        }

        @Override
        public synchronized SessionHandle registerAuthenticationSuccess(String loginName, String method) {
            if (hadAuthenticationSuccess) {
                // this seems to happen regularly on key auth; also, serves as a general consistency check
                if (lastAuthLoginName.equals(loginName) && lastAuthMethod.equals(method)) {
                    sharedLog.debug("Ignoring duplicate successful authentication event with same parameters: user '" + loginName
                        + "', method '" + method + "'");
                    return this;
                } else {
                    sharedLog.warn("Received duplicate successful authentication event with different parameters: user '" + loginName
                        + "', method " + method + "; writing second event log entry for visibility");
                }
            }
            hadAuthenticationSuccess = true;
            lastAuthLoginName = loginName;
            lastAuthMethod = method;
            writeAuthenticationSuccessLogEntry();
            return this;
        }

        @Override
        public synchronized SessionHandle registerAuthenticationFailure(String loginName, String method, String failureReason) {

            // This does not make any semantical sense, but there may be technical reasons why this might occur, e.g. different
            // authentication schemes being tried in a certain sequence. In that case, the caller should filter these out.
            // If this is impossible for some reason, adapt this code to allow this, e.g. with an extra flag.
            if (hadAuthenticationSuccess) {
                throw new IllegalStateException("Attempted to register an authentication failure after "
                    + "authentication success for session id " + internalSessionId);
            }

            lastAuthLoginName = loginName;
            lastAuthMethod = method;
            lastAuthFailureReason = failureReason; // TODO set proper reasons
            authenticationFailureCount++;

            writeAuthenticationFailureLogEntry();

            return this;
        }

        @Override
        public synchronized SessionHandle registerDataStreamEOF() {
            hadIncomingEOF = true;
            return this;
        }

        @Override
        public int modifyCustomCounter(int delta) {
            customCounter += delta;
            return customCounter;
        }

        @Override
        public synchronized int getAuthenticationFailureCount() {
            return authenticationFailureCount;
        }

        // note: add "allow overwrite without warning" flag if needed
        private void storeLogDataKeyValuePair(String key, String value) {
            String replaced = logData.put(key, value);
            if (replaced != null) {
                if (replaced.equals(value)) {
                    sharedLog.warn("Repeatedly attached the same log data for session " + internalSessionId + ", key: " + key
                        + ", value: " + value);
                } else {
                    sharedLog.warn("Replacing log data for session " + internalSessionId + ", key: " + key
                        + ", old value: " + replaced + ", new value: " + value);
                }
            }
        }

        private void writeAuthenticationSuccessLogEntry() {
            LogEntry logEntry = AuditLog.newEntry(AuditLogIds.CONNECTION_INCOMING_ACCEPT);

            // apply collected log data
            logEntry.setAll(logData);

            // add auth-specific entries
            logEntry.set(EVENT_LOG_KEY_LOGIN_NAME, lastAuthLoginName);
            logEntry.set(EVENT_LOG_KEY_AUTH_METHOD, lastAuthMethod);
            if (authenticationFailureCount != 0) {
                logEntry.set(EVENT_LOG_KEY_NUM_AUTH_FAILURES, Integer.toString(authenticationFailureCount));
            }

            AuditLog.append(logEntry);
        }

        private void writeAuthenticationFailureLogEntry() {
            LogEntry logEntry = AuditLog.newEntry(AuditLogIds.CONNECTION_INCOMING_AUTH_FAILURE);

            // apply collected log data
            logEntry.setAll(logData);

            // add auth-specific entries
            logEntry.set(EVENT_LOG_KEY_NUM_AUTH_FAILURES, Integer.toString(authenticationFailureCount));
            logEntry.set(EVENT_LOG_KEY_LOGIN_NAME, lastAuthLoginName);
            logEntry.set(EVENT_LOG_KEY_AUTH_METHOD, lastAuthMethod);
            logEntry.set(EVENT_LOG_KEY_AUTH_FAILURE_REASON, lastAuthFailureReason);

            AuditLog.append(logEntry);
        }

        private void writeFinalLogEntry() {
            boolean hadAuthenticationFailure = authenticationFailureCount != 0;

            // "final" ensures that these are set exactly once
            final String mainEventType;
            final String closeReason;

            if (!wasRegularlyClosed) {
                // log unusual cases: closed by some sort of session garbage collection, or leftover sessions on shutdown
                mainEventType = AuditLogIds.CONNECTION_INCOMING_CLOSE;
                closeReason = "fallback";
            } else {
                if (hadAuthenticationSuccess) {
                    mainEventType = AuditLogIds.CONNECTION_INCOMING_CLOSE;
                    if (hadIncomingEOF) {
                        // TODO >10.2.1 (p2) this commonly occurs at the normal end of Uplink sessions; could be improved
                        closeReason = "end of stream";
                    } else {
                        closeReason = "regular";
                    }
                } else if (hadAuthenticationFailure) {
                    mainEventType = AuditLogIds.CONNECTION_INCOMING_REFUSE;
                    closeReason = "auth failure";
                } else {
                    mainEventType = AuditLogIds.CONNECTION_INCOMING_REFUSE;
                    // no auth attempt registered -> assume auth timeout
                    closeReason = "auth timeout";
                }
            }

            LogEntry logEntry = AuditLog.newEntry(mainEventType);

            // apply collected log data
            logEntry.setAll(logData);

            // add authentication result data
            if (hadAuthenticationSuccess) {
                // success with or without a previous failure -> log successful attempt parameters
                logEntry.set(EVENT_LOG_KEY_LOGIN_NAME, lastAuthLoginName);
                logEntry.set(EVENT_LOG_KEY_AUTH_METHOD, lastAuthMethod);
                if (hadAuthenticationFailure) {
                    // always log the number of login failures, even if redeemed by subsequent success
                    logEntry.set(EVENT_LOG_KEY_NUM_AUTH_FAILURES, Integer.toString(authenticationFailureCount));
                }
            } else if (hadAuthenticationFailure) {
                // failure without subsequent success -> log last login attempt parameters
                logEntry.set(EVENT_LOG_KEY_NUM_AUTH_FAILURES, Integer.toString(authenticationFailureCount));
                logEntry.set(EVENT_LOG_KEY_LAST_PREFIX + EVENT_LOG_KEY_LOGIN_NAME, lastAuthLoginName);
                logEntry.set(EVENT_LOG_KEY_LAST_PREFIX + EVENT_LOG_KEY_AUTH_METHOD, lastAuthMethod);
                logEntry.set(EVENT_LOG_KEY_LAST_PREFIX + EVENT_LOG_KEY_AUTH_FAILURE_REASON, lastAuthFailureReason);
            }

            // add session close reason/trigger
            logEntry.set(EVENT_LOG_KEY_CLOSE_REASON, closeReason);

            // add duration
            String durationString = Long.toString(Duration.between(startTime, Instant.now()).toMillis());
            logEntry.set(EVENT_LOG_KEY_SESSION_DURATION_MSEC, durationString);

            AuditLog.append(logEntry);
        }

    }

    private final Map<T, SessionStateHolder> stateHoldersBySession = new HashMap<>();

    private final String connectionTypeLogId;

    private final Log log = LogFactory.getLog(getClass());

    public IncomingSessionTracker(String connectionTypeLogId) {
        this.connectionTypeLogId = connectionTypeLogId;
    }

    public SessionHandle registerSessionStart(T session) {
        SessionStateHolder stateHolder = new SessionStateHolder();

        // set basic data
        stateHolder.setConnectionType(connectionTypeLogId);
        stateHolder.setInternalSessionId(renderInternalIdOfSession(session));

        synchronized (stateHoldersBySession) {
            SessionStateHolder existing = stateHoldersBySession.get(session);
            if (existing != null) {
                throw new IllegalStateException(
                    "Requested duplicate registration of session object " + renderLogDescriptorOfSession(session));
            }
            stateHoldersBySession.put(session, stateHolder);
            return stateHolder;
        }
    }

    public SessionHandle forSession(T session) throws OperationFailureException {
        return getStateHolder(session);
    }

    public void registerSessionClosed(T session, boolean ignoreDuplicateCalls) {
        SessionStateHolder stateHolder;
        synchronized (stateHoldersBySession) {
            stateHolder = stateHoldersBySession.remove(session);
            if (stateHolder == null) {
                if (ignoreDuplicateCalls) {
                    return; // session is already unregistered; ignore this silently, as requested
                } else {
                    throw new IllegalStateException(
                        "Requested to close unregistered or already closed session " + renderLogDescriptorOfSession(session));
                }
            }
        }
        stateHolder.registerEndOfSession(true);
    }

    /**
     * In normal operation, this method should do nothing, as any started session has already been actively closed. If there is any unclosed
     * session object remaining, however, it is logged as best as possible for inspection and/or debugging.
     * 
     * @return the number of leftover sessions, to allow active checking by the caller; always zero in normal operation
     */
    public int logLeftoverSessions() {

        synchronized (stateHoldersBySession) {
            for (Entry<T, SessionStateHolder> e : stateHoldersBySession.entrySet()) {
                T session = e.getKey();
                SessionStateHolder stateHolder = e.getValue();
                log.error("Unfinished session state after cleanup should have occurred; "
                    + "writing best-effort entry to event log. Session object: " + renderLogDescriptorOfSession(session));
                stateHolder.registerEndOfSession(false); // false = abnormal/fallback close event
            }
            int leftoverCount = stateHoldersBySession.size();
            stateHoldersBySession.clear(); // remove all disposed entries for consistency
            return leftoverCount;
        }
    }

    /**
     * @param session the session to get the state holder for
     * @return the session's {@link SessionStateHolder}, if present
     * @throws OperationFailureException if the session was never registered, or was already closed
     */
    private SessionStateHolder getStateHolder(T session) throws OperationFailureException {
        synchronized (stateHoldersBySession) {
            SessionStateHolder stateHolder = stateHoldersBySession.get(session);
            if (stateHolder == null) {
                throw new OperationFailureException(
                    "Unregistered session or session already closed: " + renderLogDescriptorOfSession(session));
            }
            return stateHolder;
        }
    }

    private String renderLogDescriptorOfSession(T session) {
        return StringUtils.format("%s [%s]", session.toString(), renderInternalIdOfSession(session));
    }

    private String renderInternalIdOfSession(T session) {
        // rendering the hash code to an integer for now; could be rendered to hex or similar as well, as long as it is unique
        return Integer.toString(System.identityHashCode(session));
    }

}
