/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;

import java.io.Serializable;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.model.api.ComponentInstallation;
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

    private String nodeId;

    private ComponentRevisionImpl componentRevision;

    private String installationId;
    
    private boolean isPublished = false;
    
    private Integer maximumCountOfParallelInstances = null;

    @Override
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public ComponentRevision getComponentRevision() {
        if (installationId == null) {
            LogFactory.getLog(getClass()).warn("Undefined component revision");
        }
        return componentRevision;
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
    public boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(boolean isPublished) {
        this.isPublished = isPublished;
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
        return getComponentRevision().getComponentInterface().getDisplayName()
            .compareTo(o.getComponentRevision().getComponentInterface().getDisplayName());
    }

}
