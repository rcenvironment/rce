/*
 * Copyright 2006-2020 DLR, Germany
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
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.gui.workflow.editor.handlers.AbstractWorkflowPartSendHandler;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;

/**
 * 
 * Command that sends label forward or backward in the editor. Don't supply any labels that are already in the foreground (background).
 *
 * @author Jascha Riedel
 */
public class WorkflowLabelSendLayerwiseCommand extends Command {

    private static final int MINUS_ONE = -1;

    private final WorkflowDescription workflowDescription;

    private final List<WorkflowLabel> selectedLabels;

    private final AbstractWorkflowPartSendHandler.SendType sendType;

    private Map<WorkflowLabel, Integer> previousState = new HashMap<WorkflowLabel, Integer>();

    public WorkflowLabelSendLayerwiseCommand(WorkflowDescription workflowDescription,
        List<WorkflowLabelPart> cleanSelectedLabelParts,
        AbstractWorkflowPartSendHandler.SendType sendType) {

        this.workflowDescription = workflowDescription;
        selectedLabels = new ArrayList<WorkflowLabel>();
        for (WorkflowLabelPart part : cleanSelectedLabelParts) {
            selectedLabels.add((WorkflowLabel) part.getModel());
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

        int[] zIndexList = getzIndexList(workflowDescription.getWorkflowLabels().size());

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

        workflowDescription.firePropertyChange(WorkflowDescription.PROPERTY_LABEL);

    }

    @Override
    public void undo() {
        for (WorkflowLabel label : workflowDescription.getWorkflowLabels()) {
            label.setZIndex(previousState.get(label));
        }

        workflowDescription.firePropertyChange(WorkflowDescription.PROPERTY_LABEL);

    }

    private void savePreviousState() {
        for (WorkflowLabel label : workflowDescription.getWorkflowLabels()) {
            previousState.put(label, label.getZIndex());
        }
    }

    private void orderSelectedLabels() {
        Collections.sort(selectedLabels, new Comparator<WorkflowLabel>() {

            @Override
            public int compare(WorkflowLabel arg0, WorkflowLabel arg1) {
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
        for (int i = 0; i < selectedLabels.size(); i++) {
            if (selectedLabels.get(i).getZIndex() == i) {
                continue;
            }
            int z = selectedLabels.get(i).getZIndex();
            int tempZ = indexList[z];
            indexList[z] = indexList[z - 1];
            indexList[z - 1] = tempZ;
        }
    }

    private void sendSelectedArrayIndicesOneLayerForward(int[] indexList) {
        for (int i = selectedLabels.size() - 1; i >= 0; i--) {
            WorkflowLabel selectedLabel = selectedLabels.get(i);
            int z = selectedLabel.getZIndex();
            if (z == workflowDescription.getWorkflowLabels().size() - 1 - (selectedLabels.size() - 1 - i)) {
                continue;
            }
            if (z < workflowDescription.getWorkflowLabels().size() - 1) {
                int tempZ = indexList[z];
                indexList[z] = indexList[z + 1];
                indexList[z + 1] = tempZ;
            }
        }
    }

    private void sendSelectedArrayIndicesToBackground(int[] indexList) {
        for (int i = 0; i < selectedLabels.size(); i++) {
            for (int j = selectedLabels.get(i).getZIndex(); j > i; j--) {
                int tempZ = indexList[j];
                indexList[j] = indexList[j - 1];
                indexList[j - 1] = tempZ;
            }
        }
    }

    private void sendSelectedArrayIndicesToForeground(int[] indexList) {
        for (int i = selectedLabels.size() - 1; i >= 0; i--) {
            for (int j = selectedLabels.get(i).getZIndex(); j < indexList.length - selectedLabels.size() + i; j++) {
                int tempZ = indexList[j];
                indexList[j] = indexList[j + 1];
                indexList[j + 1] = tempZ;
            }
        }
    }

    private void setNewzIndicesFromIndexList(int[] indexList) {
        Map<WorkflowLabel, Integer> assignMap = new HashMap<>();
        for (int i = 0; i < indexList.length; i++) {
            for (WorkflowLabel label : workflowDescription.getWorkflowLabels()) {
                if (label.getZIndex() == indexList[i]) {
                    assignMap.put(label, i);
                    break;
                }
            }
        }
        for (WorkflowLabel label : assignMap.keySet()) {
            label.setZIndex(assignMap.get(label));
        }
    }

}
