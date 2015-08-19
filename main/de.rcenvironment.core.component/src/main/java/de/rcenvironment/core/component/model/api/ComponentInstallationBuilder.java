/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

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
    public ComponentInstallationBuilder setNodeId(String nodeId) {
        componentInstallation.setNodeId(nodeId);
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
     * @param isPublished <code>true</code> if {@link ComponentInstallation} is published (is made
     *        available for remote nodes), otherwise <code>false</code>
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
