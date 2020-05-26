/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper for the list of available {@link ToolDescriptor}s for a given source id. A source id disappearing or disconnecting should be
 * represented by a final update containing an empty list. From a consumer's perspective, this is indistinguishable from a source id
 * removing all of its published tools.
 * 
 * @author Robert Mischke
 */
public class ToolDescriptorListUpdate implements Serializable {

    // TODO review the {@link Serializable} nature before final release
    private static final long serialVersionUID = 6598175381212424344L;

    private List<ToolDescriptor> toolDescriptors;

    private String destinationId;
    
    private String displayName;

    public ToolDescriptorListUpdate() {
        // default constructor for deserialization
    }

    public ToolDescriptorListUpdate(String destinationId, String displayName, List<ToolDescriptor> toolDescriptors) {
        this.destinationId = destinationId;
        this.displayName = displayName;
        this.toolDescriptors = toolDescriptors;
    }

    /**
     * @return the destination id to use when requesting execution of this tool
     */
    public String getDestinationId() {
        return destinationId;
    }

    public List<ToolDescriptor> getToolDescriptors() {
        return toolDescriptors;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
