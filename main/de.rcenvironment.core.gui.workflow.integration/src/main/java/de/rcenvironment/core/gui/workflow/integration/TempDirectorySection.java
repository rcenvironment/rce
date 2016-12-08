/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Section for the temp directory behavior properties.
 * 
 * @author Sascha Zur
 */
public class TempDirectorySection extends ValidatingWorkflowNodePropertySection {

    private Button neverDeleteTempDirectoryButton;

    private Button onceDeleteTempDirectoryButton;

    private Button alwaysDeleteTempDirectoryButton;

    private Button onSuccessDeleteTempDirectoryButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        parent.setLayout(new GridLayout(1, true));
        
        final Composite composite = getWidgetFactory().createFlatFormComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        composite.setLayout(new GridLayout(1, true));
        
        final Section scriptSection = getWidgetFactory().createSection(composite, Section.TITLE_BAR);
        scriptSection.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        scriptSection.setText(Messages.tempDirectorySection);

        Composite scriptComposite = getWidgetFactory().createFlatFormComposite(scriptSection);
        scriptComposite.setLayout(new GridLayout(1, true));
        scriptComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        new Label(scriptComposite, SWT.None).setText("Choose the deletion behavior for working directory(ies):");
        neverDeleteTempDirectoryButton = new Button(scriptComposite, SWT.RADIO);
        neverDeleteTempDirectoryButton.setText("Do not delete working directory(ies)");
        neverDeleteTempDirectoryButton.addSelectionListener(new DeleteTempDirectorySelectionListener(
            ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER));

        onceDeleteTempDirectoryButton = new Button(scriptComposite, SWT.RADIO);
        onceDeleteTempDirectoryButton.setText("Delete working directory(ies) after workflow execution");
        onceDeleteTempDirectoryButton.addSelectionListener(new DeleteTempDirectorySelectionListener(
            ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE));

        alwaysDeleteTempDirectoryButton = new Button(scriptComposite, SWT.RADIO);
        alwaysDeleteTempDirectoryButton.setText("Delete working directory(ies) after every run");
        alwaysDeleteTempDirectoryButton.addSelectionListener(new DeleteTempDirectorySelectionListener(
            ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS));
        onSuccessDeleteTempDirectoryButton = new Button(scriptComposite, SWT.CHECK);
        onSuccessDeleteTempDirectoryButton.setText("Keep working directory(ies) in case of failure");
        onSuccessDeleteTempDirectoryButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                setProperty(ToolIntegrationConstants.KEY_KEEP_ON_FAILURE, "" + onSuccessDeleteTempDirectoryButton.getSelection());

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        scriptSection.setClient(scriptComposite);

    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        ReadOnlyConfiguration readOnlyconfig = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition().getReadOnlyConfiguration();

        boolean deleteNeverActive =
            Boolean.parseBoolean(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER));
        boolean deleteOnceActive =
            Boolean.parseBoolean(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE));
        boolean deleteAlwaysActive =
            Boolean.parseBoolean(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS));

        neverDeleteTempDirectoryButton.setEnabled(deleteNeverActive);
        onceDeleteTempDirectoryButton.setEnabled(deleteOnceActive);
        alwaysDeleteTempDirectoryButton.setEnabled(deleteAlwaysActive);

        onceDeleteTempDirectoryButton.setSelection(false);
        alwaysDeleteTempDirectoryButton.setSelection(false);
        neverDeleteTempDirectoryButton.setSelection(false);

        String chosen = getProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR);
        if (chosen != null) {
            if (chosen.equals(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE) && deleteOnceActive) {
                onceDeleteTempDirectoryButton.setSelection(true);
            } else if (chosen.equals(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS) && deleteAlwaysActive) {
                alwaysDeleteTempDirectoryButton.setSelection(true);
            } else if (chosen.equals(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER) && deleteNeverActive) {
                neverDeleteTempDirectoryButton.setSelection(true);
            } else {
                // The chosen behavior is not active anymore, determine an active option to replace it.
                determineDeletionBehaviour(deleteNeverActive, deleteOnceActive, deleteAlwaysActive);
            }
        } else {
            determineDeletionBehaviour(deleteNeverActive, deleteOnceActive, deleteAlwaysActive);
        }
        onSuccessDeleteTempDirectoryButton.setEnabled(isKeepButtonActive());
        onSuccessDeleteTempDirectoryButton.setSelection(isKeepButtonActive()
            && Boolean.parseBoolean(getProperty(ToolIntegrationConstants.KEY_KEEP_ON_FAILURE)));
        setProperty(ToolIntegrationConstants.KEY_KEEP_ON_FAILURE, "" + onSuccessDeleteTempDirectoryButton.getSelection());
    }

    private void determineDeletionBehaviour(boolean deleteNeverActive, boolean deleteOnceActive, boolean deleteAlwaysActive) {
        if (deleteOnceActive) {
            onceDeleteTempDirectoryButton.setSelection(true);
            setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE);
        } else if (deleteNeverActive) {
            neverDeleteTempDirectoryButton.setSelection(true);
            setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER);
        } else if (deleteAlwaysActive) {
            setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS);
            alwaysDeleteTempDirectoryButton.setSelection(true);
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }

    /**
     * Listener for the delete temp dir radio buttons.
     * 
     * @author Sascha Zur
     */
    private class DeleteTempDirectorySelectionListener implements SelectionListener {

        private final String key;

        DeleteTempDirectorySelectionListener(String key) {
            this.key = key;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR, key);

            onSuccessDeleteTempDirectoryButton.setEnabled(isKeepButtonActive());
            if (!isKeepButtonActive()) {
                onSuccessDeleteTempDirectoryButton.setSelection(false);
                setProperty(ToolIntegrationConstants.KEY_KEEP_ON_FAILURE, "" + false);
            }
        }
    }

    private boolean isKeepButtonActive() {
        ReadOnlyConfiguration readOnlyconfig = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition().getReadOnlyConfiguration();
        boolean alwaysActiveAndPermitted =
            alwaysDeleteTempDirectoryButton.getSelection()
                && Boolean
                    .parseBoolean(readOnlyconfig
                        .getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION));
        boolean onceActiveAndPermitted =
            onceDeleteTempDirectoryButton.getSelection()
                && Boolean.parseBoolean(readOnlyconfig
                    .getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE));
        return alwaysActiveAndPermitted || onceActiveAndPermitted;
    }
}
