/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.api;

import java.util.Optional;

/**
 * Provides common/default type id constants for event log entries. Extensions or plugins may provide and use additional ids, but all code
 * that is part of a standard RCE distribution should place their ids here.
 *
 * @author Robert Mischke (template)
 */
public enum EventType {

    // THIS IS A GENERATED FILE - ANY MANUAL CHANGES WILL BE OVERWRITTEN!

    // For modifications, edit 'resources/event-types.yml' and rerun 'scripts/generate-code-and-asciidoc-from-event-types.py'.

    /**
     * The application is starting and has passed basic initialization, including profile selection.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>user_home</b>: The "home" directory of the user running the application.
     * <li><b>user_name</b>: The technical/system name of the user running the application.
     * <li><b>profile_location</b>: The file system location of the profile that the application is using.
     * <li><b>work_dir</b>: The selected "working directory" when starting the application.
     * <li><b>rce_version</b>: The full RCE version string.
     * <li><b>os_name</b>: The name of the underlying operating system, as reported by the JRE.
     * <li><b>jvm_version</b>: The Java version, as reported by the JRE.
     * </ul>
     */
    APPLICATION_STARTING("application.starting", "Application Starting", new String[] { Attributes.USER_HOME, Attributes.USER_NAME,
        Attributes.PROFILE_LOCATION, Attributes.WORK_DIR, Attributes.RCE_VERSION, Attributes.OS_NAME, Attributes.JVM_VERSION
    }, new String[] {
        "User home directory",
        "User system id",
        "Profile location",
        "Working directory",
        "RCE version id",
        "OS name and version",
        "JVM version"
    }),

    /**
     * A user or technical process has requested this instance to shut down, e.g. via GUI, console command, or system shell command. If
     * available, information about the cause should be logged as "Shutdown trigger"/"method".
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>method</b> <i>(optional)</i>: The kind of event that initiated/requested the shutdown. Currently not strictly specified, for
     * informational use only. Current examples are "console command", "CLI/network signal", and null (unspecified)
     * <li><b>is_restart</b>: "yes" if the shutdown request is part of a restart request, i.e., a new application session will start after
     * shutdown.
     * </ul>
     */
    APPLICATION_SHUTDOWN_REQUESTED("application.shutdown.requested", "Application Shutdown Requested", new String[] { Attributes.METHOD,
        Attributes.IS_RESTART
    }, new String[] {
        "Shutdown trigger",
        "Part of a restart"
    }),

    /**
     * A marker event logged at the latest possible time during a regular shutdown of the application.
     */
    APPLICATION_TERMINATING("application.terminating", "Application Terminating", new String[] {
    }, new String[] {
    }),

    /**
     * A network server port (typically TCP) was opened by the application.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The technical type and/or protocol of the server port.
     * <li><b>port</b>: The IP port number.
     * <li><b>bind_ip</b>: The IP address the port is bound to, which affects from which network interfaces it can be accessed.
     * </ul>
     */
    SERVERPORT_OPENED("serverport.opened", "Server Port Opened", new String[] { Attributes.TYPE, Attributes.PORT, Attributes.BIND_IP
    }, new String[] {
        "Port type",
        "IP port number",
        "IP bind address"
    }),

    /**
     * A network server port (typically TCP) was closed by the application.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The technical type and/or protocol of the server port.
     * <li><b>port</b>: The IP port number.
     * <li><b>bind_ip</b>: The IP address the port was bound to.
     * </ul>
     */
    SERVERPORT_CLOSED("serverport.closed", "Server Port Closed", new String[] { Attributes.TYPE, Attributes.PORT, Attributes.BIND_IP
    }, new String[] {
        "Port type",
        "IP port number",
        "IP bind address"
    }),

    /**
     * An incoming connection has been successfully established. This should be logged as late as reasonably possible to avoid "accepted"
     * events where the connection is immediately closed again due to a validation or version mismatch error. Mutually exclusive with the
     * "connection.incoming.refused" event, but may be reached after one or more "connection.incoming.auth.failed" events if the server
     * allows multiple login attempts.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The technical type and/or protocol of the connection. Usually equal to the server port's type.
     * <li><b>connection_id</b>: An association id for this connection. The only assumptions that should be made about its content is that
     * it is a string of "reasonable" length, not empty, suitable for log output, and unique within the application's session.
     * <li><b>remote_node_id</b> <i>(optional)</i>: The id of the remote RCE node, if available from the protocol type's connection process.
     * This id can by any of the four supported id types, but will typically be an "instance session id".
     * <li><b>login_name</b> <i>(optional)</i>: The user/login/account identifier successfully used for authentication/authorization, if
     * applicable.
     * <li><b>auth_method</b> <i>(optional)</i>: The kind of authentication/authorization (e.g. passphrase or private key) that was
     * successfully used, if applicable.
     * <li><b>auth_failure_count</b> <i>(optional)</i>: The number of failed authentication/authorization attempts for this connection, if
     * applicable.
     * <li><b>remote_ip</b>: The remote IP address of the incoming connection.
     * <li><b>remote_port</b>: The remote IP port number of the incoming connection.
     * <li><b>server_port</b>: The local server IP port number that the incoming connection is connected to.
     * </ul>
     */
    CONNECTION_INCOMING_ACCEPTED("connection.incoming.accepted", "Incoming Connection Accepted", new String[] { Attributes.TYPE,
        Attributes.CONNECTION_ID, Attributes.REMOTE_NODE_ID, Attributes.LOGIN_NAME, Attributes.AUTH_METHOD, Attributes.AUTH_FAILURE_COUNT,
        Attributes.REMOTE_IP, Attributes.REMOTE_PORT, Attributes.SERVER_PORT
    }, new String[] {
        "Connection type",
        "Connection id",
        "Remote node id",
        "Login/user name",
        "Authentication method",
        "Failure count",
        "Remote IP address",
        "Remote IP port",
        "Server IP port"
    }),

