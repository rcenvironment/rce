/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;

/**
 * Connection class for connecting {@link ComponentDescription}s within a {@link WorkflowDescription}.
 * 
 * @author Roland Gude
 * @author Heinrich Wendel
 * @author Oliver Seebach
 */
public class Connection extends PropertiesChangeSupport implements Serializable, Comparable<Connection> {

    /** Property that is fired when a bendpoint has been changed. */
    public static final String PROPERTY_BENDPOINT = "de.rcenvironment.bendpoint";
    
    private static final long serialVersionUID = 6019856436149503867L;

    /** The source {@link WorkflowNode}. */
    private final WorkflowNode sourceWorkflowNode;

    /** The output of the source {@link WorkflowNode}. */
    private final EndpointDescription outputEndpointDescription;

    /** The target {@link WorkflowNode}. */
    private final WorkflowNode targetWorkflowNode;

    /** The input of the target {@link WorkflowNode}. */
    private final EndpointDescription inputEndpointDescription;

    private List<Location> bendpoints = new ArrayList<>();

    public Connection(WorkflowNode source, EndpointDescription output, WorkflowNode target, EndpointDescription input) {
        this.sourceWorkflowNode = source;
        this.outputEndpointDescription = output;
        this.targetWorkflowNode = target;
        this.inputEndpointDescription = input;
    }
    
    public Connection(WorkflowNode source, EndpointDescription output, WorkflowNode target, 
        EndpointDescription input, List<Location> bendpoints) {
        this.sourceWorkflowNode = source;
        this.outputEndpointDescription = output;
        this.targetWorkflowNode = target;
        this.inputEndpointDescription = input;
        this.bendpoints = new ArrayList<Location>(bendpoints);
    }

    public WorkflowNode getSourceNode() {
        return sourceWorkflowNode;
    }

    public EndpointDescription getOutput() {
        return outputEndpointDescription;
    }

    public WorkflowNode getTargetNode() {
        return targetWorkflowNode;
    }

    public EndpointDescription getInput() {
        return inputEndpointDescription;
    }

    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof Connection) {
            Connection c = (Connection) o;
            if (c.getTargetNode().getIdentifier().equals(targetWorkflowNode.getIdentifier())
                && c.getSourceNode().getIdentifier().equals(sourceWorkflowNode.getIdentifier())
                && inputEndpointDescription.getIdentifier().equals(c.getInput().getIdentifier())
                && outputEndpointDescription.getIdentifier().equals(c.getOutput().getIdentifier())) {
                equals = true;
            }
        }
        return equals;
    }

    @Override
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(targetWorkflowNode.getIdentifier());
        builder.append(sourceWorkflowNode.getIdentifier());
        builder.append(inputEndpointDescription.getIdentifier());
        builder.append(outputEndpointDescription.getIdentifier());
        return builder.toString().hashCode();
    }
    
    /**
     * Adds a list of bendpoints to a connection. The index of the list passed it used as index of the bendpoints.
     * 
     * The {@link Location} instances are not modified. TODO remove comment if {@link Location} is immutable.
     * 
     * @param locations list describing bendpoints
     */
    public void addBendpoints(List<Location> locations){
        for (int i = 0; i < locations.size(); i++) {
            this.bendpoints.add(i, new Location(locations.get(i).x, locations.get(i).y));
        }
        firePropertyChange(PROPERTY_BENDPOINT);
    }
    
    /**
     * Adds a bendpoint to a connection.
     * 
     * @param index the index
     * @param x the x coordinate
     * @param y the y coordinate
     * @param inverse flag whether the connection is from source to target or inverse
     */
    public void addBendpoint(int index, int x, int y, boolean inverse){
        int indexToUse = new Integer(index);
        if (inverse) {
            indexToUse = (bendpoints.size() - indexToUse);
        }
        boolean alreadyExistent = false;
        for (Location location : this.bendpoints){
            if (location.x == x && location.y == y){
                alreadyExistent = true;
            }
        }
        if (!alreadyExistent){
            this.bendpoints.add(indexToUse, new Location(x, y));
            firePropertyChange(PROPERTY_BENDPOINT);
        }
    }
    
    /**
     * Removes a bendpoint from a connection.
     * 
     * @param index the index
     * @param inverse whether connection is inverse wrt its wrapper
     */
    public void removeBendpoint(int index, boolean inverse){
        int indexToUse = new Integer(index);
        if (inverse) {
            indexToUse = (bendpoints.size() - indexToUse - 1);
        }
        if (indexToUse >= 0 && indexToUse < bendpoints.size() && bendpoints.size() > 0){
            bendpoints.remove(indexToUse);
        }
        firePropertyChange(PROPERTY_BENDPOINT);
    }
    
    
    /**
     * Sets a bendpoint at the given index.
     * 
     * @param index the index
     * @param x the x coordinate
     * @param y the y coordinate
     * @param inverse whether connection is inverse wrt its wrapper
     */
    public void setBendpoint(int index, int x, int y, boolean inverse){
        int indexToUse = new Integer(index);
        if (inverse) {
            indexToUse = (bendpoints.size() - indexToUse) - 1;
        }
        if (indexToUse >= 0 && indexToUse <= bendpoints.size() && bendpoints.size() > 0){
            Location location = new Location(x, y);
            bendpoints.set(indexToUse, location);
        }
        firePropertyChange(PROPERTY_BENDPOINT);
    }
    
    /**
     * Removes a bendpoint from a connection.
     * 
     * @param index the index
     */
    public void removeBendpoint(int index){
        bendpoints.remove(index);
        firePropertyChange(PROPERTY_BENDPOINT);
    }
    
    /**
     * Returns a bendpoint from a connection.
     * 
     * @param index the index
     * @return the bendpoint
     */
    public Location getBendpoint(int index){
        return bendpoints.get(index);
    }

    /**
     * @return list of bendpoints or <code>null</code> if no bendpoints exist (null check needed as member variable 'bendpoints' can be null
     *         until RCE 7)
     */
    public List<Location> getBendpoints() {
        if (bendpoints != null) {
            return new ArrayList<Location>(bendpoints);            
        } else {
            return null;
        }
    }
    
    /**
     * Sets the bendpoints for a connection.
     *
     * @param bendpoints the bendpoints to be set.
     */
    public void setBendpoints(List<Location> bendpoints) {
        this.bendpoints = new ArrayList<Location>(bendpoints);
        firePropertyChange(PROPERTY_BENDPOINT);
    }
    
    /**
     * Sets a bendpoint at the given index.
     * 
     * @param index the index
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void setBendpoint(int index, int x, int y){
        Location location = new Location(x, y);
        bendpoints.set(index, location);
        firePropertyChange(PROPERTY_BENDPOINT);
    }
    
    /**
     * Removes all bendpoints from a connection.
     */
    public void removeAllBendpoints(){
        bendpoints = new ArrayList<>();
        firePropertyChange(PROPERTY_BENDPOINT);
    }

    @Override
    public int compareTo(Connection o) {
        int result = getSourceNode().compareTo(o.getSourceNode());
        if (result == 0) {
            result = getOutput().compareTo(o.getOutput());
            if (result == 0) {
                result = getTargetNode().compareTo(o.getTargetNode());
                if (result == 0) {
                    result = getInput().compareTo(o.getInput());
                }
            }
        }
        return result;
    }
    
}
