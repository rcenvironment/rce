/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
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
        componentInstallationBuilder.setIsPublished(templateComponentInstallation.getIsPublished());
        componentInstallationBuilder.setMaximumCountOfParallelInstances(templateComponentInstallation.getMaximumCountOfParallelInstances());
        componentInstallationBuilder
            .setNodeId(NodeIdentifierUtils.parseLogicalNodeIdStringWithExceptionWrapping(templateComponentInstallation.getNodeId()));
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
        if (nodeId != null) {
            componentInstallation.setNodeIdFromObject(nodeId);
        } else {
            componentInstallation.setNodeId((String) null);
        }
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
     * @param isPublished <code>true</code> if {@link ComponentInstallation} is published (is made available for remote nodes), otherwise
     *        <code>false</code>
     * @return builder object for method chaining purposes
     */
    public ComponentInstallationBuilder setIsPublished(boolean isPublished) {
        componentInstallation.setIsPublished(isPublished);
        return this;
    }

    /**
     * @return {@link ComponentInstallation} object built
     */
    public ComponentInstallation build() {
        return componentInstallation;
    }
}
