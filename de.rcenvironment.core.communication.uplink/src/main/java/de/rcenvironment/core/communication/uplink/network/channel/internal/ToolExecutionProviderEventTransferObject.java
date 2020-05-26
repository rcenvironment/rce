/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.Serializable;

/**
 * A simple holder for serializing type/data pairs.
 *
 * @author Robert Mischke
 */
public class ToolExecutionProviderEventTransferObject implements Serializable {

    private static final long serialVersionUID = 8336834773030824980L;

    /**
     * The type string.
     */
    public final String t;

    /**
     * The data string.
     */
    public final String d;

    /**
     * Deserialization constructor.
     */
    public ToolExecutionProviderEventTransferObject() {
        this.t = null;
        this.d = null;
    }

    public ToolExecutionProviderEventTransferObject(String type, String data) {
        this.t = type;
        this.d = data;
    }

}
