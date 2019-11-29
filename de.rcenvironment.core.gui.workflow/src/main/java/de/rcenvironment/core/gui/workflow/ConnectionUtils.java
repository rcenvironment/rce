/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;

/**
 * Utility class with useful methods when working with connections, connection
 * wrappers etc.
 * 
 * @author Oliver Seebach
 *
 */
public final class ConnectionUtils {

    private static final Log LOGGER = LogFactory.getLog(ConnectionUtils.class);

    private ConnectionUtils() {
        // prevent instantiation
    }

    /**
     * Finds connections between the two given nodes in the given workflow
     * description. Considers both directions.
     * 
     * @param sourceNode
     *            The first node to be considered.
     * @param targetNode
     *            The second node to be considered.
     * @param workflowDescription
     *            The workflow description to be searched in.
     * @return The connections between the two given nodes.
     */
    public static List<Connection> getConnectionsBetweenNodes(WorkflowNode sourceNode, WorkflowNode targetNode,
            WorkflowDescription workflowDescription) {

        List<Connection> connectionsBetweenNodes = new ArrayList<>();

        if (sourceNode != null && targetNode != null && workflowDescription != null) {
            for (Connection connection : workflowDescription.getConnections()) {
                if ((connection.getSourceNode().getIdentifierAsObject().equals(sourceNode.getIdentifierAsObject())
                        && connection.getTargetNode().getIdentifierAsObject()
                                .equals(targetNode.getIdentifierAsObject()))
                        || (connection.getSourceNode().getIdentifierAsObject()
                                .equals(targetNode.getIdentifierAsObject())
                                && connection.getTargetNode().getIdentifierAsObject()
                                        .equals(sourceNode.getIdentifierAsObject()))) {
                    connectionsBetweenNodes.add(connection);
                }
            }
        }
        return connectionsBetweenNodes;
    }

    /**
     * Finds connections between the two given nodes in the given workflow
     * description. Considers just one direction, source to target.
     * 
     * @param sourceNode
     *            The first node to be considered.
     * @param targetNode
     *            The second node to be considered.
     * @param workflowDescription
     *            The workflow description to be searched in.
     * @return The connections between the two given nodes.
     */
    public static List<Connection> getConnectionsFromSourceToTarget(WorkflowNode sourceNode, WorkflowNode targetNode,
            WorkflowDescription workflowDescription) {

        List<Connection> connectionsBetweenNodes = new ArrayList<>();

        if (sourceNode != null && targetNode != null && workflowDescription != null) {
            for (Connection connection : workflowDescription.getConnections()) {
                if ((connection.getSourceNode().getIdentifierAsObject().equals(sourceNode.getIdentifierAsObject())
                        && connection.getTargetNode().getIdentifierAsObject()
                                .equals(targetNode.getIdentifierAsObject()))) {
                    connectionsBetweenNodes.add(connection);
                }
            }
        }
        return connectionsBetweenNodes;
    }

    /**
     * Finds a list of bendpoints for a connection between two given workflow nodes
     * in the given workflow description. As the bendpoints are equal for all
     * connections between the same nodes the result is returned as soon as the
     * first connection matches the given source and target nodes. Considers both
     * directions.
     * 
     * @param source
     *            The source node.
     * @param target
     *            The target node.
     * @param workflowDescription
     *            The workflow description.
     * @return The list of
     */
    public static List<Location> findAlreadyExistentBendpointsBySourceAndTargetIgnoringDirection(WorkflowNode source,
            WorkflowNode target, WorkflowDescription workflowDescription) {
        List<Location> alreadyExistentBendpoints = new ArrayList<>();
        for (Connection connection : workflowDescription.getConnections()) {
            if ((connection.getSourceNode().getIdentifierAsObject().equals(source.getIdentifierAsObject())
                    && connection.getTargetNode().getIdentifierAsObject().equals(target.getIdentifierAsObject()))) {
                alreadyExistentBendpoints = connection.getBendpoints();
                break;
            } else if (connection.getSourceNode().getIdentifierAsObject().equals(target.getIdentifierAsObject())
                    && connection.getTargetNode().getIdentifierAsObject().equals(source.getIdentifierAsObject())) {
                // invert order
                for (Location l : connection.getBendpoints()) {
                    alreadyExistentBendpoints.add(0, l);
                }
                break;
            }
        }
        return alreadyExistentBendpoints;
    }

    /**
     * Finds a list of bendpoints for a connection between two given workflow nodes
     * in the given workflow description. As the bendpoints are equal for all
     * connections between the same nodes the result is returned as soon as the
     * first connection matches the given source and target nodes. Considers just
     * one direction, source to target.
     * 
     * @param source
     *            The source node.
     * @param target
     *            The target node.
     * @param workflowDescription
     *            The workflow description.
     * @return The list of
     */
    public static List<Location> findAlreadyExistentBendpointsFromSourceToTarget(WorkflowNode source,
            WorkflowNode target, WorkflowDescription workflowDescription) {
        List<Location> alreadyExistentBendpoints = new ArrayList<>();
        for (Connection connection : workflowDescription.getConnections()) {
            if ((connection.getSourceNode().getIdentifierAsObject().equals(source.getIdentifierAsObject())
                    && connection.getTargetNode().getIdentifierAsObject().equals(target.getIdentifierAsObject()))) {
                alreadyExistentBendpoints = connection.getBendpoints();
                break;
            }
        }
        return alreadyExistentBendpoints;
    }

