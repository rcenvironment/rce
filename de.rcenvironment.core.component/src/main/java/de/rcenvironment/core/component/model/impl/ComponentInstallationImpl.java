/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;

import java.io.Serializable;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A writable {@link ComponentInstallation} implementation.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class ComponentInstallationImpl implements ComponentInstallation, Serializable {

    private static final long serialVersionUID = 3895539478658080757L;

    // TODO review 9.0.0: it would probably be beneficial to use a LogicalNodeSessionId here - misc_ro
    private LogicalNodeId nodeId;

    private ComponentRevisionImpl componentRevision;

    private String installationId; // component interface id and version

    private Integer maximumCountOfParallelInstances = null;

    @Override
    public String getNodeId() {
        if (nodeId == null) {
            return null;
        }
        return nodeId.getLogicalNodeIdString();
    }

    @Override
    @JsonIgnore
    public LogicalNodeId getNodeIdObject() {
        return nodeId;
    }

    /**
     * Sets the location for this component.
     * 
     * @param idString the string form of the id to set
     */
    public void setNodeId(String idString) {
        if (idString != null) {
            this.nodeId = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(idString);
        } else {
            this.nodeId = null;
        }
    }

    /**
     * Sets the location for this component.
     * 
     * @param nodeIdObject the id object to set
     */
    @JsonIgnore
    public void setNodeIdObject(LogicalNodeId nodeIdObject) {
        this.nodeId = nodeIdObject;
    }

    @Override
    public ComponentRevision getComponentRevision() {
        if (componentRevision == null) {
            // TODO fail instead? there should be no actual case where this is expected
            LogFactory.getLog(getClass()).warn("Undefined component revision");
        }
        return componentRevision;
    }

    @Override
    @JsonIgnore // convenience method without a corresponding field, so do not serialize it
    public ComponentInterface getComponentInterface() {
        if (componentRevision == null) {
            // TODO fail instead? there should be no actual case where this is expected
            LogFactory.getLog(getClass()).warn("Undefined component revision; returning null ComponentInterface");
            return null;
        }
        return componentRevision.getComponentInterface();
    }

    public void setComponentRevision(ComponentRevisionImpl componentRevision) {
        this.componentRevision = componentRevision;
    }

    @Override
    public String getInstallationId() {
        if (installationId == null) {
            LogFactory.getLog(getClass()).warn("Undefined installation id");
        }
        return installationId;
    }

    public void setInstallationId(String installationId) {
        this.installationId = installationId;
    }

    @Override
    public Integer getMaximumCountOfParallelInstances() {
        return maximumCountOfParallelInstances;
    }

    public void setMaximumCountOfParallelInstances(Integer maximumCountOfParallelInstances) {
        this.maximumCountOfParallelInstances = maximumCountOfParallelInstances;
    }

    @Override
    public String toString() {
        return StringUtils.format("ComponentInstallation(node=%s,rev=%s)", nodeId, componentRevision);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentInstallationImpl)) {
            return false;
        }
        ComponentInstallationImpl other = (ComponentInstallationImpl) obj;
        return nodeId.equals(other.nodeId) && getInstallationId().equals(other.getInstallationId());
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode() ^ installationId.hashCode();
    }

    @Override
    public int compareTo(ComponentInstallation o) {
        return getComponentInterface().getDisplayName()
            .compareTo(o.getComponentInterface().getDisplayName());
    }

}
