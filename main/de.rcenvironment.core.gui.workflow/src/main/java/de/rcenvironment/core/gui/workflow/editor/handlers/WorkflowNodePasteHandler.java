/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentSize;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;
import de.rcenvironment.core.gui.workflow.parts.EditorEditPartFactory;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * Pasting {@link WorkflowNode}s, {@link Connection}s, {@link EndpointDescription}s and {@link WorkflowLabel}s to the {@link WorkflowEditor}
 * .
 * 
 * 
 * @author Doreen Seider
 * @author Oliver Seebach
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author David Scholz
 */
// TODO fix class name, it is not longer only a paste handler for WorkflowNodes
public class WorkflowNodePasteHandler extends AbstractWorkflowNodeEditHandler {

    private final Map<String, String> nameMapping = new HashMap<String, String>();

    // mapping between old identifier and new workflow node !
    private final Map<WorkflowNode, WorkflowNode> nodeMapping = new HashMap<WorkflowNode, WorkflowNode>();

    private final Map<EndpointDescription, EndpointDescription> endpointMapping = new HashMap<EndpointDescription, EndpointDescription>();

    private final Map<String, EndpointDescription> endpointIDMapping = new HashMap<String, EndpointDescription>();

    private final Map<WorkflowNode, Rectangle> newNodeAndLocationMapping = new HashMap<>();

    private int nodeConstraintPositionCounter = 0;

    private int labelConstraintPositionCounter = 0;

    private final int offset = 20;

    private int componentSizeOffset = 0;

    private boolean hasConnections = false;

    private int nodeMinX = Integer.MAX_VALUE;

    private int nodeMinY = Integer.MAX_VALUE;

    private int labelMinX = Integer.MAX_VALUE;

    private int labelMinY = Integer.MAX_VALUE;

    private Point editorOffsetPoint = null;

    private boolean pasteTriggeredByMouse = false;

