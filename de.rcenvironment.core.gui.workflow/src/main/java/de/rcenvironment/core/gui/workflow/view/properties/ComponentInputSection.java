/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPart;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.WorkflowRunNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;

/**
 * Property section for displaying and editing inputs.
 * 
 * @author Doreen Seider
 */
public class ComponentInputSection extends AbstractInputSection {
    
    private static ComponentInputSection instance;
    
    private WorkflowNode workflowNode;
    
    private String componentId;
    
    public ComponentInputSection() {
        super();
        instance = this;
    }
    
    public static ComponentInputSection getInstance() {
        return instance;
    }
    
    @Override
    protected void retrieveWorkflowInformation(IWorkbenchPart part, ISelection selection) {
        final Object firstSelectionElement = ((IStructuredSelection) selection).getFirstElement();
        workflowNode = (WorkflowNode) ((WorkflowRunNodePart) firstSelectionElement).getModel();
        wfExeInfo = (WorkflowExecutionInformation) ((WorkflowExecutionInformationPart) ((WorkflowPart)
            ((WorkflowRunNodePart) firstSelectionElement)
                .getParent()).getParent()).getModel();
        componentId = wfExeInfo.getComponentExecutionInformation(workflowNode.getIdentifierAsObject()).getExecutionIdentifier();
    }
    
    @Override
    protected void initializeTreeViewer(IWorkbenchPart part, ISelection selection) {
        inputTreeViewer.setLabelProvider(new EditableInputLabelProvider(wfExeInfo.getExecutionIdentifier(), componentId));
        inputTreeViewer.setInput(workflowNode);
    }

    @Override
    protected void openInputDialog(TreeItem item) {
        new InputQueueDialogController(wfExeInfo, componentId, item.getText()).open();
    }
}
