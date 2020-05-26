/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;

/**
 * Creates {@link ComponentInstallation} objects.
 * 
 * @author Doreen Seider
 */

public class ComponentInstallationBuilder {

    private final ComponentInstallationImpl componentInstallation;

    public ComponentInstallationBuilder() {
        componentInstallation = new ComponentInstallationImpl();
    }

    /**
     * Creates a {@link ComponentInstallationBuilder} instance from an existing {@link ComponentInstallation} instance. If {@link #build()}
     * is called directly afterwards, the {@link ComponentInstallation} instance returned is equal to the {@link ComponentInstallation}
     * instance given regarding installation id, component revision, publishing state, max count of parallel instances, and node id.
     * 
     * @param templateComponentInstallation {@link ComponentInstallation} instance that serves as template
     * @return pre-initialized {@link ComponentInstallationBuilder} instance
     */
    public static ComponentInstallationBuilder fromComponentInstallation(ComponentInstallation templateComponentInstallation) {
        ComponentInstallationBuilder componentInstallationBuilder = new ComponentInstallationBuilder();
        componentInstallationBuilder.setInstallationId(templateComponentInstallation.getInstallationId());
        componentInstallationBuilder.setComponentRevision((ComponentRevisionImpl) templateComponentInstallation.getComponentRevision());
        componentInstallationBuilder.setMaximumCountOfParallelInstances(templateComponentInstallation.getMaximumCountOfParallelInstances());
        // note: id objects are immutable, so it is safe to copy them
        componentInstallationBuilder.setNodeId(templateComponentInstallation.getNodeIdObject());
        return componentInstallationBuilder;
    }

    /**
     * @param installationId identifier of the installation
     * @return builder object for method chaining purposes
     */
    public ComponentInstallationBuilder setInstallationId(String installationId) {
        componentInstallation.setInstallationId(installationId);
        return this;
    }

    /**
     * @param componentRevision related {@link ComponentRevision}
     * @return builder object for method chaining purposes
     */
    public ComponentInstallationBuilder setComponentRevision(ComponentRevision componentRevision) {
        componentInstallation.setComponentRevision((ComponentRevisionImpl) componentRevision);
        return this;
    }

    /**
     * @param nodeId installation node
     * @return builder object for method chaining purposes
     */
    public ComponentInstallationBuilder setNodeId(LogicalNodeId nodeId) {
        // Can be null in case of local node.
        componentInstallation.setNodeIdObject(nodeId);
        return this;
    }

    /**
     * @param maximumCountOfParallelInstances maxcount of instances
     * @return builder object for method chaining purposes
     */
    public ComponentInstallationBuilder setMaximumCountOfParallelInstances(Integer maximumCountOfParallelInstances) {
        componentInstallation.setMaximumCountOfParallelInstances(maximumCountOfParallelInstances);
        return this;
    }
    
    /**
     * @param isMapped if the installation represents a remote component
     * @return builder object for method chaining purposes
     */
    public ComponentInstallationBuilder setIsMappedCompoent(boolean isMapped) {
        componentInstallation.setMappedComponent(isMapped);
        return this;
    }

    /**
     * @return {@link ComponentInstallation} object built
     */
    public ComponentInstallation build() {
        return componentInstallation;
    }
    
}
