/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.model.api.ComponentImageManagerService;
import de.rcenvironment.core.component.model.impl.ComponentImageManagerImpl.ImagePackage;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * This "container" class is holding the icons of one component. It contains mechanisms to automatic update the container with the newest
 * available component icons. The icons can be received by manual requesting them and by a callback which notifies the caller when a new
 * icon is available.
 * 
 * @author Dominik Schneider
 *
 */
public class ComponentImageContainer {

    private String componentId;

    private ComponentImageManagerService componentImageManager;

    private ImagePackage imagePackage;

    private List<Runnable> toExecuteList;

    private ComponentImageChangeListener changeListener;

    public ComponentImageContainer(String componentId) {
        this.componentId = componentId;
        bindComponentImageService();
        receiveNewestImagePackage();
        this.toExecuteList = new ArrayList<>();

    }

    /**
     * Creates a listener for changes of the component image and executes all callbacks for dynamic updates.
     */
    private void createComponentImageChangeListener() {
        this.changeListener = ComponentImageChangeListener.create(componentId, () -> {
            for (Runnable runnable : toExecuteList) {
                ConcurrencyUtils.getAsyncTaskService().execute("Component icon updates", runnable);
            }

        });
        ServiceRegistry.createPublisherAccessFor(this).registerService(DistributedComponentKnowledgeListener.class, this.changeListener);
    }

    /**
     * This methods bind the ComponentImageManager. This is NOT an OSGI-method
     */
    private void bindComponentImageService() {
        this.componentImageManager = ServiceRegistry.createAccessFor(this).getService(ComponentImageManagerService.class);
    }

    /**
     * Returns the component icon in 16x16. The returned icon is shared, the caller MUST NOT dispose it!.
     * 
     * @return ComponentIcon
     */
    public Image getComponentIcon16() {
        receiveNewestImagePackage();
        return imagePackage.getIcon16(componentId);
    }

    /**
     * Returns the component icon in 24x24. The returned icon is shared, the caller MUST NOT dispose it!.
     * 
     * @return ComponentIcon
     */
    public Image getComponentIcon24() {
        receiveNewestImagePackage();
        return imagePackage.getIcon24(componentId);
    }

    /**
     * Returns the component icon in 32x32. The returned icon is shared, the caller MUST NOT dispose it!.
     * 
     * @return ComponentIcon
     */
    public Image getComponentIcon32() {
        receiveNewestImagePackage();
        return imagePackage.getIcon32(componentId);
    }

    private void receiveNewestImagePackage() {
        this.imagePackage = componentImageManager.getImagePackage(componentId);

    }

    /**
     * Registers a runnable which will be executed on ComponentImageChangedListener. Note: If the container is received by an icon hash,
     * there will be no dynamic updates of the images. In this case, do NOT call this method.
     * 
     * @param run Runnable which will be executed
     */
    public synchronized void addComponentImageChangeListener(Runnable run) {
        this.toExecuteList.add(run);
        if (toExecuteList.size() == 1) {
            createComponentImageChangeListener();
        }
    }

    /**
     * Removes a runnable which has been used to register for dynamic updates.
     * 
     * @param run The runnable which has been used to register for the listener.
     */
    public synchronized void removeComponentImageChangeLIstener(Runnable run) {
        if (toExecuteList.contains(run)) {
            toExecuteList.remove(run);
            if (toExecuteList.isEmpty()) {
                // removes the published listener
                ServiceRegistry.createPublisherAccessFor(this).dispose();
                // allows garbage collector to delete the listener
                this.changeListener = null;
            }
        }
    }

    /**
     * Get the componentId of the component which is linked to this container.
     * 
     * @return the componentId
     */
    public String getComponentId() {
        return componentId;
    }

}
