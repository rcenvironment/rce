/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;

import java.util.ArrayList;
import java.util.List;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import de.rcenvironment.core.component.model.api.ComponentImageContainerService;
import de.rcenvironment.core.component.model.api.ComponentInterface;

/**
 * Default implementation of {@link ComponentImageContainerService}. 
 * 
 * @author Dominik Schneider
 *
 */
@Component(scope = ServiceScope.SINGLETON)
public class ComponentImageContainerServiceImpl implements ComponentImageContainerService {

    private List<ComponentImageContainer> componentImageContainerList;

    public ComponentImageContainerServiceImpl() {
        componentImageContainerList = new ArrayList<>();
    }

    @Override
    public synchronized ComponentImageContainer getComponentImageContainer(String componentId) {
        componentId = ComponentImageUtility.getNormalId(componentId);

        if (componentId != null) {
            // returns an already existing container
            for (ComponentImageContainer entry : componentImageContainerList) {
                if (entry.getComponentId().equals(componentId)) {
                    return entry;
                }
            }
        } else {
            throw new IllegalArgumentException("ComponentId must not be null");
        }
        ComponentImageContainer container = new ComponentImageContainer(componentId);
        componentImageContainerList.add(container);
        return container;
    }

    @Override
    public ComponentImageContainer getComponentImageContainer(ComponentInterface ci) {
        if (ci != null) {
            return getComponentImageContainer(ci.getIdentifierAndVersion());
        } else {
            throw new IllegalArgumentException("Component interface must not be null");
        }
    }

}
