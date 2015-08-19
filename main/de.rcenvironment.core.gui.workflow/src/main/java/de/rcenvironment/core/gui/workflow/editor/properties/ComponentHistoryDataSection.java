/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Config tab to define if the component should store {@link ComponentHistoryDataItem}s or not.
 * 
 * @author Doreen Seider
 */
public class ComponentHistoryDataSection extends ValidatingWorkflowNodePropertySection {

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.dataItemTitle);
        final Composite sectionInstallationClient = factory.createFlatFormComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(1, false));
        final Button button = factory.createButton(sectionInstallationClient, Messages.storeDataItem, SWT.CHECK);
        button.setData(CONTROL_PROPERTY_KEY, ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM);

        factory.createLabel(sectionInstallationClient, Messages.dataItemNote);

        sectionProperties.setClient(sectionInstallationClient);
    }

}
