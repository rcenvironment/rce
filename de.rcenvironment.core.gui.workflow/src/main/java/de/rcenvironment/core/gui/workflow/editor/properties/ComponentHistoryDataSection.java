/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;

/**
 * Config tab to define if the component should store {@link ComponentHistoryDataItem}s or not.
 * 
 * @author Doreen Seider
 * @author Kathrin Schaffert
 */
public class ComponentHistoryDataSection extends ValidatingWorkflowNodePropertySection {

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        parent.setLayout(new GridLayout(1, false));

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sectionProperties.setText(Messages.dataItemTitle);

        final Composite sectionInstallationClient = factory.createFlatFormComposite(parent);
        sectionInstallationClient.setLayout(new GridLayout(2, false));

        final Button button = new Button(sectionInstallationClient, SWT.CHECK);
        button.setData(CONTROL_PROPERTY_KEY, ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM);

        final Label label = new Label(sectionInstallationClient, SWT.NONE);
        label.setText(Messages.storeDataItem);
        label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        Label note = factory.createLabel(sectionInstallationClient, Messages.dataItemNote);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        note.setLayoutData(gridData);

    }

}
