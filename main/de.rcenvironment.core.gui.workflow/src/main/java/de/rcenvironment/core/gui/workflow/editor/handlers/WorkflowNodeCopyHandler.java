/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;


/**
 * Handle copy part of copy&paste for nodes, labels and connections.
 *
 * @author Doreen Seider
 * @author Oliver Seebach
 */
public class WorkflowNodeCopyHandler extends AbstractWorkflowNodeEditHandler {

    private WorkflowDescriptionPersistenceHandler descriptionHandler = new WorkflowDescriptionPersistenceHandler();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    void edit() {
        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        List selection = viewer.getSelectedEditParts();
        List connections = new ArrayList();
        // check whether there are connections between the selected nodes; if yes, add them to clipboard, too.
        for (Object selectedObject : selection){
            if (selectedObject instanceof WorkflowNodePart){
                WorkflowNodePart workflowNodePart = ((WorkflowNodePart) selectedObject);
                WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
                for (Object comparisonObject : selection){
                    if (comparisonObject instanceof WorkflowNodePart){
                        WorkflowNodePart comparisonWorkflowNodePart = ((WorkflowNodePart) comparisonObject);
                        WorkflowNode comparisonWorkflowNode = (WorkflowNode) comparisonWorkflowNodePart.getModel();
                        for (Connection connection : model.getConnections()){
                            if ((connection.getSourceNode().getIdentifier().equals(workflowNode.getIdentifier()) 
                                && connection.getTargetNode().getIdentifier().equals(comparisonWorkflowNode.getIdentifier()))){
                                connections.add(connection);
                            }
                        }
                    }
                }
            }
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonFactory f = new JsonFactory();
            JsonGenerator generator = null;
            generator = f.createJsonGenerator(outputStream, JsonEncoding.UTF8);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            generator.writeStartObject();
            if (!selection.isEmpty()) {
                writeComponent(generator, selection);
            }
            if (!connections.isEmpty()) {
                writeConnection(generator, connections);
            }
            generator.writeEndObject();
            generator.close();
            StringSelection stringSelection = new StringSelection(outputStream.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when writing components to JSON: " + e.getMessage());
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private void writeComponent(JsonGenerator generator, List selection) throws JsonGenerationException, IOException {
        generator.writeArrayFieldStart(WorkflowDescriptionPersistenceHandler.NODES);
        for (Object node : selection) {
            if (node instanceof WorkflowNodePart) {
                WorkflowNodePart workflowNodePart = (WorkflowNodePart) node;
                WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
                descriptionHandler.writeWorkflowNode(generator, workflowNode);
            }
        }
        generator.writeEndArray();
    }

    @SuppressWarnings({ "rawtypes" })
    private void writeConnection(JsonGenerator generator, List connections) throws JsonGenerationException, IOException {
        generator.writeArrayFieldStart(WorkflowDescriptionPersistenceHandler.CONNECTIONS);
        for (Object ob : connections) {
            if (ob instanceof Connection) {
                Connection connection = (Connection) ob;
                descriptionHandler.writeConnection(generator, connection);
            }
        }
        generator.writeEndArray();
    }
}
