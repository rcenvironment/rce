/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
    
    private Button notConvIgnoreButton;
    
    private Button notConvFailButton;
    
    private Button notConvNotAValueButton;


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

        final Composite notConvComposite = new Composite(sectionInstallationClient, SWT.NONE);
        GridLayout notConvLayout = new GridLayout(1, false);
        notConvLayout.marginWidth = 0;
        notConvComposite.setLayout(notConvLayout);

        Label notConvLabel = new Label(notConvComposite, SWT.NONE);
        notConvLabel.setText(Messages.notConvBehavior);
        GridData notConvLabelData = new GridData();
        notConvLabel.setLayoutData(notConvLabelData);

        notConvIgnoreButton = new Button(notConvComposite, SWT.RADIO);
        notConvIgnoreButton.setText(Messages.notConvIgnore);
        notConvIgnoreButton.setData(CONTROL_PROPERTY_KEY, ConvergerComponentConstants.NOT_CONVERGED_IGNORE);

        notConvFailButton = new Button(notConvComposite, SWT.RADIO);
        notConvFailButton.setText(Messages.notConvFail);
        notConvFailButton.setData(CONTROL_PROPERTY_KEY, ConvergerComponentConstants.NOT_CONVERGED_FAIL);

        notConvNotAValueButton = new Button(notConvComposite, SWT.RADIO);
        notConvNotAValueButton.setText(Messages.notConvNotAValue);
        notConvNotAValueButton.setData(CONTROL_PROPERTY_KEY, ConvergerComponentConstants.NOT_CONVERGED_NOT_A_VALUE);

        sectionProperties.setClient(sectionInstallationClient);
    }

}
