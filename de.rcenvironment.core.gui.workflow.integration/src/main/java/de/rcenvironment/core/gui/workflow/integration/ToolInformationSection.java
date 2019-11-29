/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.gui.workflow.editor.documentation.ToolIntegrationDocumentationGUIHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * GUI Section for information about the integrated tool and its creator.
 * 
 * @author Sascha Zur
 */
public class ToolInformationSection extends ValidatingWorkflowNodePropertySection {

    private Label toolNameLabel;

    private Label integratorNameLabel;

    private Label integratorEmailLabel;

    private Label toolDescriptionLabel;

    private Composite infoComposite;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        parent.setLayout(new GridLayout(1, true));
        
        final Composite composite = getWidgetFactory().createFlatFormComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        composite.setLayout(new GridLayout(1, true));
        
        final Section infoSection = getWidgetFactory().createSection(composite, Section.TITLE_BAR);
        infoSection.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        infoSection.setText(Messages.infoSection);

        infoComposite = getWidgetFactory().createFlatFormComposite(infoSection);
        infoComposite.setLayout(new GridLayout(2, false));
        new Label(infoComposite, SWT.NONE).setText(Messages.toolNameLabel);
        toolNameLabel = new Label(infoComposite, SWT.NONE);

        new Label(infoComposite, SWT.NONE).setText(Messages.toolIntegratorNameLabel);
        integratorNameLabel = new Label(infoComposite, SWT.NONE);

        new Label(infoComposite, SWT.NONE).setText(Messages.toolIntegratorEmailLabel);
        integratorEmailLabel = new Label(infoComposite, SWT.NONE);

        Label descriptionLabel = new Label(infoComposite, SWT.BEGINNING);

        descriptionLabel.setText(Messages.toolDescriptionLabel);
        GridData labelGridData = new GridData();
        labelGridData.verticalAlignment = GridData.BEGINNING;
        descriptionLabel.setLayoutData(labelGridData);

        toolDescriptionLabel = new Label(infoComposite, SWT.NONE);

        Button toolDocumentationButton = new Button(infoComposite, SWT.PUSH);
        GridData docuData = new GridData();
        docuData.horizontalSpan = 2;
        toolDocumentationButton.setLayoutData(docuData);
        toolDocumentationButton.setText("Open Documentation");

        toolDocumentationButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                String identifier = getConfiguration().getComponentIdentifierWithVersion();
                ToolIntegrationDocumentationGUIHelper.getInstance().showComponentDocumentation(identifier, false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }

        });

        infoSection.setClient(infoComposite);

    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        updateLabel();
        infoComposite.layout();
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }

    private void updateLabel() {
        toolNameLabel.setText("");
        integratorNameLabel.setText("");
        integratorEmailLabel.setText("");
        toolDescriptionLabel.setText("");
        ReadOnlyConfiguration readOnlyconfig = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition().getReadOnlyConfiguration();
        readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_NAME);

        if (readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_NAME) != null) {
            toolNameLabel.setText(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_NAME));
        }
        if (readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME) != null) {
            integratorNameLabel.setText(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_NAME));
        }
        if (readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL) != null) {
            integratorEmailLabel.setText(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_INTEGRATOR_EMAIL));
        }
        if (readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DESCRIPTION) != null) {
            toolDescriptionLabel.setText(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DESCRIPTION));
        }
    }

}
