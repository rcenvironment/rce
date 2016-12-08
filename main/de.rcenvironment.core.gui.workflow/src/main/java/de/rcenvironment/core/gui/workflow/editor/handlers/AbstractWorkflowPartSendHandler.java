/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowLabelSendLayerwiseCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeSendLayerwiseCommand;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * 
 * The common base class for a label "send" handler.
 *
 * @author Jascha Riedel
 */
public abstract class AbstractWorkflowPartSendHandler extends AbstractHandler {

    protected GraphicalViewer viewer;

    protected CommandStack commandStack;

    protected WorkflowDescription workflowDescription;

    /**
     * 
     * Enum to define where to send the workflow part.
     *
     * @author Jascha
     */
    public enum SendType {
        /** One layer back. */
        SEND_BACK,
        /** One layer forward. */
        SEND_FORWARD,
        /** All the way back. */
        SEND_TO_BACKGROUND,
        /** All the way forward. */
        SEND_TO_FOREGROUND;
    }

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        final IWorkbenchPart activePart = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof WorkflowEditor) {
            viewer = ((WorkflowEditor) activePart).getViewer();
            commandStack = (CommandStack) ((WorkflowEditor) activePart).getAdapter(CommandStack.class);
            workflowDescription = (WorkflowDescription) viewer.getContents().getModel();
            edit();

        }
        return null;
    }

    abstract void edit();

    protected void send(SendType sendType) {
        if (viewer.getSelectedEditParts().get(0) instanceof WorkflowLabelPart) {
            @SuppressWarnings("unchecked") List<WorkflowLabelPart> labelParts = viewer.getSelectedEditParts();
            switch (sendType) {
            case SEND_BACK:
            case SEND_TO_BACKGROUND:
                if (onlyLabelsInBackgroundSelected(labelParts)) {
                    return;
                }
                break;
            case SEND_FORWARD:
            case SEND_TO_FOREGROUND:
                if (onlyLabelsInForegroundSelected(labelParts)) {
                    return;
                }
                break;
            default:
                return;
            }
            commandStack.execute(new WorkflowLabelSendLayerwiseCommand((WorkflowDescription) viewer.getContents().getModel(),
                labelParts, sendType));
        } else if (viewer.getSelectedEditParts().get(0) instanceof WorkflowNodePart) {
            @SuppressWarnings("unchecked") List<WorkflowNodePart> nodeParts = viewer.getSelectedEditParts();
            switch (sendType) {
            case SEND_BACK:
            case SEND_TO_BACKGROUND:
                if (onlyNodesInBackgroundSelected(nodeParts)) {
                    return;
                }
                break;
            case SEND_FORWARD:
            case SEND_TO_FOREGROUND:
                if (onlyNodesInForegroundSelected(nodeParts)) {
                    return;
                }
                break;
            default:
                return;
            }

            commandStack.execute(new WorkflowNodeSendLayerwiseCommand((WorkflowDescription) viewer.getContents().getModel(),
                nodeParts, sendType));
        }

    }

    protected boolean onlyLabelsInForegroundSelected(List<WorkflowLabelPart> labels) {
        List<WorkflowLabelPart> returnList = new ArrayList<WorkflowLabelPart>();
        for (WorkflowLabelPart labelPart : labels) {
            returnList.add(labelPart);
        }
        int topIndex = workflowDescription.getWorkflowLabels().size() - 1;
        while (true) {
            int currentIndex = topIndex;
            for (WorkflowLabelPart labelPart : returnList) {
                if (((WorkflowLabel) labelPart.getModel()).getZIndex() == topIndex) {
                    returnList.remove(labelPart);
                    topIndex--;
                    break;
                }
            }
            if (currentIndex == topIndex) {
                break;
            }
        }
        return returnList.isEmpty();
    }

    protected boolean onlyLabelsInBackgroundSelected(List<WorkflowLabelPart> labels) {
        List<WorkflowLabelPart> returnList = new ArrayList<WorkflowLabelPart>();
        for (WorkflowLabelPart labelPart : labels) {
            returnList.add(labelPart);
        }
        int bottomIndex = 0;
        while (true) {
            int currentIndex = bottomIndex;
            for (WorkflowLabelPart labelPart : returnList) {
                if (((WorkflowLabel) labelPart.getModel()).getZIndex() == bottomIndex) {
                    returnList.remove(labelPart);
                    bottomIndex++;
                    break;
                }
            }
            if (currentIndex == bottomIndex) {
                break;
            }
        }
        return returnList.isEmpty();
    }

    protected boolean onlyNodesInForegroundSelected(List<WorkflowNodePart> nodes) {
        List<WorkflowNodePart> returnList = new ArrayList<WorkflowNodePart>();
        for (WorkflowNodePart nodePart : nodes) {
            returnList.add(nodePart);
        }
        int topIndex = workflowDescription.getWorkflowNodes().size() - 1;
        while (true) {
            int currentIndex = topIndex;
            for (WorkflowNodePart nodePart : returnList) {
                if (((WorkflowNode) nodePart.getModel()).getZIndex() == topIndex) {
                    returnList.remove(nodePart);
                    topIndex--;
                    break;
                }
            }
            if (currentIndex == topIndex) {
                break;
            }
        }
        return returnList.isEmpty();
    }

    protected boolean onlyNodesInBackgroundSelected(List<WorkflowNodePart> nodes) {
        List<WorkflowNodePart> returnList = new ArrayList<WorkflowNodePart>();
        for (WorkflowNodePart nodePart : nodes) {
            returnList.add(nodePart);
        }
        int bottomIndex = 0;
        while (true) {
            int currentIndex = bottomIndex;
            for (WorkflowNodePart nodePart : returnList) {
                if (((WorkflowNode) nodePart.getModel()).getZIndex() == bottomIndex) {
                    returnList.remove(nodePart);
                    bottomIndex++;
                    break;
                }
            }
            if (currentIndex == bottomIndex) {
                break;
            }
        }
        return returnList.isEmpty();
    }

}
