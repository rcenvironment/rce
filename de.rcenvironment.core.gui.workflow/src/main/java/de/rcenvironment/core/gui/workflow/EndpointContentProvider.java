/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Content Provider taking a WorkflowDescription as root and displaying all componets with either
 * their inputs or outputs.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class EndpointContentProvider implements ITreeContentProvider {

    private EndpointType type;

    /**
     * @param type Display input or output endpoints?
     */
    public EndpointContentProvider(EndpointType type) {
        this.type = type;
    }

    @Override
    public Object[] getChildren(Object element) {
        if (element instanceof WorkflowDescription) {
            List<WorkflowNode> items = ((WorkflowDescription) element).getWorkflowNodes();
            Collections.sort(items);
            return items.toArray();
        } else if (element instanceof WorkflowNode) {
            List<EndpointItem> items = new ArrayList<EndpointItem>();
            items.addAll(EndpointHandlingHelper.getEndpoints((WorkflowNode) element, type));
            Collections.sort(items);
            return items.toArray();
        } else {
            List<EndpointItem> items = new ArrayList<EndpointItem>();
            Collections.sort(items);
            return items.toArray();
        }
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return (getChildren(element) != null && getChildren(element).length > 0);
    }

    @Override
    public Object[] getElements(Object element) {
        return getChildren(element);
    }

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer view, Object object1, Object object2) {

    }

    /**
     * Class representing one item in the endpoint tree.
     * 
     * @author Doreen Seider
     */
    public static class EndpointItem implements Serializable, Comparable<EndpointItem> {

        private static final long serialVersionUID = 777733457712592306L;

        private String name;

        private EndpointDescription endpointDesc;

        public EndpointItem(EndpointDescription endpointDesc) {
            this.endpointDesc = endpointDesc;
            name = endpointDesc.getName();
        }

        public String getName() {
            return name;
        }

        public EndpointDescription getEndpointDescription() {
            return endpointDesc;
        }

        public String getShortName() {
            return name;
        }

        @Override
        public int compareTo(EndpointItem o) {
            return name.compareTo(o.getName());
        }

    }

    /**
     * Class representing one endpoint.
     * 
     * @author Heinrich Wendel
     */
    public static class Endpoint extends EndpointItem {

        private static final long serialVersionUID = 1633769598091968303L;

        private WorkflowNode parent;

        public Endpoint(WorkflowNode parent, EndpointDescription endpointDesc) {
            super(endpointDesc);
            this.parent = parent;
        }

        public WorkflowNode getWorkflowNode() {
            return parent;
        }
    }

}
