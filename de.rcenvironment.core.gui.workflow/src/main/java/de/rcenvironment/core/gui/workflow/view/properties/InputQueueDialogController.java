/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.properties;

import java.util.Deque;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.gui.workflow.Activator;


/**
 * Controller class for the {@link InputQueueDialog}.
 *
 * @author Doreen Seider
 */
public class InputQueueDialogController {

    private static final String SETTINGS_KEY_SROLLLOCK = "de.rcenvironment.rce.gui.workflow.view.properties.settinngs.scrolllock";
    
    private static InputQueueDialogController instance;
    
    private String workflowId;
    
    private String componentId;
    
    private String inputName;
    
    private InputModel inputModel;
    
    private InputQueueDialog dialog;
    
    private IDialogSettings dialogSettings;
    
    private int tableItemCount = 0;
    
    private boolean isScrollLocked;
    
    public InputQueueDialogController(WorkflowExecutionInformation workflowInfo, String componentId, String inputName) {
        instance = this;
        this.workflowId = workflowInfo.getExecutionIdentifier();
        this.componentId = componentId;
        this.inputName = inputName;
        dialogSettings = Activator.getInstance().getDialogSettings();
        
        inputModel = InputModel.getInstance();
                
        dialog = new InputQueueDialog(Display.getCurrent().getActiveShell());
        dialog.create();
        initialize();
    }
    
    public static InputQueueDialogController getInstance() {
        return instance;
    }
    
    /**
     * Shows the dialog.
     * @return The return code (which button pressed).
     */
    public int open() {
        return dialog.open();
    }
    
    /**
     * Redraws the table.
     */
    public void redrawTable() {
        Table table = dialog.getInputQueueTableViewer().getTable();
        if (!table.isDisposed()) {
            dialog.getInputQueueTableViewer().setInput(inputModel.getInputs(workflowId, componentId, inputName));
            
            if (!isScrollLocked && table.getItemCount() > tableItemCount) {
                table.setTopIndex(table.getItemCount());
                tableItemCount = table.getItemCount();
            }
        }        
    }
        
    private void initialize() {
        dialog.getShell().setText(inputName);
        dialog.getInputQueueTableViewer().setLabelProvider(new InputQueueLabelProvider());
        dialog.getInputQueueTableViewer().setContentProvider(new InputQueueContentProvider());
        redrawTable();
        
        isScrollLocked = dialogSettings.getBoolean(SETTINGS_KEY_SROLLLOCK);
        dialog.getScrollLockButton().setSelection(isScrollLocked);
        dialog.getScrollLockButton().addListener(SWT.Selection, new Listener() {
            
            @Override
            public void handleEvent(Event event) {
                isScrollLocked = dialog.getScrollLockButton().getSelection();
                dialogSettings.put(SETTINGS_KEY_SROLLLOCK, isScrollLocked);
            }
        });
    }
        
    /**
     * Provides the concrete label texts to display and images if required.
     * 
     * @author Doreen Seider
     */
    class InputQueueLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(Object object, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object object, int columnIndex) {
            String text = "-";
            if (object instanceof EndpointDatum) {
                TypedDatum input = ((EndpointDatum) object).getValue();
                switch (input.getDataType()) {
                case FileReference:
                    text = ((FileReferenceTD) input).getFileName();
                    break;
                case DirectoryReference:
                    text = ((DirectoryReferenceTD) input).getDirectoryName();
                    break;
                default:
                    text = input.toString();
                    break;
                }
            }
            return text;
        }
        
    }
    
    /**
     * Take the whole content to structured pieces.
     * 
     * @author Doreen Seider
     */
    class InputQueueContentProvider implements IStructuredContentProvider {

        @Override
        public void dispose() {
            // do nothing
        }

        @Override
        public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
            // do nothing
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Deque<?>) {
                return ((Deque<EndpointDatum>) inputElement).toArray();
            } else {
                // empty default
                return new Object[] {};
            }
        }
        
    }

}
