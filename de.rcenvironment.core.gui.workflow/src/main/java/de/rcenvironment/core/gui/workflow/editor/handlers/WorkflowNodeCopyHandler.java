/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * Handle copy part of copy&paste for {@link WorkflowNode}s, {@link Connection}s
 * and {@link WorkflowLabel}s.
 *
 * @author Doreen Seider
 * @author Oliver Seebach
 * @author Marc Stammerjohann
 */
// TODO fix class name, it is not longer only a copy handler for WorkflowNodes
public class WorkflowNodeCopyHandler extends AbstractWorkflowNodeEditHandler {

    private WorkflowDescriptionPersistenceHandler descriptionHandler = new WorkflowDescriptionPersistenceHandler();

    @Override
    void edit() {
        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        List<?> selection = viewer.getSelectedEditParts();
        List<WorkflowNodePart> nodes = new ArrayList<WorkflowNodePart>();
        List<Connection> connections = new ArrayList<Connection>();
        List<WorkflowLabelPart> labels = new ArrayList<WorkflowLabelPart>();

        // check whether there are connections between the selected nodes; if yes, add
        // them to clipboard, too.
        for (Object selectedObject : selection) {
            if (selectedObject instanceof WorkflowNodePart) {
                WorkflowNodePart workflowNodePart = (WorkflowNodePart) selectedObject;
                nodes.add(workflowNodePart);
                WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
                for (Object comparisonObject : selection) {
                    if (comparisonObject instanceof WorkflowNodePart) {
                        WorkflowNodePart comparisonWorkflowNodePart = ((WorkflowNodePart) comparisonObject);
                        WorkflowNode comparisonWorkflowNode = (WorkflowNode) comparisonWorkflowNodePart.getModel();
                        for (Connection connection : model.getConnections()) {
                            if ((connection.getSourceNode().getIdentifierAsObject()
                                    .equals(workflowNode.getIdentifierAsObject())
                                    && connection.getTargetNode().getIdentifierAsObject()
                                            .equals(comparisonWorkflowNode.getIdentifierAsObject()))) {
                                connections.add(connection);
                            }
                        }
                    }
                }
            } else if (selectedObject instanceof WorkflowLabelPart) {
                WorkflowLabelPart workflowLabelPart = (WorkflowLabelPart) selectedObject;
                labels.add(workflowLabelPart);
            }
        }

        Collections.sort(labels, new WorkflowLabelpartComparator());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonFactory f = new JsonFactory();
            JsonGenerator generator = null;
            generator = f.createGenerator(outputStream, JsonEncoding.UTF8);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            generator.writeStartObject();
            if (!nodes.isEmpty()) {
                writeComponent(generator, nodes);
            }
            if (!connections.isEmpty()) {
                writeConnection(generator, connections);
            }
            if (!labels.isEmpty()) {
                writeLabel(generator, labels);
            }
            if (!connections.isEmpty()) {
                writeBendpoints(generator, connections);
            }
            generator.writeEndObject();
            generator.close();
            ClipboardHelper.setContent(outputStream.toString());
        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when writing components to JSON: " + e.getMessage());
        }
    }

    private void writeComponent(JsonGenerator generator, List<WorkflowNodePart> nodes)
            throws JsonGenerationException, IOException {
        generator.writeArrayFieldStart(WorkflowDescriptionPersistenceHandler.NODES);
        for (WorkflowNodePart workflowNodePart : nodes) {
            WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
            descriptionHandler.writeWorkflowNode(generator, workflowNode);
        }
        generator.writeEndArray();
    }

    private void writeConnection(JsonGenerator generator, List<Connection> connections)
            throws JsonGenerationException, IOException {
        generator.writeArrayFieldStart(WorkflowDescriptionPersistenceHandler.CONNECTIONS);
        for (Connection connection : connections) {
            descriptionHandler.writeConnection(generator, connection);
        }
        generator.writeEndArray();
    }

    private void writeBendpoints(JsonGenerator generator, List<Connection> connections)
            throws JsonGenerationException, IOException {
        generator.writeArrayFieldStart(WorkflowDescriptionPersistenceHandler.BENDPOINTS);
        Map<String, String> uniqueConnectionBendpointMapping = descriptionHandler
                .calculateUniqueBendpointList(connections);
        descriptionHandler.writeBendpoints(generator, uniqueConnectionBendpointMapping);
        generator.writeEndArray();
    }

    private void writeLabel(JsonGenerator generator, List<WorkflowLabelPart> labels)
            throws JsonGenerationException, IOException {
        generator.writeArrayFieldStart(WorkflowDescriptionPersistenceHandler.LABELS);
        for (WorkflowLabelPart workflowLabelPart : labels) {
            WorkflowLabel workflowLabel = (WorkflowLabel) workflowLabelPart.getModel();
            descriptionHandler.writeLabel(generator, workflowLabel);
        }
        generator.writeEndArray();
    }

    /**
     * Comparing two {@link WorkflowLabelPart}s, depending on the Y-Position of the
     * {@link WorkflowLabel}.
     *
     * @author Marc Stammerjohann
     */
    private class WorkflowLabelpartComparator implements Comparator<WorkflowLabelPart> {

        @Override
        public int compare(WorkflowLabelPart part1, WorkflowLabelPart part2) {
            int y1 = getYPosition(part1);
            int y2 = getYPosition(part2);

            if (y1 == y2) {
                return 0;
            } else if (y1 < y2) {
                return 0 - 1;
            } else {
                return 1;
            }
        }

        private int getYPosition(WorkflowLabelPart part) {
            int y = 0;
            Object model = part.getModel();
            if (model instanceof WorkflowLabel) {
                WorkflowLabel label = (WorkflowLabel) model;
                y = label.getY();
            }
            return y;
        }

    }
}
