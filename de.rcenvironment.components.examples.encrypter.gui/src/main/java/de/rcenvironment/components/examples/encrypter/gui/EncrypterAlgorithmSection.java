/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.examples.encrypter.common.EncrypterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Class for GUI configuration section of encoder component.
 * 
 * @author Sascha Zur
 */
public class EncrypterAlgorithmSection extends ValidatingWorkflowNodePropertySection {

    @Override
    protected void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        /*
         * First, create a section in the configuration tab. Use the widget factory for all
         * elements.
         */
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.algorithmTabTitle);
        // Create composite in section
        final Composite configurationComposite = factory.createFlatFormComposite(sectionProperties);
        configurationComposite.setLayout(new GridLayout(2, false));

        // Create content of the section
        new Label(configurationComposite, SWT.NONE).setText(Messages.algorithmComboLabel);

        CCombo algorithmCombo = factory.createCCombo(configurationComposite, SWT.READ_ONLY);
        // The setData method is used to automatically synchronize the GUI elements with a
        // configuration value.
        algorithmCombo
            .setData(ValidatingWorkflowNodePropertySection.CONTROL_PROPERTY_KEY, EncrypterComponentConstants.CONFIG_KEY_ALGORITHM);
        algorithmCombo.setItems(EncrypterComponentConstants.ALGORITHMS);

        sectionProperties.setClient(configurationComposite);

    }
}
