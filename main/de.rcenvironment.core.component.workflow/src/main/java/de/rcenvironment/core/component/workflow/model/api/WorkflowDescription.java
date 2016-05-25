/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.EndpointChangeListener;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Describes a {@link WorkflowController} in a way that can be used by a {@link WorkflowRegistry} to
 * create that {@link WorkflowController}.
 * 
 * @author Roland Gude
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class WorkflowDescription extends PropertiesChangeSupport implements Serializable, Cloneable {

    /** Property that is fired when a WorkflowNode was added. */
    public static final String PROPERTY_NODES_OR_CONNECTIONS = "de.rcenvironment.wf.n_cn";
    
    /** Property that is fired when a WorkflowNode was added. */
    public static final String PROPERTY_NODES = "e.rcenvironment.wf.n";

    /** Property that is fired when a WorkflowNode was removed. */
    public static final String PROPERTY_CONNECTIONS = "e.rcenvironment.wf.cn";

    /** Property that is fired when a WorkflowLabel was removed. */
    public static final String PROPERTY_LABEL = "e.rcenvironment.wf.l";

    private static final long serialVersionUID = 339866937554580256L;

    private final String identifier;

    private int workflowVersionNumber = WorkflowConstants.INITIAL_WORKFLOW_VERSION_NUMBER;

    private String name;
    
    private String fileName;

    private String additionalInformation;
    
    private NodeIdentifier controllerNode;
    
    private boolean isControllerNodeIdTransient = false;

    private final List<WorkflowNode> nodes = new ArrayList<WorkflowNode>();

    private final List<Connection> connections = new ArrayList<Connection>();

    // removed temporarily the final declaration to initialize the labels later on due to backwards compatibility: <=6.1. -seid_do, April
    // 2014
    private List<WorkflowLabel> labels = new ArrayList<WorkflowLabel>();
    
    /**
     * @param identifier The identifier of the {@link WorkflowDescription}.
     */
    public WorkflowDescription(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getWorkflowVersion() {
        return workflowVersionNumber;
    }

    public void setWorkflowVersion(Integer workflowVersion) {
        workflowVersionNumber = workflowVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(final String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
    
    public NodeIdentifier getControllerNode() {
        return controllerNode;
    }

    public void setControllerNode(NodeIdentifier controllerNode) {
        this.controllerNode = controllerNode;
    }
    
    public boolean getIsControllerNodeIdTransient() {
        return isControllerNodeIdTransient;
    }
    
    public void setIsControllerNodeIdTransient(boolean isControllerNodeIdTransient) {
        this.isControllerNodeIdTransient = isControllerNodeIdTransient;
    }

    public List<WorkflowNode> getWorkflowNodes() {
        return nodes;
    }

    public List<WorkflowLabel> getWorkflowLabels() {
        return labels;
    }
    
    /**
     * Adds a new label to the list of @link {@link WorkflowLabel}s and fires a property change
     * event.
     * 
     * @param label the new label
     */
    public void addWorkflowLabel(WorkflowLabel label) {
        labels.add(label);
        firePropertyChange(PROPERTY_LABEL);
    }

    /**
     * Adds a labels to the list of @link {@link WorkflowLabel}s and fires a property change event.
     * 
     * @param labelsToAdd the new labels
     */
    public void addWorkflowLabels(List<WorkflowLabel> labelsToAdd) {
        for (WorkflowLabel label : labelsToAdd) {
            labels.add(label);
        }
        firePropertyChange(PROPERTY_LABEL);
    }
    
    /**
     * Set new labels. It replaces the current list of @link {@link WorkflowLabel}s and fires a property change event.
     * 
     * @param labelsToSet the new labels to set
     */
    public void setWorkflowLabels(List<WorkflowLabel> labelsToSet) {
        labels = labelsToSet;
        firePropertyChange(PROPERTY_LABEL);
    }

    /**
     * Returns the {@link WorkflowNode} with the given identifier.
     * 
     * @param nodeId the identifier of the desired {@link WorkflowNode}
     * @return the {@link WorkflowNode} with the given identifier
     * @throws IllegalArgumentException if no {@link WorkflowNode} with the given identifier exists
     */
    public WorkflowNode getWorkflowNode(final String nodeId) throws IllegalArgumentException {
        for (WorkflowNode node : nodes) {
            if (node.getIdentifier().equals(nodeId)) {
                return node;
            }
        }
        throw new IllegalArgumentException(StringUtils.format("No node with identifier %s found", nodeId));
    }

    private void addWorkflowNodeWithoutNotify(WorkflowNode node) {
        nodes.add(node);

        EndpointChangeListener l = new EndpointChangeListener(this);
        node.getInputDescriptionsManager().addPropertyChangeListener(l);
        node.getOutputDescriptionsManager().addPropertyChangeListener(l);
    }
    
    /**
     * Adds a new {@link WorkflowNode}.
     * 
     * @param node The new Workflow node.
     */
    public void addWorkflowNode(WorkflowNode node) {
        addWorkflowNodeWithoutNotify(node);

        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }
    
    /**
     * Adds a list of new {@link WorkflowNode}s.
     * 
     * @param nodesToAdd The list of new Workflow nodes.
     */
    public void addWorkflowNodes(List<WorkflowNode> nodesToAdd) {
        for (WorkflowNode node : nodesToAdd) {
            nodes.add(node);

            EndpointChangeListener l = new EndpointChangeListener(this);
            node.getInputDescriptionsManager().addPropertyChangeListener(l);
            node.getOutputDescriptionsManager().addPropertyChangeListener(l);
        }
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }
    
    /**
     * Adds a new list of {@link Connection}s at once.
     * 
     * @param nodeToAdd {@link WorkflowNode} to add
     * @param connectionsToAdd The list of {@link Connection}s to add.
     */
    public void addWorkflowNodeAndConnections(WorkflowNode nodeToAdd, List<Connection> connectionsToAdd) {
        addWorkflowNodeWithoutNotify(nodeToAdd);
        addConnectionsWithoutNotify(connectionsToAdd);
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }

    /**
     * Adds a new list of {@link Connection}s at once.
     * 
     * @param nodesToAdd {@link WorkflowNode}s to add
     * @param connectionsToAdd The list of {@link Connection}s to add.
     */
    public void addWorkflowNodesAndConnections(List<WorkflowNode> nodesToAdd, List<Connection> connectionsToAdd) {
        for (WorkflowNode nodeToAdd : nodesToAdd) {
            addWorkflowNodeWithoutNotify(nodeToAdd);
        }
        addConnectionsWithoutNotify(connectionsToAdd);
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }

    /**
     * Removes a {@link WorkflowLabel}.
     * 
     * @param label The {@link WorkflowLabel} to remove.
     */
    public void removeWorkflowLabel(WorkflowLabel label) {
        labels.remove(label);
        firePropertyChange(PROPERTY_LABEL);
    }

    /**
     * Removes a {@link WorkflowLabel}.
     * 
     * @param labelsToRemove The {@link WorkflowLabel}s to remove.
     */
    public void removeWorkflowLabels(List<WorkflowLabel> labelsToRemove) {
        for (WorkflowLabel label : labelsToRemove) {
            labels.remove(label);
        }
        firePropertyChange(PROPERTY_LABEL);
    }

    /**
     * Removes a {@link WorkflowNode}.
     * 
     * @param node The {@link WorkflowNode} to remove.
     */
    public void removeWorkflowNode(WorkflowNode node) {
        nodes.remove(node);
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }
    
    /**
     * Removes a {@link WorkflowNode} and its {@link Connection}s.
     * 
     * @param node {@link WorkflowNode} to delete
     */
    private List<Connection> removeWorkflowNodeAndRelatedConnectionsWithoutNotify(WorkflowNode node) {
        List<Connection> cnsToDelete = new ArrayList<>();
        for (Connection cn: getConnections()) {
            if (cn.getTargetNode().equals(node) || cn.getSourceNode().equals(node)) {
                cnsToDelete.add(cn);
            }
        }
        removeConnectionsWithoutNotify(cnsToDelete);
        nodes.remove(node);
        return cnsToDelete;
    }
    
    /**
     * Removes a {@link WorkflowNode} and its {@link Connection}s.
     * 
     * @param node {@link WorkflowNode} to delete
     * @return List of {@link Connection}s deleted
     */
    public List<Connection> removeWorkflowNodeAndRelatedConnections(WorkflowNode node) {
        List<Connection> cnsDeleted = removeWorkflowNodeAndRelatedConnectionsWithoutNotify(node);
        if (!cnsDeleted.isEmpty()) {
            firePropertyChange(PROPERTY_CONNECTIONS);
        }
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
        return cnsDeleted;
    }
    
    /**
     * Removes a list of {@link WorkflowNode}s and their {@link Connection}s.
     * 
     * @param nodesToRemove The list of {@link WorkflowNode}s to remove.
     * @return List of {@link Connection}s deleted
     */
    public List<Connection> removeWorkflowNodesAndRelatedConnections(List<WorkflowNode> nodesToRemove) {
        List<Connection> cnsDeleted = new ArrayList<>();
        for (WorkflowNode node : nodesToRemove){
            cnsDeleted.addAll(removeWorkflowNodeAndRelatedConnectionsWithoutNotify(node));
        }
        if (!cnsDeleted.isEmpty()) {
            firePropertyChange(PROPERTY_CONNECTIONS);
        }
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
        return cnsDeleted;
    }
    
    /**
     * Removes a list of {@link WorkflowNode}s and their {@link Connection}s.
     * 
     * @param nodesToRemove The list of {@link WorkflowNode}s to remove.
     * @return List of {@link Connection}s deleted
     */
    public List<Connection> removeWorkflowNodesAndRelatedConnectionsWithoutNotify(List<WorkflowNode> nodesToRemove) {
        List<Connection> cnsDeleted = new ArrayList<>();
        for (WorkflowNode node : nodesToRemove){
            cnsDeleted.addAll(removeWorkflowNodeAndRelatedConnectionsWithoutNotify(node));
        }
        return cnsDeleted;
    }
    
    /**
     * Removes a list of {@link WorkflowNode}s.
     * 
     * @param nodesToRemove The list of {@link WorkflowNode}s to remove.
     */
    public void removeWorkflowNodes(List<WorkflowNode> nodesToRemove) {
        for (WorkflowNode node : nodesToRemove){
            nodes.remove(node);
        }
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }

    /**
     * Removes a all {@link WorkflowNode}.
     */
    public void removeAllWorkflowNodes() {
        nodes.clear();
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_NODES);
    }
    
    /**
     * Returns all {@link Connection}s.
     * 
     * @return all {@link Connection}s.
     */
    public List<Connection> getConnections() {
        return new ArrayList<Connection>(connections);
    }

    /**
     * Adds a new {@link Connection}.
     * 
     * @param connection The {@link Connection} to add.
     */
    public void addConnection(Connection connection) {
        addConnectionWithoutNotify(connection);
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_CONNECTIONS);
    }
    
    /**
     * Adds a new list of {@link Connection}s at once.
     * 
     * @param connectionsToAdd The list of {@link Connection}s to add.
     */
    public void addConnections(List<Connection> connectionsToAdd) {
        addConnectionsWithoutNotify(connectionsToAdd);
        firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
        firePropertyChange(PROPERTY_CONNECTIONS);
    }
    
    private void addConnectionsWithoutNotify(List<Connection> connectionsToAdd) {
        for (Connection connection : connectionsToAdd){
            addConnectionWithoutNotify(connection);
        }
    }
    
    private void addConnectionWithoutNotify(Connection connection) {
        connections.add(connection);

        EndpointDescription output = connection.getOutput();
        EndpointDescription input = connection.getInput();

        getWorkflowNode(connection.getSourceNode().getIdentifier()).getOutputDescriptionsManager()
            .addConnectedDataType(output.getName(), input.getDataType());
        getWorkflowNode(connection.getTargetNode().getIdentifier()).getInputDescriptionsManager()
            .addConnectedDataType(input.getName(), output.getDataType());
    }
    
    /**
     * Removes a {@link Connection}.
     * 
     * @param connection The {@link Connection} to remove.
     */
    public void removeConnection(Connection connection) {
        if (removeConnectionWithoutNotify(connection)) {
            firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
            firePropertyChange(PROPERTY_CONNECTIONS);
        }
    }
    
    /**
     * Removes a list of {@link Connection}s at once.
     * 
     * @param connectionsToRemove The list of {@link Connection}s to remove.
     */
    public void removeConnections(List<Connection> connectionsToRemove){
        boolean removed = false;
        for (Connection connection : connectionsToRemove){
            if (removeConnectionWithoutNotify(connection)) {
                removed = true;
            }
        }
        if (removed) {
            firePropertyChange(PROPERTY_NODES_OR_CONNECTIONS);
            firePropertyChange(PROPERTY_CONNECTIONS);            
        }
    }
    
    private boolean removeConnectionWithoutNotify(Connection connection) {
        EndpointDescription output = connection.getOutput();
        EndpointDescription input = connection.getInput();

        getWorkflowNode(connection.getSourceNode().getIdentifier()).getOutputDescriptionsManager()
            .removeConnectedDataType(output.getName(), input.getDataType());
        getWorkflowNode(connection.getTargetNode().getIdentifier()).getInputDescriptionsManager()
            .removeConnectedDataType(input.getName(), output.getDataType());
        
        return connections.remove(connection);
    }
    
    private boolean removeConnectionsWithoutNotify(List<Connection> connectionsToRemove){
        boolean removed = false;
        for (Connection connection : connectionsToRemove){
            if (removeConnectionWithoutNotify(connection)) {
                removed = true;
            }
        }
        return removed;
    }
    
    /**
     * Removes a {@link Connection}.
     * 
     * @param connectionsToAdd The list of {@link Connection}s to add.
     */
    public void replaceConnections(List<Connection> connectionsToAdd) {
        removeConnectionsWithoutNotify(getConnections());
        addConnections(connectionsToAdd);
    }
    
    /**
     * Copies the execution information of the given workflow description. It includes name, controller's node, component's node and
     * "additional information".
     * 
     * @param wd workflow description with execution information to copy
     */
    public void copyExecutionInformationFromWorkflowDescription(WorkflowDescription wd) {
        setName(wd.getName());
        setControllerNode(wd.getControllerNode());
        setAdditionalInformation(wd.getAdditionalInformation());
        for (WorkflowNode node : getWorkflowNodes()) {
            if (wd.getWorkflowNode(node.getIdentifier()) != null) {
                node.getComponentDescription().setComponentInstallation(
                    wd.getWorkflowNode(node.getIdentifier()).getComponentDescription().getComponentInstallation());
            }
        }
    }

    /**
     * Clones a given {@link WorkflowDescription}.
     * 
     * @return the cloned {@link WorkflowDescription}.
     */
    @Override
    public WorkflowDescription clone() {

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            WorkflowDescription wd = (WorkflowDescription) ois.readObject();
            ois.close();
            bin.close();
            oos.close();
            bos.close();
            return wd;
        } catch (IOException e) {
            LogFactory.getLog(ComponentDescription.class).error("Failed to clone workflow description", e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            LogFactory.getLog(ComponentDescription.class).error("Failed to clone workflow description", e);
            throw new RuntimeException(e);
        }
    }

}
