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

import de.rcenvironment.core.component.api.LoopComponentConstants;

/**
 * Nested loop config gui.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Kathrin Schaffert
 */
public class NestedLoopSection extends ValidatingWorkflowNodePropertySection {

    private Button nestedLoopButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayout(new GridLayout(1, false));
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.nestedLoopTitle);
        sectionProperties.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Composite sectionInstallationClient = factory.createFlatFormComposite(parent);
        sectionInstallationClient.setLayout(new GridLayout(2, false));
        nestedLoopButton = new Button(sectionInstallationClient, SWT.CHECK);
        nestedLoopButton.setData(CONTROL_PROPERTY_KEY, LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP);
        Label neestedLoopLabel = new Label(sectionInstallationClient, SWT.NONE);
        neestedLoopLabel.setText(Messages.isNestedLoop);
        neestedLoopLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    }

}
