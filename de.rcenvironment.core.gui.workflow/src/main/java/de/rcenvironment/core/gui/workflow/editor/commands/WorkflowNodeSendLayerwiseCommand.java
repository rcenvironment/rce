/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.handlers.AbstractWorkflowPartSendHandler;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * 
 * Command that sends nodes forward or backward in the editor. Don't supply any labels that are already in the foreground (background).
 *
 * @author Jascha Riedel
 */
public class WorkflowNodeSendLayerwiseCommand extends Command {

    /** Option Constant. */
    public static final int SEND_BACK = 0;

    /** Option Constant. */
    public static final int SEND_FORWARD = 1;

    /** Option Constant. */
    public static final int SEND_TO_BACKGROUND = 2;

    /** Option Constant. */
    public static final int SEND_TO_FOREGROUND = 4;

    private static final int MINUS_ONE = -1;

    private final WorkflowDescription workflowDescription;

    private final List<WorkflowNode> selectedNodes;

    private final AbstractWorkflowPartSendHandler.SendType sendType;

    private Map<WorkflowNode, Integer> previousState = new HashMap<WorkflowNode, Integer>();

    public WorkflowNodeSendLayerwiseCommand(WorkflowDescription workflowDescription,
        List<WorkflowNodePart> cleanSelectedNodeParts,
        AbstractWorkflowPartSendHandler.SendType sendType) {

        this.workflowDescription = workflowDescription;
        selectedNodes = new ArrayList<WorkflowNode>();
        for (WorkflowNodePart part : cleanSelectedNodeParts) {
            selectedNodes.add((WorkflowNode) part.getModel());
        }
        this.sendType = sendType;
    }

    @Override
    public void execute() {
        savePreviousState();
        redo();
    }

    @Override
    public void redo() {

        orderSelectedLabels();

        int[] zIndexList = getzIndexList(workflowDescription.getWorkflowNodes().size());

        switch (sendType) {
        case SEND_BACK:
            sendSelectedArrayIndicesOneLayerBack(zIndexList);
            break;
        case SEND_FORWARD:
            sendSelectedArrayIndicesOneLayerForward(zIndexList);
            break;
        case SEND_TO_BACKGROUND:
            sendSelectedArrayIndicesToBackground(zIndexList);
            break;
        case SEND_TO_FOREGROUND:
            sendSelectedArrayIndicesToForeground(zIndexList);
        default:
            break;
        }

        setNewzIndicesFromIndexList(zIndexList);

        workflowDescription.firePropertyChange(WorkflowDescription.PROPERTY_NODES);

    }

    @Override
    public void undo() {
        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            node.setZIndex(previousState.get(node));
        }

        workflowDescription.firePropertyChange(WorkflowDescription.PROPERTY_NODES);

    }

    private void savePreviousState() {
        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            previousState.put(node, node.getZIndex());
        }
    }

    private void orderSelectedLabels() {
        Collections.sort(selectedNodes, new Comparator<WorkflowNode>() {

            @Override
            public int compare(WorkflowNode arg0, WorkflowNode arg1) {
                if (arg0.getZIndex() < arg1.getZIndex()) {
                    return MINUS_ONE;
                } else if (arg0.getZIndex() > arg1.getZIndex()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    private int[] getzIndexList(int size) {
        int[] returnList = new int[size];
        for (int i = 0; i < size; i++) {
            returnList[i] = i;
        }
        return returnList;
    }

    private void sendSelectedArrayIndicesOneLayerBack(int[] indexList) {
        for (int i = 0; i < selectedNodes.size(); i++) {
            if (selectedNodes.get(i).getZIndex() == i) {
                continue;
            }
            int z = selectedNodes.get(i).getZIndex();
            int tempZ = indexList[z];
            indexList[z] = indexList[z - 1];
            indexList[z - 1] = tempZ;
        }
    }

    private void sendSelectedArrayIndicesOneLayerForward(int[] indexList) {
        for (int i = selectedNodes.size() - 1; i >= 0; i--) {
            WorkflowNode selectedNode = selectedNodes.get(i);
            int z = selectedNode.getZIndex();
            if (z < workflowDescription.getWorkflowNodes().size() - 1) {
                int tempZ = indexList[z];
                indexList[z] = indexList[z + 1];
                indexList[z + 1] = tempZ;
            }
        }
    }

    private void sendSelectedArrayIndicesToBackground(int[] indexList) {
        for (int i = 0; i < selectedNodes.size(); i++) {
            for (int j = selectedNodes.get(i).getZIndex(); j > i; j--) {
                int tempZ = indexList[j];
                indexList[j] = indexList[j - 1];
                indexList[j - 1] = tempZ;
            }
        }
    }

    private void sendSelectedArrayIndicesToForeground(int[] indexList) {
        for (int i = selectedNodes.size() - 1; i >= 0; i--) {
            for (int j = selectedNodes.get(i).getZIndex(); j < indexList.length - selectedNodes.size() + i; j++) {
                int tempZ = indexList[j];
                indexList[j] = indexList[j + 1];
                indexList[j + 1] = tempZ;
            }
        }
    }

    private void setNewzIndicesFromIndexList(int[] indexList) {
        Map<WorkflowNode, Integer> assignMap = new HashMap<>();
        for (int i = 0; i < indexList.length; i++) {
            for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
                if (node.getZIndex() == indexList[i]) {
                    assignMap.put(node, i);
                    break;
                }
            }
        }
        for (WorkflowNode node : assignMap.keySet()) {
            node.setZIndex(assignMap.get(node));
        }
    }

}