    /**
     * As part of establishing an incoming connection, an authentication/authorization attempt was made but failed. Whether this failure is
     * fatal for the overall connection attempt or not is implementation-specific. If this is ultimately followed up by a successful
     * authentication/authorization attempt (in case another attempt was allowed in the first place), this event should be succeeded by a
     * "connection.incoming.accepted" event. Otherwise, this should always be succeeded by a "connection.incoming.refused" event.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The technical type and/or protocol of the connection. Usually equal to the server port's type.
     * <li><b>connection_id</b>: An association id for this connection. The only assumptions that should be made about its content is that
     * it is a string of "reasonable" length, not empty, suitable for log output, and unique within the application's session.
     * <li><b>login_name</b>: The user/login/account identifier used for the failed authentication/authorization attempt.
     * <li><b>auth_method</b>: The kind of authentication/authorization (e.g. passphrase or private key) that was attempted.
     * <li><b>auth_failure_reason</b>: The reason why authentication/authorization failed (e.g., wrong password or unknown user).
     * <li><b>auth_failure_count</b>: The number of failed authentication/authorization attempts for this connection.
     * <li><b>remote_ip</b>: The remote IP address of the incoming connection.
     * <li><b>remote_port</b>: The remote IP port number of the incoming connection.
     * <li><b>server_port</b>: The local server IP port number that the incoming connection is connected to.
     * </ul>
     */
    CONNECTION_INCOMING_AUTH_FAILED("connection.incoming.auth.failed", "Incoming Connection Failed To Authenticate", new String[] {
        Attributes.TYPE, Attributes.CONNECTION_ID, Attributes.LOGIN_NAME, Attributes.AUTH_METHOD, Attributes.AUTH_FAILURE_REASON,
        Attributes.AUTH_FAILURE_COUNT, Attributes.REMOTE_IP, Attributes.REMOTE_PORT, Attributes.SERVER_PORT
    }, new String[] {
        "Connection type",
        "Connection id",
        "Login/user name",
        "Authentication method",
        "Failure reason",
        "Failure count",
        "Remote IP address",
        "Remote IP port",
        "Server IP port"
    }),

    /**
     * An incoming connection has failed to complete its login process or has been refused for some other reason. Mutually exclusive with
     * the "connection.incoming.accepted" and "connection.incoming.closed" events. Both incorrect authentication attempts as well as
     * authentication timeouts (e.g. when an SSH client makes no authentication attempt at all) are both represented by this event. These
     * sub-types can be distinguished by the "last_auth_failure_reason" attribute.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The technical type and/or protocol of the connection. Usually equal to the server port's type.
     * <li><b>connection_id</b>: An association id for this connection. The only assumptions that should be made about its content is that
     * it is a string of "reasonable" length, not empty, suitable for log output, and unique within the application's session.
     * <li><b>close_reason</b>: The human-readable reason why this connection was refused.
     * <li><b>last_login_name</b> <i>(optional)</i>: The user/login/account identifier used for the last failed authentication/authorization
     * attempt, if applicable.
     * <li><b>last_auth_failure_reason</b> <i>(optional)</i>: The last reason why authentication/authorization failed (e.g., wrong password
     * or unknown user), if applicable.
     * <li><b>last_auth_method</b> <i>(optional)</i>: The last kind of authentication/authorization (e.g. passphrase or private key) that
     * was attempted, if applicable.
     * <li><b>auth_failure_count</b> <i>(optional)</i>: The number of failed authentication/authorization attempts for this connection, if
     * applicable.
     * <li><b>remote_ip</b>: The remote IP address of the incoming connection.
     * <li><b>remote_port</b>: The remote IP port number of the incoming connection.
     * <li><b>server_port</b>: The local server IP port number that the incoming connection is connected to.
     * <li><b>duration</b>: The duration (in msec) that this connection was open/established for. The precise start and end times for this
     * calculation are implementation- and type-dependent.
     * </ul>
     */
    CONNECTION_INCOMING_REFUSED("connection.incoming.refused", "Incoming Connection Refused", new String[] { Attributes.TYPE,
        Attributes.CONNECTION_ID, Attributes.CLOSE_REASON, Attributes.LAST_LOGIN_NAME, Attributes.LAST_AUTH_FAILURE_REASON,
        Attributes.LAST_AUTH_METHOD, Attributes.AUTH_FAILURE_COUNT, Attributes.REMOTE_IP, Attributes.REMOTE_PORT, Attributes.SERVER_PORT,
        Attributes.DURATION
    }, new String[] {
        "Connection type",
        "Connection id",
        "Reason",
        "Last login/user name",
        "Last auth. failure",
        "Last auth. method",
        "Auth. failure count",
        "Remote IP address",
        "Remote IP port",
        "Server IP port",
        "Duration"
    }),

