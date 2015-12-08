/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Config tab to define if the component should store {@link ComponentHistoryDataItem}s or not.
 * 
 * @author Doreen Seider
 */
public class ToolMockModeSection extends ValidatingWorkflowNodePropertySection {

    private Button checkBox;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText("Tool run imitation mode");
        final Composite sectionInstallationClient = factory.createFlatFormComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(1, false));
        checkBox = factory.createButton(sectionInstallationClient, "Use the integrated tool in tool run imitation mode", SWT.CHECK);
        checkBox.setData(CONTROL_PROPERTY_KEY, ToolIntegrationConstants.KEY_IS_MOCK_MODE);

        factory.createLabel(sectionInstallationClient,
            "\nNote: The tool provider decides whether this component can be used in tool run imitation mode.\n\n"
                + "If a component is used in tool run imitation mode,"
                    + " usually the tool behind is not executed, but dummy output values are sent instead.\nThe actual behavior in "
                    + "tool run imitation mode is defined by the tool provider when integrating the tool.");

        sectionProperties.setClient(sectionInstallationClient);
    }
    
    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);

        boolean isDeactivationSupported = Boolean.valueOf(workflowNode.getConfigurationDescription().getComponentConfigurationDefinition()
            .getReadOnlyConfiguration().getValue(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED));
        if (!isDeactivationSupported) {
            setProperty(ToolIntegrationConstants.KEY_IS_MOCK_MODE, String.valueOf(false));
            checkBox.setSelection(false);
        }
        checkBox.setEnabled(isDeactivationSupported);
    }

}
