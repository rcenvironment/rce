/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.workflow.view.Messages;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Class that maps information about a running component onto the IPropertySource interface.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor change)
 */
public class ComponentInstancePropertySource extends WorkflowInstancePropertySource {

    private static final String PROP_KEY_PLATFORM = "de.rcenvironment.rce.gui.workflow.view.properties.platform";

    private WorkflowNodeIdentifier wfNodeId;

    private final PlatformService platformService;

    public ComponentInstancePropertySource(WorkflowExecutionInformation wfExeInfo, WorkflowNodeIdentifier wfNodeId) {
        super(wfExeInfo);
        this.wfNodeId = wfNodeId;
        this.platformService = ServiceRegistry.createAccessFor(this).getService(PlatformService.class);
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        IPropertyDescriptor[] descriptors = new IPropertyDescriptor[4];

        descriptors[0] = new PropertyDescriptor(PROP_KEY_NAME, Messages.name);
        descriptors[1] = new PropertyDescriptor(PROP_KEY_STARTTIME, Messages.starttime);
        descriptors[2] = new PropertyDescriptor(PROP_KEY_PLATFORM, Messages.platform);
        descriptors[3] = new PropertyDescriptor(PROP_KEY_WORKKLOWPLATFORM, Messages.controllerPlatform);

        return descriptors;
    }

    @Override
    public Object getPropertyValue(Object key) {

        ComponentExecutionInformation compInstDescr = wfExeInfo.getComponentExecutionInformation(wfNodeId);
        if (compInstDescr == null) {
            return de.rcenvironment.core.gui.workflow.view.properties.Messages.componentInstanceUnknown;
        }
        Object value = null;
        if (key.equals(PROP_KEY_PLATFORM)) {
            if (compInstDescr.getNodeId() == null || platformService.matchesLocalInstance(compInstDescr.getNodeId())) {
                value = Messages.local;
            } else {
                value = compInstDescr.getNodeId().getAssociatedDisplayName();
            }
        } else if (key.equals(PROP_KEY_NAME)) {
            value = compInstDescr.getInstanceName();
        } else {
            value = super.getPropertyValue(key);
        }
        return value;
    }

}