    /**
     * An incoming connection has been closed, either by the client, the server, or a network event. This event is always preceded by a
     * "connection.incoming.accepted" event. Mutually exclusive with the "connection.incoming.refused" event.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The technical type and/or protocol of the connection. Usually equal to the server port's type.
     * <li><b>connection_id</b>: An association id for this connection. The only assumptions that should be made about its content is that
     * it is a string of "reasonable" length, not empty, suitable for log output, and unique within the application's session.
     * <li><b>remote_node_id</b> <i>(optional)</i>: The id of the remote RCE node, if available from the protocol type's connection process.
     * This id can by any of the four supported id types, but will typically be an "instance session id".
     * <li><b>close_reason</b>: The human-readable reason why this connection was closed.
     * <li><b>remote_ip</b>: The remote IP address of the incoming connection.
     * <li><b>remote_port</b>: The remote IP port number of the incoming connection.
     * <li><b>server_port</b>: The local server IP port number that the incoming connection is connected to.
     * <li><b>duration</b>: The duration (in msec) that this connection was open/established for. The precise start and end times for this
     * calculation are implementation- and type-dependent.
     * </ul>
     */
    CONNECTION_INCOMING_CLOSED("connection.incoming.closed", "Incoming Connection Closed", new String[] { Attributes.TYPE,
        Attributes.CONNECTION_ID, Attributes.REMOTE_NODE_ID, Attributes.CLOSE_REASON, Attributes.REMOTE_IP, Attributes.REMOTE_PORT,
        Attributes.SERVER_PORT, Attributes.DURATION
    }, new String[] {
        "Connection type",
        "Connection id",
        "Remote node id",
        "Reason",
        "Remote IP address",
        "Remote IP port",
        "Server IP port",
        "Duration"
    }),

    /**
     * After the login credentials for an incoming SSH connection were accepted, this subsequent event indicates successful completion of
     * the Uplink protocol handshake, too. This includes protocol compatibility validation and the successful assignment of an unused Uplink
     * namespace.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>session_id</b>: The Uplink-specific session id.
     * <li><b>connection_id</b>: The SSH connection id for correlation with "connection.incoming.*" events.
     * <li><b>client_version</b>: The software version of the Uplink client; includes information about the client software used (e.g.,
     * "rce/...").
     * <li><b>protocol_version</b>: The Uplink protocol version being used for the connection/session, based on the initial client-server
     * handshake.
     * <li><b>login_name</b>: The final/effective login name used. Due to namespace mapping constraints, this may be different from the
     * original login name; see "original_login_name".
     * <li><b>original_login_name</b> <i>(optional)</i>: The login name requested by the client, before any modifications by the server.
     * Omitted if equal to the effective login name (see "login_name").
     * <li><b>client_id</b>: The final/effective client id used. Due to namespace mapping constraints, this may be different from the
     * original client id; see "original_client_id".
     * <li><b>original_client_id</b> <i>(optional)</i>: The client id requested by the client, before any modifications by the server.
     * Omitted if equal to the effective client id (see "client_id").
     * <li><b>namespace</b>: The namespace assigned to this client for Uplink destination address mapping. Typically related to the session
     * id.
     * </ul>
     */
    UPLINK_INCOMING_ACCEPTED("uplink.incoming.accepted", "Incoming Uplink Connection Accepted", new String[] { Attributes.SESSION_ID,
        Attributes.CONNECTION_ID, Attributes.CLIENT_VERSION, Attributes.PROTOCOL_VERSION, Attributes.LOGIN_NAME,
        Attributes.ORIGINAL_LOGIN_NAME, Attributes.CLIENT_ID, Attributes.ORIGINAL_CLIENT_ID, Attributes.NAMESPACE
    }, new String[] {
        "Uplink session id",
        "SSH connection id",
        "Client software id",
        "Protocol version",
        "Effective login name",
        "Original login name",
        "Effective client id",
        "Original client id",
        "Assigned namespace"
    }),

    /**
     * After the login credentials for an incoming SSH connection were accepted, this subsequent event indicates failure of the Uplink
     * protocol handshake. This may be due to a version incompatibility or the desired namespace already being used. The next event logged
     * after this should be "connection.incoming.closed", as the SSH connection was already "accepted". Mutually exclusive with
     * "uplink.incoming.accepted" and "uplink.incoming.closed".
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>session_id</b>: The Uplink-specific session id.
     * <li><b>connection_id</b>: The SSH connection id for correlation with "connection.incoming.*" events.
     * <li><b>client_version</b>: The software version of the Uplink client; includes information about the client software used (e.g.,
     * "rce/...").
     * <li><b>protocol_version</b>: The protocol version requested by the client. May or may not be a version that this server supports.
     * <li><b>login_name</b>: The login name used by the client, without any mapping modification that would have been made on success.
     * <li><b>client_id</b>: The client id sent by the client, without any mapping modification that would have been made on success.
     * <li><b>reason</b>: The human-readable reason for refusing the Uplink session.
     * </ul>
     */
    UPLINK_INCOMING_REFUSED("uplink.incoming.refused", "Incoming Uplink Connection Refused", new String[] { Attributes.SESSION_ID,
        Attributes.CONNECTION_ID, Attributes.CLIENT_VERSION, Attributes.PROTOCOL_VERSION, Attributes.LOGIN_NAME, Attributes.CLIENT_ID,
        Attributes.REASON
    }, new String[] {
        "Uplink session id",
        "SSH connection id",
        "Client software id",
        "Protocol version",
        "Original login name",
        "Original client id",
        "Reason"
    }),

