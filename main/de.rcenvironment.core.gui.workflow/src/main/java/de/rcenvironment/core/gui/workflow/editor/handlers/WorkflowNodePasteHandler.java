/*
 * Copyright (C) 2006-2014 DLR, Germany
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
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;
import de.rcenvironment.core.gui.workflow.parts.EditorEditPartFactory;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * Pastes a workflow node.
 * 
 * @author Doreen Seider
 * @author Oliver Seebach
 * @author Sascha Zur
 */
public class WorkflowNodePasteHandler extends AbstractWorkflowNodeEditHandler {

    private final Map<String, String> nameMapping = new HashMap<String, String>();

    // mapping between old identifier and new workflow node !
    private final Map<WorkflowNode, WorkflowNode> nodeMapping = new HashMap<WorkflowNode, WorkflowNode>();

    private final Map<EndpointDescription, EndpointDescription> endpointMapping = new HashMap<EndpointDescription, EndpointDescription>();

    private final Map<String, EndpointDescription> endpointIDMapping = new HashMap<String, EndpointDescription>();

    private int nodeConstraintPositionCounter = 0;

    private int labelConstraintPositionCounter = 0;

    @Override
    void edit() {
        List<WorkflowNode> workflowNodesToCreate = new LinkedList<>();
        List<WorkflowNodePart> pastedWorkflowNodeParts = new LinkedList<>();
        List<WorkflowLabel> workflowLabelsToCreate = new LinkedList<>();
        List<Connection> connectionsToCreate = new LinkedList<>();
        List<Rectangle> nodeConstraintsToCreate = new LinkedList<>();
        List<Rectangle> labelConstraintsToCreate = new LinkedList<>();
        final int offset = 20;
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
                    workflowNodesToCreate.add(newNode);
                    nodeConstraintsToCreate.add(nodeConstraintPositionCounter, new Rectangle(node.getX() + offset, node.getY() + offset, 0,
                        0));
                    nodeConstraintPositionCounter++;

                    // fill mapping of original and copied nodes to create connections lateron
                    nameMapping.put(node.getIdentifier(), newNode.getIdentifier());
                    nodeMapping.put(node, newNode);

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
                if (partToPaste instanceof WorkflowLabelPart) {
                    WorkflowLabel label = (WorkflowLabel) ((WorkflowLabelPart) partToPaste).getModel();
                    WorkflowLabel newLabel = createCopiedWorkflowLabel(label);
                    newLabel.setLocation(label.getX() + offset, label.getY() + offset);
                    workflowLabelsToCreate.add(newLabel);
                    labelConstraintsToCreate.add(labelConstraintPositionCounter, new Rectangle(label.getX() + offset,
                        label.getY() + offset,
                        label.getSize().width, label.getSize().height));
                    labelConstraintPositionCounter++;
                }
                if (partToPaste instanceof Connection) {
                    Connection oldConnection = (Connection) partToPaste;
                    WorkflowNode source = nodeMapping.get(oldConnection.getSourceNode());
                    WorkflowNode target = nodeMapping.get(oldConnection.getTargetNode());
                    EndpointDescription sourceOutput = endpointIDMapping.get(oldConnection.getOutput().getIdentifier());
                    EndpointDescription targetInput = endpointIDMapping.get(oldConnection.getInput().getIdentifier());
                    Connection newConnection = new Connection(source, sourceOutput, target, targetInput);
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
        nameMapping.clear();
        nodeConstraintPositionCounter = 0;
        
        // move selection from original components to pasted ones
        viewer.deselectAll();
        for (Object editPartObject : viewer.getContents().getChildren()){
            if (editPartObject instanceof WorkflowNodePart){
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
            return parseInputStream(jp);
        }
        return null;
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
                        combinedList.add((WorkflowNodePart) createEditPart);
                    }
                }
            } else if (WorkflowDescriptionPersistenceHandler.CONNECTIONS.equals(fieldname)) {
                // parsing content to Connection
                Set<Connection> parseConnections = descriptionHandler.parseConnections(jp, parseNodes);
                for (Connection connection : parseConnections) {
                    combinedList.add(connection);
                }
            }

        }
        return combinedList;
    }

    /**
     * Copies {@link WorkflowLabel} but doesn't clone it. The identifier needs to remain unique for
     * every label.
     * 
     * @param origin {@link WorkflowLabel} to copy.
     * @return copied {@link WorkflowLabel}
     */
    private WorkflowLabel createCopiedWorkflowLabel(WorkflowLabel origin) {
        WorkflowLabel copied = new WorkflowLabel(origin.getText());
        copied.setAlpha(origin.getAlpha());
        copied.setColorBackground(origin.getColorBackground());
        copied.setColorText(origin.getColorText());
        copied.setSize(origin.getSize());
        // copied.setLocation(origin.getX(), origin.getY());
        return copied;
    }

    /**
     * Copies {@link ComponentDescription} but doesn't clone it. It is needed that identifiers of
     * endpoints are not part of the new {@link ComponentDescription}. It it would, the endpoints of
     * the copied {@link ComponentDescription} will be related to existing connections as well.
     * 
     * @param origin {@link ComponentDescription} to copy.
     * @return copied {@link ComponentDescription}
     */
    private ComponentDescription copyComponentDescription(ComponentDescription origin) {
        ComponentDescription copied = new ComponentDescription(origin.getComponentInstallation());
        copied.setIsNodeTransient(origin.getIsNodeTransient());
        for (EndpointDescription ep : origin.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
            copied.getInputDescriptionsManager().addDynamicEndpointDescription(ep.getDynamicEndpointIdentifier(),
                ep.getName(), ep.getDataType(), ep.getMetaData());
        }
        for (EndpointDescription ep : origin.getInputDescriptionsManager().getStaticEndpointDescriptions()) {
            copied.getInputDescriptionsManager().editStaticEndpointDescription(ep.getName(), ep.getDataType(), ep.getMetaData());
        }
        for (EndpointDescription ep : origin.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
            copied.getOutputDescriptionsManager().addDynamicEndpointDescription(ep.getDynamicEndpointIdentifier(),
                ep.getName(), ep.getDataType(), ep.getMetaData());
        }
        for (EndpointDescription ep : origin.getOutputDescriptionsManager().getStaticEndpointDescriptions()) {
            copied.getOutputDescriptionsManager().editStaticEndpointDescription(ep.getName(), ep.getDataType(), ep.getMetaData());
        }
        copied.getConfigurationDescription().setConfiguration(new HashMap<>(origin.getConfigurationDescription().getConfiguration()));

        return copied;
    }
}
