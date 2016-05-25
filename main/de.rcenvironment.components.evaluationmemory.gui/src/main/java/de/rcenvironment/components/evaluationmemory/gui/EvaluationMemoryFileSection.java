/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodePropertiesSection;

/**
 * Section to set the path to the memory file.
 * 
 * @author Doreen Seider
 */
public class EvaluationMemoryFileSection extends ValidatingWorkflowNodePropertySection {

    private Text memoryFilePathText;

    private Button selectFilePathButton;

    private Button selectAtWfStartButton;

    private Button storeLoopFailures;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage propSheetPage) {
        super.createCompositeContent(parent, propSheetPage);
        parent.setLayout(new GridLayout(1, false));
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
        
        Section memFileSection = propSheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        memFileSection.setLayout(new GridLayout());
        memFileSection.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        memFileSection.setText("Evaluation Memory File");

        final Composite memFileComposite = propSheetPage.getWidgetFactory().createComposite(parent);
        memFileComposite.setLayout(new GridLayout(2, true));
        memFileComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));

        selectAtWfStartButton = new Button(memFileComposite, SWT.CHECK);
        selectAtWfStartButton.setText("Select at workflow start");
        selectAtWfStartButton.setBackground(memFileComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        selectAtWfStartButton.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                enableWidgets(!selectAtWfStartButton.getSelection());
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        
        selectAtWfStartButton.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        selectAtWfStartButton.setLayoutData(gridData);
        
        memoryFilePathText = new Text(memFileComposite, SWT.WRAP | SWT.BORDER | SWT.SINGLE);
        memoryFilePathText.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE);
        memoryFilePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        
        selectFilePathButton = new Button(memFileComposite, SWT.PUSH);
        selectFilePathButton.setText("...");
        selectFilePathButton.addListener(SWT.Selection, new Listener() {
            
            @Override
            public void handleEvent(Event event) {
                FileDialog dialog = new FileDialog(parent.getShell(), SWT.OPEN);
                String path = dialog.open();
                if (path != null) {
                    memoryFilePathText.setText(path);
                }
            }
        });
        
        Section memOptionsSection = propSheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        memOptionsSection.setLayout(new GridLayout());
        memOptionsSection.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        memOptionsSection.setText("Evaluation Memory Options");
        
        final Composite memOptionsComposite = propSheetPage.getWidgetFactory().createComposite(parent);
        memOptionsComposite.setLayout(new GridLayout(2, true));
        memOptionsComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));

        storeLoopFailures = new Button(memOptionsComposite, SWT.CHECK);
        storeLoopFailures.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_CONSIDER_LOOP_FAILURES);
        storeLoopFailures.setText("Consider loop failures as valid loop results"
            + " (values of type not-a-value that are explicitly sent by components)");
        storeLoopFailures.setBackground(memOptionsComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    }
    
    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
        Boolean selectAtWfStart = Boolean.valueOf(getConfiguration().getConfigurationDescription()
            .getConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START));
        enableWidgets(!selectAtWfStart);
    }

    private void enableWidgets(boolean enabled) {
        memoryFilePathText.setEnabled(enabled);
        selectFilePathButton.setEnabled(enabled);
    }

}