    /**
     * After a previous "uplink.incoming.accepted" event, this indicates the end of the application-level Uplink session. This event should
     * be logged for any kind of connection termination, from graceful disconnect to low-level connection errors. Mutually exclusive with
     * "uplink.incoming.refused".
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>session_id</b>: The Uplink-specific session id.
     * <li><b>connection_id</b>: The SSH connection id for correlation with "connection.incoming.*" events.
     * <li><b>final_state</b>: The final (technical) state of the Uplink connection; indicates the reason for terminating the session.
     * </ul>
     */
    UPLINK_INCOMING_CLOSED("uplink.incoming.closed", "Incoming Uplink Connection Closed", new String[] { Attributes.SESSION_ID,
        Attributes.CONNECTION_ID, Attributes.FINAL_STATE
    }, new String[] {
        "Uplink session id",
        "SSH connection id",
        "Final connection state"
    }),

    /**
     * Either a new node in the logical RCE network has been discovered, or an existing node has changed its title. Relevant for determining
     * which technical node was behind a given display name at a certain time. Besides general logging, this is also a basic security trail
     * for potential node "impersonation" attempts.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>instance_id</b>: The persistent part (the "instance node id") of the logical RCE-specific network address.
     * <li><b>session_id</b>: The per-session suffix of the of the logical RCE-specific network address. Used to distinguish runs of the
     * same instance, i.e., the session id always changes on an instance's restart.
     * <li><b>logical_sub_node_id</b> <i>(optional)</i>: The logical "sub-node" selector within the logical RCE-specific network address. If
     * absent, then this event represents a change of the "root" name of an instance/node. If present, then a logical "sub-node" has changed
     * its specific name, while the "root" name was unchanged.
     * <li><b>name</b>: The title/name of the RCE instance, as defined by its user or administrator.
     * <li><b>is_local_node</b> <i>(optional)</i> <i>(derived)</i>: \"yes\" if the observed node is the local node. This is a convenience
     * property to simplify event filtering.
     * </ul>
     */
    NETWORK_NODE_NAMED("network.node.named", "Network Node Discovered/Named", new String[] { Attributes.INSTANCE_ID,
        Attributes.SESSION_ID, Attributes.LOGICAL_SUB_NODE_ID, Attributes.NAME, Attributes.IS_LOCAL_NODE
    }, new String[] {
        "Persistent instance id",
        "Instance session id",
        "Logical sub-node id",
        "Announced name",
        "Is local/own node"
    }),

