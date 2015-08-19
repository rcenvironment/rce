/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Provides a GUI for the parametric study component.
 * 
 * @author Sascha Zur
 */
public class ConvergerParameterSection extends ValidatingWorkflowNodePropertySection {

    private static final int TEXT_WIDTH = 50;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.parameterTitle);
        final Composite sectionInstallationClient = factory.createFlatFormComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(2, false));
        WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(sectionInstallationClient,
            Messages.absoluteConvergenceMessage, ConvergerComponentConstants.KEY_EPS_A, TEXT_WIDTH, WidgetGroupFactory.ONLY_FLOAT
                | WidgetGroupFactory.ALIGN_CENTER);
        WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(sectionInstallationClient,
            Messages.relativeConvergenceMessage, ConvergerComponentConstants.KEY_EPS_R, TEXT_WIDTH, WidgetGroupFactory.ONLY_FLOAT
                | WidgetGroupFactory.ALIGN_CENTER);
        WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(sectionInstallationClient,
            Messages.iterationsToConsider, ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, TEXT_WIDTH,
            WidgetGroupFactory.ONLY_INTEGER | WidgetGroupFactory.GREATER_OR_EQUAL_ZERO | WidgetGroupFactory.ALIGN_CENTER);
        Text maxConvChecksText = WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(sectionInstallationClient,
            Messages.maxConvChecks, ConvergerComponentConstants.KEY_MAX_CONV_CHECKS, TEXT_WIDTH, WidgetGroupFactory.ONLY_INTEGER
                | WidgetGroupFactory.GREATER_OR_EQUAL_ZERO | WidgetGroupFactory.ALIGN_CENTER).text;
        maxConvChecksText.setMessage(Messages.noMaxIterations);
        
        sectionProperties.setClient(sectionInstallationClient);
    }

}
