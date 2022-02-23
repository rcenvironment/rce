/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

/**
 * Provides event id constants for common ("core") audit log events. Extensions or plugins may provide their own ids in addition to this
 * class, but all code that is part of a standard RCE distribution should place their ids here.
 *
 * @author Robert Mischke
 */
public final class AuditLogIds {

    /**
     * The first event of any application run.
     */
    public static final String APPLICATION_START = "application.start";

    /**
     * Represents the start of a new runtime session of an instance. When the audit log is written to a local file, this event will
     * represent a session using the surrounding profile directory.
     */
    public static final String APPLICATION_SESSION_STARTING = "application.session.start";

    /**
     * Logged when a user has requested the logging instance to shut down, e.g. via GUI, console command, or system shell command. If
     * available, the cause should be logged as the "method" field.
     */
    public static final String APPLICATION_SHUTDOWN_REQUESTED = "application.shutdown.request";

    /**
     * The final event before JVM shutdown.
     */
    public static final String APPLICATION_TERMINATING = "application.terminating";

    /**
     * Logged when a network listen port (typically, TCP) is being (or has been) opened by this instance. Common fields are "type", "port",
     * and "bind_ip"; additional fields are type-specific.
     */
    public static final String NETWORK_SERVERPORT_OPEN = "serverport.open";

    /**
     * Logged when a network listen port (typically, TCP) is being (or has been) closed by this instance. Common fields are "type", "port",
     * and "bind_ip"; additional fields are type-specific.
     */
    public static final String NETWORK_SERVERPORT_CLOSE = "serverport.close";

    /**
     * Logged when a new display name has been assigned to a network node. Relevant for determining which node was behind a given display
     * name at a certain time, and for detecting potential node "impersonation" attempts.
     */
    public static final String NETWORK_NAMING_UPDATE = "network.naming.update";

    /**
     * Logged when an incoming connection has been successfully established. This should only be logged once the connection has been fully
     * validated on the application logic level. Mutually exclusive with the refused/failed event.
     */
    public static final String CONNECTION_INCOMING_ACCEPT = "connection.incoming.accept";

    /**
     * Logged when an incoming connection has failed or been refused for any reason. Mutually exclusive with the success event.
     * <p>
     * SSH authentication timeout, which occurs when q client makes no authentication attempt at all, and a certain time expires, is also
     * represented by this event. It can be recognized by the value of the "reason" field(s).
     */
    public static final String CONNECTION_INCOMING_REFUSE = "connection.incoming.refuse";

    /**
     * Logged on a failed authentication attempt. Whether this failure is fatal for the overall connection attempt or not is
     * implementation-specific.
     * <p>
     * If this is ultimately followed up by a successful authentication attempt (in case another attempt is allowed in the first place),
     * this event should be succeeded with a {@link #CONNECTION_INCOMING_ACCEPT} event. Otherwise, this should always be followed up by a
     * {@link #CONNECTION_INCOMING_REFUSE} event.
     */
    public static final String CONNECTION_INCOMING_AUTH_FAILURE = "connection.incoming.authfail";

    /**
     * Logged when a connection has been terminally closed.
     */
    public static final String CONNECTION_INCOMING_CLOSE = "connection.incoming.close";

    /**
     * Logged when an Uplink session has been successfully initiated. This includes a successful bidirectional handshake with compatibility
     * validation, and the successful assignment of an unused Uplink namespace.
     */
    public static final String UPLINK_SESSION_START = "uplink.session.start";

    /**
     * Logged when an Uplink session failed to be initialized, or was refused by the server. This may be due to a version incompatibility or
     * the desired namespace already being used.
     */
    public static final String UPLINK_SESSION_REFUSE = "uplink.session.refuse";

    /**
     * Logged when a successfully initialized Uplink terminates.
     */
    public static final String UPLINK_SESSION_CLOSE = "uplink.session.close";

    /**
     * Logged when a set of accounts has been first initialized since application start. Whether this event is fired collectively for all
     * account data or per account domain is left to the implementation, and should be reflected in the data field "type". Its value should
     * either be "all", or the name of the domain, e.g. "ssh".
     */
    public static final String ACCOUNTS_INITIALIZED = "accounts.initialized";

    /**
     * Logged when a set of accounts has been updated at application runtime, after being already initialized. Whether this event is fired
     * collectively for all account data or per account domain is left to the implementation, and should be reflected in the data field
     * "type". Its value should either be "all", or the name of the domain, e.g. "ssh".
     */
    public static final String ACCOUNTS_UPDATED = "accounts.updated";

    /**
     * A special value for visually separating application runs.
     */
    public static final String SEPARATOR_LINE_VALUE = "-------------------------------------------------------------------------";

    /**
     * A generic "true" value.
     * 
     * Currently logged as a string value to simplify log parsing. Use this constant instead of a custom String if the field is an actual
     * boolean value, so any future migration to JSON boolean affects all appropriate places.
     */
    public static final String TRUE_VALUE = "true";

    private AuditLogIds() {}
}