    /**
     * A workflow run was initiated from the local node. The designated workflow controller may be either the local or a remote node.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>workflow_controller_node</b>: The logical node id of the node designated to run the workflow controller.
     * <li><b>workflow_controller_is_local_node</b> <i>(derived)</i>: A convenience attribute designating whether the workflow controller
     * node is the local node.
     * <li><b>local_workflow_file</b> <i>(optional)</i>: The path of the local workflow file that was submitted for execution, if
     * applicable; OTHERWISE absent.
     * <li><b>workflow_metadata</b>: TODO specify contents; structured data
     * <li><b>success</b>: Whether the workflow was successfully initiated, i.e., whether the request was made and accepted by the
     * designated execution node.
     * </ul>
     */
    WORKFLOW_REQUEST_INITIATED("workflow.request.initiated", "Workflow Request Initiated (Request Sent)", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.WORKFLOW_CONTROLLER_NODE, Attributes.WORKFLOW_CONTROLLER_IS_LOCAL_NODE,
        Attributes.LOCAL_WORKFLOW_FILE, Attributes.WORKFLOW_METADATA, Attributes.SUCCESS
    }, new String[] {
        "Workflow run id",
        "Workflow controller node",
        "Controller is local",
        "Workflow file path",
        "Workflow metadata (WIP)",
        "success"
    }),

    /**
     * A workflow run was requested, and an attempt was made to initialize its workflow controller on the local node. If controller
     * initialization fails, this event MUST still be logged to make the request visible, as the ".initiated" event may have been logged be
     * on a remote node.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>start_ts</b> <i>(optional)</i>: On success, the canonical start time of the workflow, which will usually be close, but not
     * necessarily equal to the timestamp of this event; OTHERWISE absent. (TODO specify format)
     * <li><b>initiator_node</b>: The logical session node id of the node that initiated this workflow run. Note that in RCE 10.x and
     * earlier, this value is NOT strongly verified, and should be considered informational only.
     * <li><b>initiator_is_local_node</b> <i>(derived)</i>: A convenience attribute designating whether the node that initiated this
     * workflow run is the local node. Note that in RCE 10.x and earlier, this value is NOT strongly verified, and should be considered
     * informational only.
     * <li><b>workflow_metadata</b>: TODO specify contents; structured data
     * <li><b>success</b>: Whether the workflow was successfully initialized.
     * </ul>
     */
    WORKFLOW_EXECUTION_REQUESTED("workflow.execution.requested", "Workflow Execution Requested (Request Received)", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.START_TS, Attributes.INITIATOR_NODE, Attributes.INITIATOR_IS_LOCAL_NODE,
        Attributes.WORKFLOW_METADATA, Attributes.SUCCESS
    }, new String[] {
        "Workflow run id",
        "Start timestamp",
        "Initiator node",
        "Initiator is local",
        "Workflow metadata (WIP)",
        "success"
    }),

    /**
     * The end of a local workflow controller's execution. (TODO clarify whether this may be logged on a failed ".requested" event or not.).
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>end_ts</b>: The canonical end time of the workflow, which will usually be close, but not necessarily equal to the timestamp of
     * this event. (TODO specify format)
     * <li><b>duration</b> <i>(derived)</i>: A convenience attribute specifying the duration of this workflow run. (TODO specify format)
     * <li><b>final_state</b>: The final state of the workflow, as defined by the workflow engine.
     * </ul>
     */
    WORKFLOW_EXECUTION_COMPLETED("workflow.execution.completed", "Workflow Execution Completed", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.END_TS, Attributes.DURATION, Attributes.FINAL_STATE
    }, new String[] {
        "Workflow run id",
        "End timestamp",
        "Duration",
        "Final workflow state"
    }),

    /**
     * The final event of a workflow run that was initiated by the local node. (TODO clarify whether this may be logged on a failed
     * ".initiate" event or not.).
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>final_state</b>: The final state of the workflow, as defined by the workflow engine.
     * </ul>
     */
    WORKFLOW_REQUEST_COMPLETED("workflow.request.completed", "Workflow Request Completed", new String[] { Attributes.WORKFLOW_RUN_ID,
        Attributes.FINAL_STATE
    }, new String[] {
        "Workflow run id",
        "Final workflow state"
    }),

    /**
     * A workflow component run was initiated from the local node, which is always the workflow controller. The node controlling the
     * component's execution may be the local or a remote one.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>component_run_id</b>: The id string assigned to this component run. Unique within the scope of the associated workflow.
     * <li><b>execution_controller_node</b>: The logical session node id of the node designated to be this component run's execution
     * controller.
     * <li><b>execution_controller_is_local_node</b> <i>(derived)</i>: A convenience attribute designating whether this component's run
     * execution controller node is the local node.
     * </ul>
     */
    COMPONENT_REQUEST_INITIATED("component.request.initiated", "Component/Tool Execution Initiated (Request Sent)", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.COMPONENT_RUN_ID, Attributes.EXECUTION_CONTROLLER_NODE,
        Attributes.EXECUTION_CONTROLLER_IS_LOCAL_NODE
    }, new String[] {
        "Workflow run id",
        "Component run id",
        "Executing node",
        "Executing node is local"
    }),

    /**
     * A workflow component run was requested, and an attempt was made to initialize its controller on the local node. If controller
     * initialization fails, this event MUST still be logged to make the request visible, as the ".initiated" event may have been logged be
     * on a remote workflow controller node.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>component_run_id</b>: The id string assigned to this component run. Unique within the scope of the associated workflow.
     * <li><b>workflow_controller_node</b>: The logical session node id of the node running the workflow controller. Note that in RCE 10.x
     * and earlier, this value is NOT strongly verified, and should be considered informational only.
     * <li><b>workflow_controller_is_local_node</b>: A convenience attribute designating whether the workflow controller node is the local
     * node. Note that in RCE 10.x and earlier, this value is NOT strongly verified, and should be considered informational only.
     * <li><b>start_ts</b> <i>(optional)</i>: On success, the canonical start time of the component run, which will usually be close, but
     * not necessarily equal to the timestamp of this event; OTHERWISE absent. (TODO specify format)
     * </ul>
     */
    COMPONENT_EXECUTION_REQUESTED("component.execution.requested", "Component/Tool Execution Requested", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.COMPONENT_RUN_ID, Attributes.WORKFLOW_CONTROLLER_NODE,
        Attributes.WORKFLOW_CONTROLLER_IS_LOCAL_NODE, Attributes.START_TS
    }, new String[] {
        "Workflow run id",
        "Component run id",
        "Workflow controller node",
        "Workflow contr. is local",
        "Start timestamp"
    }),

    /**
     * The end of a local workflow controller's execution. (TODO clarify whether this may be logged on a failed ".requested" event or not.).
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>component_run_id</b>: The id string assigned to this component run. Unique within the scope of the associated workflow.
     * <li><b>end_ts</b>: The canonical end time of the component run, which will usually be close, but not necessarily equal to the
     * timestamp of this event. (TODO specify format)
     * <li><b>duration</b> <i>(derived)</i>: A convenience attribute specifying the wall-clock duration of this component run. (TODO specify
     * format)
     * <li><b>final_state</b>: The final state of the component, as defined by the workflow engine.
     * </ul>
     */
    COMPONENT_EXECUTION_COMPLETED("component.execution.completed", "Component/Tool Execution Completed", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.COMPONENT_RUN_ID, Attributes.END_TS, Attributes.DURATION, Attributes.FINAL_STATE
    }, new String[] {
        "Workflow run id",
        "Component run id",
        "End timestamp",
        "Duration",
        "Final component state"
    }),

    /**
     * The final event of a component run that was initiated by a workflow controller running on the local node. (TODO clarify whether this
     * may be logged on a failed ".initiate" event or not.).
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>workflow_run_id</b>: The id string assigned to this workflow run. Can be considered globally unique for all practical
     * purposes.
     * <li><b>component_run_id</b>: The id string assigned to this component run. Unique within the scope of the associated workflow.
     * <li><b>end_ts</b>: The canonical end time of the component run, which will usually be close, but not necessarily equal to the
     * timestamp of this event. (TODO specify format)
     * <li><b>duration</b> <i>(derived)</i>: A convenience attribute specifying the wall-clock duration of this component run. (TODO specify
     * format)
     * <li><b>final_state</b>: The final state of the component, as defined by the workflow engine.
     * </ul>
     */
    COMPONENT_REQUEST_COMPLETED("component.request.completed", "Component/Tool Execution Request Completed", new String[] {
        Attributes.WORKFLOW_RUN_ID, Attributes.COMPONENT_RUN_ID, Attributes.END_TS, Attributes.DURATION, Attributes.FINAL_STATE
    }, new String[] {
        "Workflow run id",
        "Component run id",
        "End timestamp",
        "Duration",
        "Final component state"
    }),

    /**
     * A set of accounts has been initialized, typically at application or subsystem startup.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The type of accounts that were initialized, e.g. "ssh".
     * <li><b>number_of_accounts</b>: Indicates the total number of registered accounts after initialization.
     * <li><b>origin</b> <i>(optional)</i>: Indicates the data source from which the initial account data was read. Absent if not applicable
     * for the current account type.
     * </ul>
     */
    ACCOUNTS_INITIALIZED("accounts.initialized", "Login/Account Data Initialized", new String[] { Attributes.TYPE,
        Attributes.NUMBER_OF_ACCOUNTS, Attributes.ORIGIN
    }, new String[] {
        "Account type",
        "New number of accounts",
        "Account data origin"
    }),

    /**
     * A set of accounts has been updated at application runtime, after being already initialized.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: The type of accounts that were updated, e.g. "ssh".
     * <li><b>number_of_accounts</b>: Indicates the total number of registered accounts after the update (NOT the number of changes!).
     * <li><b>origin</b> <i>(optional)</i>: Indicates the data source from which the updated account data was read. Absent if not applicable
     * for the current account type.
     * </ul>
     */
    ACCOUNTS_UPDATED("accounts.updated", "Login/Account Data Updated", new String[] { Attributes.TYPE, Attributes.NUMBER_OF_ACCOUNTS,
        Attributes.ORIGIN
    }, new String[] {
        "Account type",
        "New number of accounts",
        "Account data origin"
    }),

    /**
     * Indicates that the system monitoring subsystem was initialized and logs static system information.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>jvm_pid</b>: The PID of the main process running the JVM. Depending on the operating system, this process may either be the
     * RCE executable or a separate Java process.
     * <li><b>jvm_processor_count</b>: The number of processors, as reported by the JVM.
     * <li><b>jvm_heap_limit</b>: The configured heap (Java RAM) limit, as reported by the JVM.
     * <li><b>system_total_ram</b>: The total system RAM in bytes, as reported by the system monitoring library.
     * <li><b>system_logical_cpus</b>: The number of "logical CPUs", as reported by the system monitoring library.
     * </ul>
     */
    SYSMON_INITIALIZED("sysmon.initialized", "System Monitoring Initialized", new String[] { Attributes.JVM_PID,
        Attributes.JVM_PROCESSOR_COUNT, Attributes.JVM_HEAP_LIMIT, Attributes.SYSTEM_TOTAL_RAM, Attributes.SYSTEM_LOGICAL_CPUS
    }, new String[] {
        "Java process id",
        "Processor count (JVM)",
        "Heap limit (JVM)",
        "System RAM (native)",
        "Logical CPUs (native)"
    }),

    /**
     * A custom event type, allowing extensions or plugins to make use of the event logging system. This event type is special in the way
     * that its list of attributes is not pre-defined, except for "type". This allows custom events to log all kinds of extra attributes.
     * Consequently, any event log validation code must have a special rule to accept those attributes. Note that while custom attribute
     * keys are supported, there are still certain rules for them. For now, they must start with a-z, end with a-z or a digit, and not
     * exceed the maximum length defined above. It is also recommended to keep them lowercase and dot-separated for consistency.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li><b>type</b>: A custom type id describing the logged event. It should generally follow the naming pattern of the standard events,
     * and plugins/extensions should strive to make those ids collision-free.
     * </ul>
     */
    CUSTOM("custom", "Custom Event", new String[] { Attributes.TYPE
    }, new String[] {
        "Custom event type id"
    });

    /**
     * The attribute keys used by the event types above. Except for the special event type {@link #CUSTOM}, which may use custom attribute
     * keys in addition to the required "type" attribute, this list comprises all valid key values for attributes.
     */
    public static class Attributes {

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String USER_HOME = "user_home";

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String USER_NAME = "user_name";

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String PROFILE_LOCATION = "profile_location";

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String WORK_DIR = "work_dir";

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String RCE_VERSION = "rce_version";

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String OS_NAME = "os_name";

        /**
         * Used by APPLICATION_STARTING.
         */
        public static final String JVM_VERSION = "jvm_version";

        /**
         * Used by APPLICATION_SHUTDOWN_REQUESTED.
         */
        public static final String METHOD = "method";

        /**
         * Used by APPLICATION_SHUTDOWN_REQUESTED.
         */
        public static final String IS_RESTART = "is_restart";

        /**
         * Used by ACCOUNTS_INITIALIZED, ACCOUNTS_UPDATED, CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED,
         * CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED, CUSTOM, SERVERPORT_CLOSED, SERVERPORT_OPENED.
         */
        public static final String TYPE = "type";

        /**
         * Used by SERVERPORT_CLOSED, SERVERPORT_OPENED.
         */
        public static final String PORT = "port";

        /**
         * Used by SERVERPORT_CLOSED, SERVERPORT_OPENED.
         */
        public static final String BIND_IP = "bind_ip";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED, CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED,
         * UPLINK_INCOMING_ACCEPTED, UPLINK_INCOMING_CLOSED, UPLINK_INCOMING_REFUSED.
         */
        public static final String CONNECTION_ID = "connection_id";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_CLOSED.
         */
        public static final String REMOTE_NODE_ID = "remote_node_id";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED, UPLINK_INCOMING_ACCEPTED, UPLINK_INCOMING_REFUSED.
         */
        public static final String LOGIN_NAME = "login_name";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED.
         */
        public static final String AUTH_METHOD = "auth_method";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED, CONNECTION_INCOMING_REFUSED.
         */
        public static final String AUTH_FAILURE_COUNT = "auth_failure_count";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED, CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED.
         */
        public static final String REMOTE_IP = "remote_ip";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED, CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED.
         */
        public static final String REMOTE_PORT = "remote_port";

        /**
         * Used by CONNECTION_INCOMING_ACCEPTED, CONNECTION_INCOMING_AUTH_FAILED, CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED.
         */
        public static final String SERVER_PORT = "server_port";

        /**
         * Used by CONNECTION_INCOMING_AUTH_FAILED.
         */
        public static final String AUTH_FAILURE_REASON = "auth_failure_reason";

        /**
         * Used by CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED.
         */
        public static final String CLOSE_REASON = "close_reason";

        /**
         * Used by CONNECTION_INCOMING_REFUSED.
         */
        public static final String LAST_LOGIN_NAME = "last_login_name";

        /**
         * Used by CONNECTION_INCOMING_REFUSED.
         */
        public static final String LAST_AUTH_FAILURE_REASON = "last_auth_failure_reason";

        /**
         * Used by CONNECTION_INCOMING_REFUSED.
         */
        public static final String LAST_AUTH_METHOD = "last_auth_method";

        /**
         * Used by COMPONENT_EXECUTION_COMPLETED, COMPONENT_REQUEST_COMPLETED, CONNECTION_INCOMING_CLOSED, CONNECTION_INCOMING_REFUSED,
         * WORKFLOW_EXECUTION_COMPLETED.
         */
        public static final String DURATION = "duration";

        /**
         * Used by NETWORK_NODE_NAMED, UPLINK_INCOMING_ACCEPTED, UPLINK_INCOMING_CLOSED, UPLINK_INCOMING_REFUSED.
         */
        public static final String SESSION_ID = "session_id";

        /**
         * Used by UPLINK_INCOMING_ACCEPTED, UPLINK_INCOMING_REFUSED.
         */
        public static final String CLIENT_VERSION = "client_version";

        /**
         * Used by UPLINK_INCOMING_ACCEPTED, UPLINK_INCOMING_REFUSED.
         */
        public static final String PROTOCOL_VERSION = "protocol_version";

        /**
         * Used by UPLINK_INCOMING_ACCEPTED.
         */
        public static final String ORIGINAL_LOGIN_NAME = "original_login_name";

        /**
         * Used by UPLINK_INCOMING_ACCEPTED, UPLINK_INCOMING_REFUSED.
         */
        public static final String CLIENT_ID = "client_id";

        /**
         * Used by UPLINK_INCOMING_ACCEPTED.
         */
        public static final String ORIGINAL_CLIENT_ID = "original_client_id";

        /**
         * Used by UPLINK_INCOMING_ACCEPTED.
         */
        public static final String NAMESPACE = "namespace";

        /**
         * Used by UPLINK_INCOMING_REFUSED.
         */
        public static final String REASON = "reason";

        /**
         * Used by COMPONENT_EXECUTION_COMPLETED, COMPONENT_REQUEST_COMPLETED, UPLINK_INCOMING_CLOSED, WORKFLOW_EXECUTION_COMPLETED,
         * WORKFLOW_REQUEST_COMPLETED.
         */
        public static final String FINAL_STATE = "final_state";

        /**
         * Used by NETWORK_NODE_NAMED.
         */
        public static final String INSTANCE_ID = "instance_id";

        /**
         * Used by NETWORK_NODE_NAMED.
         */
        public static final String LOGICAL_SUB_NODE_ID = "logical_sub_node_id";

        /**
         * Used by NETWORK_NODE_NAMED.
         */
        public static final String NAME = "name";

        /**
         * Used by NETWORK_NODE_NAMED.
         */
        public static final String IS_LOCAL_NODE = "is_local_node";

        /**
         * Used by COMPONENT_EXECUTION_COMPLETED, COMPONENT_EXECUTION_REQUESTED, COMPONENT_REQUEST_COMPLETED, COMPONENT_REQUEST_INITIATED,
         * WORKFLOW_EXECUTION_COMPLETED, WORKFLOW_EXECUTION_REQUESTED, WORKFLOW_REQUEST_COMPLETED, WORKFLOW_REQUEST_INITIATED.
         */
        public static final String WORKFLOW_RUN_ID = "workflow_run_id";

        /**
         * Used by COMPONENT_EXECUTION_REQUESTED, WORKFLOW_REQUEST_INITIATED.
         */
        public static final String WORKFLOW_CONTROLLER_NODE = "workflow_controller_node";

        /**
         * Used by COMPONENT_EXECUTION_REQUESTED, WORKFLOW_REQUEST_INITIATED.
         */
        public static final String WORKFLOW_CONTROLLER_IS_LOCAL_NODE = "workflow_controller_is_local_node";

        /**
         * Used by WORKFLOW_REQUEST_INITIATED.
         */
        public static final String LOCAL_WORKFLOW_FILE = "local_workflow_file";

        /**
         * Used by WORKFLOW_EXECUTION_REQUESTED, WORKFLOW_REQUEST_INITIATED.
         */
        public static final String WORKFLOW_METADATA = "workflow_metadata";

        /**
         * Used by WORKFLOW_EXECUTION_REQUESTED, WORKFLOW_REQUEST_INITIATED.
         */
        public static final String SUCCESS = "success";

        /**
         * Used by COMPONENT_EXECUTION_REQUESTED, WORKFLOW_EXECUTION_REQUESTED.
         */
        public static final String START_TS = "start_ts";

        /**
         * Used by WORKFLOW_EXECUTION_REQUESTED.
         */
        public static final String INITIATOR_NODE = "initiator_node";

        /**
         * Used by WORKFLOW_EXECUTION_REQUESTED.
         */
        public static final String INITIATOR_IS_LOCAL_NODE = "initiator_is_local_node";

        /**
         * Used by COMPONENT_EXECUTION_COMPLETED, COMPONENT_REQUEST_COMPLETED, WORKFLOW_EXECUTION_COMPLETED.
         */
        public static final String END_TS = "end_ts";

        /**
         * Used by COMPONENT_EXECUTION_COMPLETED, COMPONENT_EXECUTION_REQUESTED, COMPONENT_REQUEST_COMPLETED, COMPONENT_REQUEST_INITIATED.
         */
        public static final String COMPONENT_RUN_ID = "component_run_id";

        /**
         * Used by COMPONENT_REQUEST_INITIATED.
         */
        public static final String EXECUTION_CONTROLLER_NODE = "execution_controller_node";

        /**
         * Used by COMPONENT_REQUEST_INITIATED.
         */
        public static final String EXECUTION_CONTROLLER_IS_LOCAL_NODE = "execution_controller_is_local_node";

        /**
         * Used by ACCOUNTS_INITIALIZED, ACCOUNTS_UPDATED.
         */
        public static final String NUMBER_OF_ACCOUNTS = "number_of_accounts";

        /**
         * Used by ACCOUNTS_INITIALIZED, ACCOUNTS_UPDATED.
         */
        public static final String ORIGIN = "origin";

        /**
         * Used by SYSMON_INITIALIZED.
         */
        public static final String JVM_PID = "jvm_pid";

        /**
         * Used by SYSMON_INITIALIZED.
         */
        public static final String JVM_PROCESSOR_COUNT = "jvm_processor_count";

        /**
         * Used by SYSMON_INITIALIZED.
         */
        public static final String JVM_HEAP_LIMIT = "jvm_heap_limit";

        /**
         * Used by SYSMON_INITIALIZED.
         */
        public static final String SYSTEM_TOTAL_RAM = "system_total_ram";

        /**
         * Used by SYSMON_INITIALIZED.
         */
        public static final String SYSTEM_LOGICAL_CPUS = "system_logical_cpus";

    }

    private final String id;

    private final String title;

    private String[] attributeKeys;

    private String[] attributeTitles;

    EventType(String typeId, String title, String[] attributeKeys, String[] attributeTitles) {
        this.id = typeId;
        this.title = title;
        this.attributeKeys = attributeKeys;
        this.attributeTitles = attributeTitles;
        if (attributeKeys.length != attributeTitles.length) {
            throw new IllegalArgumentException();
        }
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String[] getAttributeKeys() {
        // note: the returned array is mutable; clients modifying it is considered a coding error
        return attributeKeys;
    }

    public Optional<String> getAttributeTitle(String key) {
        for (int i = 0; i < attributeKeys.length; i++) {
            if (key.equals(attributeKeys[i])) {
                return Optional.of(attributeTitles[i]);
            }
        }
        return Optional.empty();
    }

}
