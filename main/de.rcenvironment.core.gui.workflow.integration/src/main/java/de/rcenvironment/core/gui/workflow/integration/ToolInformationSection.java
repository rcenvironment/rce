/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
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

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        Section infoSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, getWidgetFactory(),
            Messages.infoSection);
        Composite infoComposite = getWidgetFactory().createFlatFormComposite(infoSection);
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

        infoSection.setClient(infoComposite);

    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        updateLabel();
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
