/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

/**
 * Collects events that occur during tool execution and should be transmitted to the initiator. Each event is represented by a (preferably
 * short) type string, and the actual event data as another string.
 * <p>
 * A basic example would be the event "a line of output was printed to stdout/stderr". These could, for example, be represented by
 * individual type strings (e.g. "o"/"e"), with the unmodified output string as the event data.
 *
 * @author Robert Mischke
 */
public interface ToolExecutionProviderEventCollector {

    /**
     * Submits the representation of an event as an opaque string that should be transmitted to the tool execution's initiator.
     * 
     * @param type an event type identifier
     * @param data the event data
     */
    void submitEvent(String type, String data);
}
