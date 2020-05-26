/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.Serializable;

/**
 * Represents the data that can/must be produced by a {@link ToolExecutionProvider} at the end of an execution.
 *
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class ToolExecutionResult implements Serializable {

    private static final long serialVersionUID = -3126547599934754293L;

    /**
     * Whether the tool execution was successful.
     */
    public boolean successful;

    /**
     * Whether the tool execution was cancelled.
     */
    public boolean cancelled;
}
