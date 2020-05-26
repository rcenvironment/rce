/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;


/**
 * Command to remove all bendpoints from a connection at once.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointDeleteAllCommand extends Command {

    private static final String SOURCE_TARGET_SEPARATOR = ":";

    private List<Connection> connections = new ArrayList<>();
    
    private ConnectionWrapper referencedWrapper;
    
    private WorkflowDescription workflowDescription;
    
    // By convention the key of this map is "SourceId:TargetId" of the nodes of the associated connection.
    private Map<String, List<Location>> removedBendpointsPerConnection = new HashMap<>();

    @Override
    public void execute() {
        for (Connection connection : connections){
            if (!removedBendpointsPerConnection.keySet().contains(connection)){
                removedBendpointsPerConnection.put(connection.getSourceNode().getIdentifierAsObject().toString() 
                    + SOURCE_TARGET_SEPARATOR 
                    + connection.getTargetNode().getIdentifierAsObject().toString()
                    , connection.getBendpoints());
            }
            connection.removeAllBendpoints();
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedWrapper, 
            this.getClass().getSimpleName() + " execute or redo");
    }

    @Override
    public void undo() {
        for (String sourceTargetString : removedBendpointsPerConnection.keySet()){
            String sourceId = sourceTargetString.split(SOURCE_TARGET_SEPARATOR)[0];
            String targetId = sourceTargetString.split(SOURCE_TARGET_SEPARATOR)[1];
            for (Connection connection : connections){
                List<Location> bendpointsToRestore = new ArrayList<>();
                if (connection.getSourceNode().getIdentifierAsObject().toString().equals(sourceId) 
                    && connection.getTargetNode().getIdentifierAsObject().toString().equals(targetId)){
                    bendpointsToRestore = removedBendpointsPerConnection.get(sourceId + SOURCE_TARGET_SEPARATOR + targetId); 
                }
                connection.addBendpoints(bendpointsToRestore);
            }
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedWrapper, 
            this.getClass().getSimpleName() + " undo");
    }
    
    @Override
    public void redo() {
        execute();
    }

    
    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }
    
    public void setReferencedModel(ConnectionWrapper referencedModel) {
        this.referencedWrapper = referencedModel;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }
    
}
