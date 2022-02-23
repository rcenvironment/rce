/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.view.Messages;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Class that maps information about a running component onto the IPropertySource interface.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor change)
 */
public class WorkflowInstancePropertySource implements IPropertySource {

    protected static final String PROP_KEY_WORKKLOWPLATFORM = "de.rcenvironment.wfCtrlNodeId";

    protected static final String PROP_KEY_STARTTIME = "de.rcenvironment.wfStart";

    protected static final String PROP_KEY_NAME = "de.rcenvironment.wfName";

    protected static final String PROP_KEY_WORKFLOWNODES_COUNT = "de.rcenvironment.wfNodeCnt";

    protected static final String PROP_KEY_CONNECTIONS_COUNT = "de.rcenvironment.cnCnt";

    protected static final String PROP_KEY_COMPONENT_TYPES = "de.rcenvironment.wfNodeTypes";

    protected static final String PROP_KEY_INVOLVED_INSTANCES = "de.rcenvironment.involvedInstances";

    protected WorkflowExecutionInformation wfExeInfo;

    private final PlatformService platformService;

    public WorkflowInstancePropertySource(WorkflowExecutionInformation wfExeInfo) {
        this.wfExeInfo = wfExeInfo;
        this.platformService = ServiceRegistry.createAccessFor(this).getService(PlatformService.class);
    }

    @Override
    public Object getEditableValue() {
        return this;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        IPropertyDescriptor[] descriptors = new IPropertyDescriptor[7];

        descriptors[0] = new PropertyDescriptor(PROP_KEY_NAME, Messages.name);
        descriptors[1] = new PropertyDescriptor(PROP_KEY_STARTTIME, Messages.starttime);
        descriptors[2] = new PropertyDescriptor(PROP_KEY_WORKKLOWPLATFORM, Messages.platform);
        descriptors[3] = new PropertyDescriptor(PROP_KEY_WORKFLOWNODES_COUNT, "Component count");
        descriptors[4] = new PropertyDescriptor(PROP_KEY_CONNECTIONS_COUNT, "Connection count");
        descriptors[5] = new PropertyDescriptor(PROP_KEY_COMPONENT_TYPES, "Component types count");
        descriptors[6] = new PropertyDescriptor(PROP_KEY_INVOLVED_INSTANCES, "Instances involved");

        return descriptors;
    }

    @Override
    public Object getPropertyValue(Object key) {
        Object value = null;
        if (key.equals(PROP_KEY_NAME)) {
            value = wfExeInfo.getInstanceName();
        } else if (key.equals(PROP_KEY_WORKKLOWPLATFORM)) {
            if (wfExeInfo.getNodeId() == null || platformService.matchesLocalInstance(wfExeInfo.getNodeId())) {
                value = Messages.local;
            } else {
                value = wfExeInfo.getNodeId().getAssociatedDisplayName();
            }
        } else if (key.equals(PROP_KEY_STARTTIME)) {
            value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(wfExeInfo.getStartTime()));
        } else if (key.equals(PROP_KEY_WORKFLOWNODES_COUNT)) {
            value = wfExeInfo.getWorkflowDescription().getWorkflowNodes().size();
        } else if (key.equals(PROP_KEY_CONNECTIONS_COUNT)) {
            value = wfExeInfo.getWorkflowDescription().getConnections().size();
        } else if (key.equals(PROP_KEY_COMPONENT_TYPES)) {
            value = getComponentTypesCount();
        } else if (key.equals(PROP_KEY_INVOLVED_INSTANCES)) {
            value = getInstancesCount();
        }
        return value;
    }

    @Override
    public boolean isPropertySet(Object key) {
        return true;
    }

    private int getComponentTypesCount() {
        int count = 0;
        Set<String> componentIdentifiers = new HashSet<>();
        for (WorkflowNode node : wfExeInfo.getWorkflowDescription().getWorkflowNodes()) {
            if (!componentIdentifiers.contains(node.getComponentDescription().getIdentifier())) {
                componentIdentifiers.add(node.getComponentDescription().getIdentifier());
                count++;
            }
        }
        return count;
    }

    private int getInstancesCount() {
        int count = 0;
        Set<String> nodeIdentifiers = new HashSet<>();
        for (WorkflowNode node : wfExeInfo.getWorkflowDescription().getWorkflowNodes()) {
            if (!nodeIdentifiers.contains(node.getComponentDescription().getComponentInstallation().getNodeId())) {
                nodeIdentifiers.add(node.getComponentDescription().getComponentInstallation().getNodeId());
                count++;
            }
        }
        if (!nodeIdentifiers.contains(wfExeInfo.getNodeId().getLogicalNodeIdString())) {
            count++;
        }
        return count;
    }

    @Override
    public void resetPropertyValue(Object arg0) {}

    @Override
    public void setPropertyValue(Object arg0, Object arg1) {}

}