    @Override
    void edit() {
        
        nameMapping.clear();
        nodeConstraintPositionCounter = 0;
        hasConnections = false;
        labelConstraintPositionCounter = 0;
        
        List<WorkflowNode> workflowNodesToCreate = new LinkedList<>();
        List<WorkflowNodePart> pastedWorkflowNodeParts = new LinkedList<>();
        List<WorkflowLabel> workflowLabelsToCreate = new LinkedList<>();
        List<Connection> connectionsToCreate = new LinkedList<>();
        List<Rectangle> nodeConstraintsToCreate = new LinkedList<>();
        List<Rectangle> labelConstraintsToCreate = new LinkedList<>();

        // determines whether the pasting was triggered via hotkey ctrl+v or via mouse (i.e. editor's context menu)
        if (viewer.getContextMenu().isDirty()) {
            pasteTriggeredByMouse = false;
        } else {
            pasteTriggeredByMouse = true;
        }
        // determines the offset of the visible part of the editor from the editor's origin
        if (viewer.getControl() instanceof FigureCanvas) {
            editorOffsetPoint = ((FigureCanvas) viewer.getControl()).getViewport().getViewLocation();
        }

        Object content = null;
        try {
            content = extractContentfromSystemClipboard();
        } catch (UnsupportedFlavorException | IOException | ParseException e) {
            LogFactory.getLog(getClass()).debug("Error when extracting content from system clipboard: " + e.getMessage());
            return;
        }
        Map<String, List<String>> otherNodesCombined = new HashMap<String, List<String>>();
        // List of all nodes in the editor
        List<String> editorNodeNames = new LinkedList<String>();
        for (Object e : viewer.getContents().getChildren()) {
            if (e instanceof WorkflowNodePart) {
                editorNodeNames.add(((WorkflowNode) ((WorkflowNodePart) e).getModel()).getName());
            }
        }
        if (content instanceof List) {
            // create map with node name as key and all other node names as value
            for (Object nodePart : (List<?>) content) {
                if (nodePart instanceof Connection) {
                    hasConnections = true;
                }

                if (nodePart instanceof WorkflowNodePart) {
                    // create list of all nodes in clipboard
                    List<String> otherNodes = new ArrayList<String>();
                    for (Object nodePart2 : (List<?>) content) {
                        if (nodePart2 instanceof WorkflowNodePart) {
                            WorkflowNodePart part = (WorkflowNodePart) nodePart2;
                            WorkflowNode node = (WorkflowNode) part.getModel();
                            otherNodes.add(node.getName());
                        }
                    }
                    WorkflowNodePart part = (WorkflowNodePart) nodePart;
                    WorkflowNode node = (WorkflowNode) part.getModel();
                    // remove current node from other's list
                    otherNodes.remove(node.getName());
                    // add nodes from editor to list of other nodes
                    otherNodes.addAll(editorNodeNames);
                    otherNodesCombined.put(node.getName(), otherNodes);
                }
            }

            getMinNodeAndLabel(content);

            for (Object partToPaste : (List<?>) content) {

                if (partToPaste instanceof WorkflowNodePart) {
                    WorkflowNodePart part = (WorkflowNodePart) partToPaste;
                    pastedWorkflowNodeParts.add(part);
                    WorkflowNode node = (WorkflowNode) part.getModel();
                    String nodeName = node.getName();
                    String newName = nodeName;

                    if (otherNodesCombined.get(nodeName).contains(nodeName)) {
                        newName = "Copy of " + nodeName;
                    }
                    int i = 2;
                    while (otherNodesCombined.get(nodeName).contains(newName)) {
                        newName = "Copy (" + i++ + ") of " + nodeName;
                    }
                    WorkflowNode newNode = new WorkflowNode(copyComponentDescription(node.getComponentDescription()));
                    newNode.setName(newName);
                    newNode.setEnabled(node.isEnabled());
                    workflowNodesToCreate.add(newNode);
                    addNewNodePosition(nodeConstraintsToCreate, node);

                    nameMapping.put(node.getIdentifier(), newNode.getIdentifier());
                    nodeMapping.put(node, newNode);

                    // mapping to store new nodes determined location without altering the node itself
                    newNodeAndLocationMapping.put(newNode, nodeConstraintsToCreate.get(nodeConstraintPositionCounter));

                    nodeConstraintPositionCounter++;

                    for (EndpointDescription endpoint : node.getInputDescriptionsManager().getEndpointDescriptions()) {
                        EndpointDescription newEndpoint = newNode.getInputDescriptionsManager().getEndpointDescription(endpoint.getName());
                        endpointMapping.put(endpoint, newEndpoint);
                        endpointIDMapping.put(endpoint.getIdentifier(), newEndpoint);
                    }
                    for (EndpointDescription endpoint : node.getOutputDescriptionsManager().getEndpointDescriptions()) {
                        EndpointDescription newEndpoint = newNode.getOutputDescriptionsManager().getEndpointDescription(endpoint.getName());
                        endpointMapping.put(endpoint, newEndpoint);
                        endpointIDMapping.put(endpoint.getIdentifier(), newEndpoint);
                    }
                }
                if (partToPaste instanceof WorkflowLabel) {
                    WorkflowLabel label = (WorkflowLabel) partToPaste;
                    WorkflowLabel newLabel = createCopiedWorkflowLabel(label);
                    newLabel.setLocation(label.getX() + offset, label.getY() + offset);
                    workflowLabelsToCreate.add(newLabel);

                    addNewLabelPosition(labelConstraintsToCreate, label);
                    labelConstraintPositionCounter++;
                }
                if (partToPaste instanceof Connection) {
                    Connection oldConnection = (Connection) partToPaste;
                    WorkflowNode source = nodeMapping.get(oldConnection.getSourceNode());
                    WorkflowNode target = nodeMapping.get(oldConnection.getTargetNode());
                    EndpointDescription sourceOutput = endpointIDMapping.get(oldConnection.getOutput().getIdentifier());
                    EndpointDescription targetInput = endpointIDMapping.get(oldConnection.getInput().getIdentifier());
                    List<Location> originalBendpoints = oldConnection.getBendpoints();
                    int bendpointOffsetX = newNodeAndLocationMapping.get(source).x - oldConnection.getSourceNode().getX();
                    int bendpointOffsetY = newNodeAndLocationMapping.get(source).y - oldConnection.getSourceNode().getY();
                    List<Location> bendpointsWithOffset =
                        ConnectionUtils.translateBendpointListByOffset(originalBendpoints, bendpointOffsetX, bendpointOffsetY);
                    Connection newConnection = new Connection(source, sourceOutput, target, targetInput, bendpointsWithOffset);
                    connectionsToCreate.add(newConnection);
                }
            }
        }
        WorkflowNodeLabelConnectionCreateCommand nodeAndConnectionCreateCommand = new WorkflowNodeLabelConnectionCreateCommand(
            workflowNodesToCreate,
            workflowLabelsToCreate,
            connectionsToCreate,
            (WorkflowDescription) viewer.getContents().getModel(),
            nodeConstraintsToCreate, labelConstraintsToCreate);
        commandStack.execute(nodeAndConnectionCreateCommand);

        selectPastedNodes(workflowNodesToCreate);
    }

