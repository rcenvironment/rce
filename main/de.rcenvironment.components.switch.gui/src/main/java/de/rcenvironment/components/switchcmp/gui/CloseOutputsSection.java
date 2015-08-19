/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Tab where the user can decide whether the component should close outputs if <code>true</code> or <code>false</code> was sent.
 * 
 * @author Doreen Seider
 */
public class CloseOutputsSection extends ValidatingWorkflowNodePropertySection {

    private Button neverCloseButton;
    
    private Button closeOnTrueButton;
    
    private Button closeOnFalseButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayout(new GridLayout(1, false));
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
        Section parentSection = aTabbedPropertySheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        parentSection.setLayout(new GridLayout());
        parentSection.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        parentSection.setText("Close Outputs");
        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        final Composite mainComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(parent);
        mainComposite.setLayout(new GridLayout(1, true));
        mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        neverCloseButton = new Button(mainComposite, SWT.RADIO);
        neverCloseButton.setText("Never close outputs");
        neverCloseButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        neverCloseButton.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.NEVER_CLOSE_OUTPUTS_KEY);

        closeOnTrueButton = new Button(mainComposite, SWT.RADIO);
        closeOnTrueButton.setText("Close outputs on 'True'");
        closeOnTrueButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        closeOnTrueButton.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.CLOSE_OUTPUTS_ON_TRUE_KEY);
        
        closeOnFalseButton = new Button(mainComposite, SWT.RADIO);
        closeOnFalseButton.setText("Close outputs on 'False'");
        closeOnFalseButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        closeOnFalseButton.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.CLOSE_OUTPUTS_ON_FALSE_KEY);

        Label noteLabel = new Label(mainComposite, SWT.NONE);
        noteLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        noteLabel.setText("Use 'Never close outputs' if the switch component is used outside of a loop or "
            + "if it is not supposed to control the loop.\nUse one of the other options if the switch component "
            + "is supposed to control a loop.\nClosing outputs will close all of the inputs, which are connected to the outputs. "
            + "A component is finished, if all of its inputs are closed.");

    }
    
}
