/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.decrypter.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.examples.decrypter.common.DecrypterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Class for GUI configuration section of Decrypter component.
 * 
 * @author Sascha Zur
 */
public class DecrypterAlgorithmSection extends ValidatingWorkflowNodePropertySection {

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
            .setData(ValidatingWorkflowNodePropertySection.CONTROL_PROPERTY_KEY, DecrypterComponentConstants.CONFIG_KEY_ALGORITHM);
        algorithmCombo.setItems(DecrypterComponentConstants.ALGORITHMS);

        Button saveToFileCheckbox = factory.createButton(configurationComposite, Messages.useDefaultPasswordLabel, SWT.CHECK);
        saveToFileCheckbox.setData(ValidatingWorkflowNodePropertySection.CONTROL_PROPERTY_KEY,
            DecrypterComponentConstants.CONFIG_KEY_USEDEFAULTPASSWORD);

        sectionProperties.setClient(configurationComposite);

    }
}
