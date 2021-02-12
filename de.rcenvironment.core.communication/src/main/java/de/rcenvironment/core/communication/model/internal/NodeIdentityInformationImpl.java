/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.NodeIdentityInformation;

/**
 * Default immutable {@link NodeIdentityInformation} implementation.
 * 
 * TODO review: this does not seem to be actually used anywhere?
 * 
 * @author Robert Mischke
 */
public class NodeIdentityInformationImpl implements NodeIdentityInformation, Cloneable {

    private static final long serialVersionUID = -7628040480675636721L;

    private final InstanceNodeSessionId instanceSessionId;

    private final String encodedPublicKey;

    private final String displayName;

    private boolean isWorkflowHost;

    public NodeIdentityInformationImpl(InstanceNodeSessionId instanceSessionId, String encodedPublicKey, String displayName,
        boolean isWorkflowHost) {
        this.instanceSessionId = instanceSessionId;
        this.encodedPublicKey = encodedPublicKey;
        this.displayName = displayName;
        this.isWorkflowHost = isWorkflowHost;
    }

    @Override
    public InstanceNodeSessionId getInstanceNodeSessionId() {
        return instanceSessionId;
    }

    @Override
    public String getEncodedPublicKey() {
        return encodedPublicKey;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean getIsWorkflowHost() {
        return isWorkflowHost;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
