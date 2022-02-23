/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPart;

import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;

/**
 * Property section for displaying and editing inputs.
 * 
 * @author Doreen Seider
 */
public class InputSection extends AbstractInputSection {

    private static InputSection instance;
    
    private Map<String, String> componentNameToIdMapping;

    public InputSection() {
        super();
        instance = this;
    }
    
    public static InputSection getInstance() {
        return instance;
    }
    
    @Override
    protected void retrieveWorkflowInformation(IWorkbenchPart part, ISelection selection) {
        final Object firstSelectionElement = ((IStructuredSelection) selection).getFirstElement();
        wfExeInfo =
            (WorkflowExecutionInformation) ((WorkflowExecutionInformationPart) firstSelectionElement).getModel();
        componentNameToIdMapping = new HashMap<String, String>();
        for (WorkflowNode workflowNode : wfExeInfo.getWorkflowDescription().getWorkflowNodes()) {
            ComponentExecutionInformation componentExecutionInformation =
                wfExeInfo.getComponentExecutionInformation(workflowNode.getIdentifierAsObject());
            componentNameToIdMapping.put(componentExecutionInformation.getInstanceName(),
                componentExecutionInformation.getExecutionIdentifier());
        }
    }
    
    @Override
    protected void initializeTreeViewer(IWorkbenchPart part, ISelection selection) {
        inputTreeViewer.setLabelProvider(new EditableInputLabelProvider(wfExeInfo));
        inputTreeViewer.setInput(wfExeInfo.getWorkflowDescription());
    }
    
    @Override
    protected void openInputDialog(TreeItem item) {
        TreeItem childItem = item;
        while (item.getParentItem() != null) {
            item = item.getParentItem();
        }
        
        String componentId = componentNameToIdMapping.get(item.getText());
        
        new InputQueueDialogController(wfExeInfo, componentId, childItem.getText()).open();
    }
}
