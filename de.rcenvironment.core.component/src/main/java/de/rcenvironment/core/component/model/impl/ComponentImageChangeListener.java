/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.model.impl;

import java.util.Collection;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * 
 * @author Dominik Schneider
 */
public abstract class ComponentImageChangeListener implements DistributedComponentKnowledgeListener {

    private String lastIconHash;

    private String componentId;

    /**
     * Inner class allows using different implementations in future.
     * 
     * @author Dominik Schneider
     *
     */
    private static class DefaultImageListener extends ComponentImageChangeListener {

        private final Runnable toExecute;

        DefaultImageListener(String componentId, Runnable toExecute) {
            super(componentId);
            this.toExecute = toExecute;
        }

        @Override
        public void onComponentImageChanged() {
            toExecute.run();
        }
    }

    public ComponentImageChangeListener(String componentId) {
        Collection<DistributedComponentEntry> installations =
            ServiceRegistry.createAccessFor(this).getService(DistributedComponentKnowledgeService.class).getCurrentSnapshot()
                .getAllInstallations();
        installations = ComponentImageUtility.getDistinctInstallations(installations);
        for (DistributedComponentEntry entry : installations) {
            if (entry.getComponentInterface().getIdentifierAndVersion().equals(componentId)) {
                lastIconHash = entry.getComponentInterface().getIconHash();

            }
        }
        this.componentId = componentId;
    }

    private boolean hasImageChanged(DistributedComponentKnowledge newKnowledge) {
        Collection<DistributedComponentEntry> installations =
            ComponentImageUtility.getDistinctInstallations(newKnowledge.getAllInstallations());
        for (DistributedComponentEntry entry : installations) {
            if (entry.getComponentInterface().getIdentifierAndVersion().equals(componentId)
                && !entry.getComponentInterface().getIconHash().equals(lastIconHash)) {
                lastIconHash = entry.getComponentInterface().getIconHash();
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a new ComponentImageChangeListener. The listener triggers only if an image has changed not only if there are changes in the
     * DistributedKnowledge. There is one default implementation at the moment.
     * 
     * @param componentId the componentId which will be used to identify changing image changing events
     * @param function the function which will be executed when the listener is triggered
     * @return Creates a new ComponentImageListener
     */
    protected static ComponentImageChangeListener create(String componentId, Runnable function) {

        return new DefaultImageListener(componentId, function);
    }

    @Override
    public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
        if (hasImageChanged(newState)) {
            onComponentImageChanged();
        }
    }

    /**
     * Will be called when an icon of a component has changed.
     */
    public abstract void onComponentImageChanged();

}