    /**
     * Translates a list of bendpoints by a given offset.
     * 
     * @param originalBendpoints
     *            The original bendpoints.
     * @param bendpointOffsetX
     *            The offset to translate in X direction.
     * @param bendpointOffsetY
     *            The offset to translate in Y direction.
     * @return The translated list of bendpoints.
     */
    public static List<Location> translateBendpointListByOffset(List<Location> originalBendpoints, int bendpointOffsetX,
            int bendpointOffsetY) {
        List<Location> bendpointsWithOffset = new ArrayList<>();
        for (Location originalLocation : originalBendpoints) {
            bendpointsWithOffset
                    .add(new Location(originalLocation.x + bendpointOffsetX, originalLocation.y + bendpointOffsetY));
        }
        return bendpointsWithOffset;
    }

    /**
     * Validates whether all connections in a connectionwrapper have the same amount
     * of bendpoints.
     * 
     * @param connectionWrapper
     *            The connection wrapper to be considered
     * @param workflowDescription
     *            The workflow description to be considered
     * @param classAndMethod
     *            The calling class and method.
     */
    public static void validateConnectionWrapperForEqualBendpointLocations(WorkflowDescription workflowDescription,
            ConnectionWrapper connectionWrapper, String classAndMethod) {
        List<Connection> connections = getConnectionsByWrapperSameDirection(workflowDescription, connectionWrapper);
        validateConnections(connections, classAndMethod);
    }

    /**
     * Validates whether connections have the same bendpoints.
     * 
     * @param connections
     *            The connections to validate.
     * @param classAndMethod
     *            The calling class and method.
     */
    public static void validateConnections(List<Connection> connections, String classAndMethod) {
        int numberOfBendpoints = connections.get(0).getBendpoints().size();
        for (Connection connection : connections) {
            if (!checkIfBendpointListsAreEqual(connections.get(0).getBendpoints(), connection.getBendpoints())) {
                LOGGER.error("Connections' bendpoints are inconsistent! Connection between '"
                        + connection.getSourceNode().getName() + "' and '" + connection.getTargetNode().getName()
                        + "' is affected. It has " + connection.getBendpoints().size() + " bendpoints ("
                        + connection.getBendpoints() + "), but should have " + numberOfBendpoints + " bendpoints ("
                        + connections.get(0).getBendpoints() + "). Caused by " + classAndMethod);
                break;
            }
        }
    }

    private static boolean checkIfBendpointListsAreEqual(List<Location> bendpointList,
            List<Location> bendpointListToCompare) {
        if (bendpointList.size() != bendpointListToCompare.size()) {
            return false;
        }
        for (int i = 0; i < bendpointList.size(); i++) {
            if ((bendpointList.get(i).x != bendpointListToCompare.get(i).x
                    && bendpointList.get(i).x != bendpointListToCompare.get(bendpointList.size() - i - 1).x)
                    || (bendpointList.get(i).y != bendpointListToCompare.get(i).y
                            && bendpointList.get(i).y != bendpointListToCompare.get(bendpointList.size() - i - 1).y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the connections wrapped in a connection wrapper.
     * 
     * @param connectionWrapper
     *            The connection wrapper to be considered
     * @param workflowDescription
     *            The workflow description to be considered
     * @return The connections in a connection wrapper.
     */
    public static List<Connection> getConnectionsByWrapperIgnoreOrientation(WorkflowDescription workflowDescription,
            ConnectionWrapper connectionWrapper) {
        List<Connection> connections = new ArrayList<>();
        for (Connection connectionInModel : workflowDescription.getConnections()) {
            if ((connectionWrapper.getSource().getIdentifierAsObject()
                    .equals(connectionInModel.getSourceNode().getIdentifierAsObject())
                    && connectionWrapper.getTarget().getIdentifierAsObject()
                            .equals(connectionInModel.getTargetNode().getIdentifierAsObject()))
                    || (connectionWrapper.getTarget().getIdentifierAsObject()
                            .equals(connectionInModel.getSourceNode().getIdentifierAsObject())
                            && connectionWrapper.getSource().getIdentifierAsObject()
                                    .equals(connectionInModel.getTargetNode().getIdentifierAsObject()))) {
                connections.add(connectionInModel);
            }
        }
        return connections;
    }

    /**
     * Return the connections wrapped in a connection wrapper.
     * 
     * @param connectionWrapper
     *            The connection wrapper to be considered
     * @param workflowDescription
     *            The workflow description to be considered
     * @return The connections in a connection wrapper.
     */
    public static List<Connection> getConnectionsByWrapperSameDirection(WorkflowDescription workflowDescription,
            ConnectionWrapper connectionWrapper) {
        List<Connection> connections = new ArrayList<>();
        for (Connection connectionInModel : workflowDescription.getConnections()) {
            if ((connectionWrapper.getSource().getIdentifierAsObject()
                    .equals(connectionInModel.getSourceNode().getIdentifierAsObject())
                    && connectionWrapper.getTarget().getIdentifierAsObject()
                            .equals(connectionInModel.getTargetNode().getIdentifierAsObject()))) {
                connections.add(connectionInModel);
            }
        }
        return connections;
    }

}
