/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import de.rcenvironment.core.component.api.LoopComponentConstants;

/**
 * Nested loop config gui.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class NestedLoopSection extends ValidatingWorkflowNodePropertySection {

    private Button nestedLoopButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.nestedLoopTitle);
        final Composite sectionInstallationClient = factory.createFlatFormComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(1, false));
        nestedLoopButton = factory.createButton(sectionInstallationClient, Messages.isNestedLoop, SWT.CHECK);
        nestedLoopButton.setData(CONTROL_PROPERTY_KEY, LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP);

        sectionProperties.setClient(sectionInstallationClient);
    }

}
