/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

/**
 * A handle for sending request to, or accessing the state of a remote tool execution.
 *
 * @author Robert Mischke
 */
public interface ToolExecutionHandle {

    /**
     * Requests cancellation of the related tool execution. This is a best-effort operation; there is no guarantee that the execution will
     * be actually cancelled. Repeated calls, or calls on an already-finished execution are silently ignored. Sending this request before
     * the start of the actual tool execution may or may not prevent it from starting.
     */
    void requestCancel();

    // TODO add a Future or some other mechanism to allow waiting for the end of execution
}
