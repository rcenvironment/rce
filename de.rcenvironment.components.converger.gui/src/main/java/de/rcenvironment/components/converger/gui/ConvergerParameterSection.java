/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
 * @author Kathrin Schaffert
 */
public class ConvergerParameterSection extends ValidatingWorkflowNodePropertySection {

    private static final int TEXT_WIDTH = 50;

    private Button notConvIgnoreButton;

    private Button notConvFailButton;

    private Button notConvNotAValueButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        parent.setLayout(new GridLayout(1, false));

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sectionProperties.setText(Messages.parameterTitle);

        final Composite sectionInstallationClient = factory.createFlatFormComposite(parent);
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

        final Composite notConvComposite = factory.createFlatFormComposite(parent);
        notConvComposite.setLayout(new GridLayout(2, false));

        Label notConvLabel = new Label(notConvComposite, SWT.NONE);
        notConvLabel.setText(Messages.notConvBehavior);
        GridData notConvLabelData = new GridData();
        notConvLabelData.horizontalSpan = 2;
        notConvLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        notConvLabel.setLayoutData(notConvLabelData);

        // The implementation in the following with Radio Button plus separate Label - instead of setting the Button's Text variable - is
        // intentional. The reason is GUI issues regarding the visibility of check marks on different (Linux) platforms with different
        // desktop variants. (#17880)
        // Kathrin Schaffert, 07.03.2022

        notConvIgnoreButton = new Button(notConvComposite, SWT.RADIO);
        notConvIgnoreButton.setData(CONTROL_PROPERTY_KEY, ConvergerComponentConstants.NOT_CONVERGED_IGNORE);

        Label notConvIgnoreLabel = new Label(notConvComposite, SWT.NONE);
        notConvIgnoreLabel.setText(Messages.notConvIgnore);
        notConvIgnoreLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        notConvFailButton = new Button(notConvComposite, SWT.RADIO);
        notConvFailButton.setData(CONTROL_PROPERTY_KEY, ConvergerComponentConstants.NOT_CONVERGED_FAIL);

        Label notConvFailLabel = new Label(notConvComposite, SWT.NONE);
        notConvFailLabel.setText(Messages.notConvFail);
        notConvFailLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        notConvNotAValueButton = new Button(notConvComposite, SWT.RADIO);
        notConvNotAValueButton.setData(CONTROL_PROPERTY_KEY, ConvergerComponentConstants.NOT_CONVERGED_NOT_A_VALUE);

        Label notConvNotAValueLabel = new Label(notConvComposite, SWT.NONE);
        notConvNotAValueLabel.setText(Messages.notConvNotAValue);
        notConvNotAValueLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    }

    @Override
    protected DefaultController createController() {
        return new ConvergerParameterSectionController();
    }

    /**
     * 
     * Convergence Parameter {@link DefaultController} implementation to handle the button activation.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected class ConvergerParameterSectionController extends DefaultController {

        @Override
        public void widgetSelected(final SelectionEvent event) {
            Button button = (Button) event.getSource();
            String key1 = (String) (button).getData(CONTROL_PROPERTY_KEY);
            if (button.getSelection()) {
                for (Control control : button.getParent().getChildren()) {
                    if (!(control instanceof Button) || ((Button) control).equals(button)) {
                        continue;
                    }

                    final String key2 = (String) control.getData(CONTROL_PROPERTY_KEY);
                    String val = getConfiguration().getConfigurationDescription().getConfigurationValue(key2);
                    if (Boolean.TRUE.equals(Boolean.valueOf(val))) {
                        setProperties(key1, String.valueOf(true), key2, String.valueOf(false));
                        break;
                    }
                }
            }
        }
    }
}