    private void selectPastedNodes(List<WorkflowNode> workflowNodesToCreate) {
        // move selection from original components to pasted ones
        viewer.deselectAll();
        for (Object editPartObject : viewer.getContents().getChildren()) {
            if (editPartObject instanceof WorkflowNodePart) {
                WorkflowNodePart nodePart = (WorkflowNodePart) editPartObject;
                WorkflowNode node = (WorkflowNode) nodePart.getModel();
                if (workflowNodesToCreate.contains(node)) {
                    viewer.appendSelection(nodePart);
                }
            }
        }
    }

    private Object extractContentfromSystemClipboard() throws UnsupportedFlavorException, IOException, ParseException {
        Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String clipboardText = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            InputStream inputStream = new ByteArrayInputStream(clipboardText.getBytes());
            JsonFactory f = new JsonFactory();
            JsonParser jp = f.createJsonParser(inputStream);
            List<?> parsedIS = parseInputStream(jp);
            jp.close();
            return parsedIS;
        }
        return null;
    }

    private void getMinNodeAndLabel(Object content) {
        nodeMinX = Integer.MAX_VALUE;
        nodeMinY = Integer.MAX_VALUE;
        for (Object partToPast : (List<?>) content) {
            if (partToPast instanceof WorkflowNodePart) {
                WorkflowNodePart part = (WorkflowNodePart) partToPast;
                WorkflowNode node = (WorkflowNode) part.getModel();
                getMinXandYForNode(node.getX(), node.getY());
            } else if (partToPast instanceof WorkflowLabelPart) {
                WorkflowLabelPart part = (WorkflowLabelPart) partToPast;
                WorkflowLabel label = (WorkflowLabel) part.getModel();
                getMinXandYForLabel(label.getX(), label.getY());
            }
        }
    }

    private void getMinXandYForNode(int nodeX, int nodeY) {
        if (nodeX < nodeMinX) {
            nodeMinX = nodeX;
        }

        if (nodeY < nodeMinY) {
            nodeMinY = nodeY;
        }
    }

    private void getMinXandYForLabel(int labelX, int labelY) {
        if (labelX < labelMinX) {
            nodeMinX = labelX;
        }

        if (labelY < labelMinY) {
            labelMinY = labelY;
        }
    }

    private void addNewNodePosition(List<Rectangle> nodeConstraintsToCreate, WorkflowNode node) {
        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        if (pasteTriggeredByMouse) {
            if (hasConnections) {
                nodeConstraintsToCreate.add(nodeConstraintPositionCounter, new Rectangle(
                    (node.getX() - nodeMinX + editor.getMouseX() + editorOffsetPoint.x),
                    (node.getY() - nodeMinY + editor.getMouseY() + editorOffsetPoint.y), 0, 0));
            } else {
                findFreeSpotForNode(editor.getMouseX() + editorOffsetPoint.x, editor.getMouseY() + editorOffsetPoint.y,
                    node.getComponentDescription().getSize(), nodeConstraintsToCreate);
            }
        } else {
            if (!model.getWorkflowNodes().contains(node) && !hasConnections) {
                findFreeSpotForNode(offset, offset, node.getComponentDescription().getSize(), nodeConstraintsToCreate);
            } else {
                findFreeSpotForNode(node.getX(), node.getY(), node.getComponentDescription().getSize(), nodeConstraintsToCreate);
            }
        }
    }

    private void findFreeSpotForNode(int x, int y, ComponentSize componentSize, List<Rectangle> nodeConstraintsToCreate) {
        if (!isNodePositionValid(x, y, componentSize)) {
            findFreeSpotForNode(x + offset, y + offset, componentSize, nodeConstraintsToCreate);
        } else {
            for (Rectangle rectangle : nodeConstraintsToCreate) {
                if (rectangle.equals(x, y, 0, 0)) {
                    x += offset;
                    y += offset;
                }
            }
            nodeConstraintsToCreate.add(nodeConstraintPositionCounter, new Rectangle(x, y, 0, 0));
        }
    }

    private void addNewLabelPosition(List<Rectangle> labelConstraintsToCreate, WorkflowLabel label) {
        if (!viewer.getContextMenu().isDirty()) {
            findFreeSpotForLabel(editor.getMouseX(), editor.getMouseY(), label.getSize(),
                labelConstraintsToCreate);
        } else if (viewer.getContextMenu().isDirty()) {
            findFreeSpotForLabel(label.getX(), label.getY(), label.getSize(), labelConstraintsToCreate);
        }
    }

    private void findFreeSpotForLabel(int x, int y, Dimension labelSize, List<Rectangle> labelConstraintsToCreate) {
        if (!isLabelPositionValid(x, y)) {
            findFreeSpotForLabel(x + offset, y + offset, labelSize, labelConstraintsToCreate);
        } else {
            for (Rectangle rectangle : labelConstraintsToCreate) {
                if (rectangle.equals(x, y, labelSize.width, labelSize.height)) {
                    x += offset * 2;
                    y += offset * 2;
                }
            }
            if (isLabelPositionValid(x, y)) {
                labelConstraintsToCreate.add(labelConstraintPositionCounter,
                    new Rectangle(x, y, labelSize.width, labelSize.height));
            } else {
                findFreeSpotForLabel(x + offset, y + offset, labelSize, labelConstraintsToCreate);
            }
        }
    }

    private boolean isNodePositionValid(int x, int y, ComponentSize componentSize) {
        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        for (WorkflowNode node : model.getWorkflowNodes()) {
            if (node.getComponentDescription().getSize() != null && componentSize != null) {
                if (node.getComponentDescription().getSize().equals(ComponentSize.MEDIUM) && !componentSize.equals(ComponentSize.MEDIUM)) {
                    componentSizeOffset = WorkflowNodePart.WORKFLOW_NODE_WIDTH;
                } else {
                    componentSizeOffset = WorkflowNodePart.SMALL_WORKFLOW_NODE_WIDTH / 2;
                }
            } else {
                // fallback if component description has no size yet or component size is null
                componentSizeOffset = WorkflowNodePart.WORKFLOW_NODE_WIDTH;
            }
            if (x >= node.getX() && x <= node.getX() + componentSizeOffset && y >= node.getY() && y <= node.getY() + componentSizeOffset) {
                return false;
            }
        }
        return true;
    }

    private boolean isLabelPositionValid(int x, int y) {
        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        for (WorkflowLabel label : model.getWorkflowLabels()) {
            if (x >= label.getX() && x <= label.getX() + offset && y >= label.getY()
                && y <= label.getY() + offset) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List parseInputStream(JsonParser jp) throws IOException, JsonParseException, ParseException {
        List combinedList = new ArrayList();
        WorkflowDescriptionPersistenceHandler descriptionHandler = new WorkflowDescriptionPersistenceHandler();
        EditorEditPartFactory factory = new EditorEditPartFactory();
        Map<String, WorkflowNode> parseNodes = null;
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            jp.nextToken();
            String fieldname = jp.getCurrentName();
            if (WorkflowDescriptionPersistenceHandler.NODES.equals(fieldname)) {
                jp.nextToken();
                // parsing content to WorkflowNode
                parseNodes = descriptionHandler.parseNodes(jp, null);
                for (String key : parseNodes.keySet()) {
                    // creating WorkflowNodePart
                    EditPart createEditPart = factory.createEditPart(null, parseNodes.get(key));
                    if (createEditPart instanceof WorkflowNodePart) {
                        combinedList.add(createEditPart);
                    }
                }
            } else if (WorkflowDescriptionPersistenceHandler.CONNECTIONS.equals(fieldname)) {
                // parsing content to Connection
                Set<Connection> parseConnections = descriptionHandler.parseConnections(jp, parseNodes);
                combinedList.addAll(parseConnections);
            } else if (WorkflowDescriptionPersistenceHandler.BENDPOINTS.equals(fieldname)) {
                // parsing content of bendpoints
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    while (jp.nextToken() != JsonToken.END_OBJECT) {

                        WorkflowNode output = null;
                        WorkflowNode input = null;
                        List<Location> bendpoints = new ArrayList<>();
                        String bendpointListString = null;

                        String bendpointField = jp.getCurrentName();
                        jp.nextToken(); // move to value
                        if (WorkflowDescriptionPersistenceHandler.SOURCE.equals(bendpointField)) {
                            output = getWorkflowNodeByName(combinedList, jp.getText());
                        } else {
                            throw new ParseException("No source definition.", jp.getCurrentLocation().getLineNr());
                        }
                        jp.nextToken();
                        bendpointField = jp.getCurrentName();
                        jp.nextToken(); // move to value
                        if (WorkflowDescriptionPersistenceHandler.TARGET.equals(bendpointField)) {
                            input = getWorkflowNodeByName(combinedList, jp.getText());
                        } else {
                            throw new ParseException("No target definition.", jp.getCurrentLocation().getLineNr());
                        }
                        jp.nextToken();
                        bendpointField = jp.getCurrentName();
                        jp.nextToken(); // move to value
                        if (WorkflowDescriptionPersistenceHandler.COORDINATES.equals(bendpointField)) {
                            bendpointListString = jp.getText();
                        } else {
                            throw new ParseException("No input definition.", jp.getCurrentLocation().getLineNr());
                        }

                        if (bendpointListString != null) {
                            for (String bendpointString : bendpointListString
                                .split(WorkflowDescriptionPersistenceHandler.BENDPOINT_SEPARATOR)) {
                                Location bendpoint =
                                    new Location(Integer.parseInt(bendpointString
                                        .split(WorkflowDescriptionPersistenceHandler.BENDPOINT_COORDINATE_SEPARATOR)[0]),
                                        Integer.parseInt(bendpointString
                                            .split(WorkflowDescriptionPersistenceHandler.BENDPOINT_COORDINATE_SEPARATOR)[1]));
                                bendpoints.add(bendpoint);
                            }

                            for (Object connectionObject : combinedList) {
                                if (connectionObject instanceof Connection) {
                                    Connection connection = (Connection) connectionObject;
                                    if ((connection.getTargetNode().getIdentifier().equals(input.getIdentifier())
                                        && connection.getSourceNode().getIdentifier().equals(output.getIdentifier()))) {
                                        connection.setBendpoints(bendpoints);
                                    } else if (connection.getTargetNode().getIdentifier().equals(output.getIdentifier())
                                        && connection.getSourceNode().getIdentifier().equals(input.getIdentifier())) {
                                        List<Location> invertedBendpointsToAdd = new ArrayList<>();
                                        for (Location l : bendpoints) {
                                            invertedBendpointsToAdd.add(0, l);
                                        }
                                        connection.setBendpoints(invertedBendpointsToAdd);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (WorkflowDescriptionPersistenceHandler.LABELS.equals(fieldname)) {
                if (parseNodes == null) {
                    jp.nextToken();
                }
                // parsing content to WorkflowLabel
                Set<WorkflowLabel> parseLabel = descriptionHandler.parseLabels(jp);
                combinedList.addAll(parseLabel);
            }
        }
        return combinedList;
    }

    private WorkflowNode getWorkflowNodeByName(List<?> combinedList, String nodeId) {
        WorkflowNode matchingNode = null;
        for (Object object : combinedList) {
            if (object instanceof WorkflowNodePart) {
                WorkflowNode node = (WorkflowNode) ((WorkflowNodePart) object).getModel();
                if (node.getIdentifier().equals(nodeId)) {
                    matchingNode = node;
                }
            }
        }
        return matchingNode;
    }

    /**
     * Copies {@link WorkflowLabel} but doesn't clone it. The identifier needs to remain unique for every label.
     * 
     * @param origin {@link WorkflowLabel} to copy.
     * @return copied {@link WorkflowLabel}
     */
    private WorkflowLabel createCopiedWorkflowLabel(WorkflowLabel origin) {
        WorkflowLabel copied = new WorkflowLabel(origin.getText());
        copied.setAlpha(origin.getAlphaDisplay());
        copied.setColorBackground(origin.getColorBackground());
        copied.setColorText(origin.getColorText());
        copied.setSize(origin.getSize());
        copied.setAlignmentType(origin.getAlignmentType());
        copied.setHasBorder(origin.hasBorder());
        copied.setTextSize(origin.getTextSize());

        // copied.setLocation(origin.getX(), origin.getY());
        return copied;
    }

    /**
     * Copies {@link ComponentDescription} but doesn't clone it. It is needed that identifiers of endpoints are not part of the new
     * {@link ComponentDescription}. It it would, the endpoints of the copied {@link ComponentDescription} will be related to existing
     * connections as well.
     * 
     * @param origin {@link ComponentDescription} to copy.
     * @return copied {@link ComponentDescription}
     */
    private ComponentDescription copyComponentDescription(ComponentDescription origin) {
        ComponentDescription copied = new ComponentDescription(origin.getComponentInstallation());
        copied.setIsNodeTransient(origin.getIsNodeTransient());
        for (EndpointDescription ep : origin.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
            copied.getInputDescriptionsManager().addDynamicEndpointDescription(ep.getDynamicEndpointIdentifier(),
                ep.getName(), ep.getDataType(), ep.getMetaData(), false);
        }
        for (EndpointDescription ep : origin.getInputDescriptionsManager().getStaticEndpointDescriptions()) {
            copied.getInputDescriptionsManager().editStaticEndpointDescription(ep.getName(), ep.getDataType(), ep.getMetaData(), false);
        }
        for (EndpointDescription ep : origin.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
            copied.getOutputDescriptionsManager().addDynamicEndpointDescription(ep.getDynamicEndpointIdentifier(),
                ep.getName(), ep.getDataType(), ep.getMetaData(), false);
        }
        for (EndpointDescription ep : origin.getOutputDescriptionsManager().getStaticEndpointDescriptions()) {
            copied.getOutputDescriptionsManager().editStaticEndpointDescription(ep.getName(), ep.getDataType(), ep.getMetaData(), false);
        }
        copied.getConfigurationDescription().setConfiguration(new HashMap<>(origin.getConfigurationDescription().getConfiguration()));
        return copied;
    }

}
