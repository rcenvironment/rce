/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Tab where the user can decide whether the component should close outputs if <code>true</code> or <code>false</code> was sent.
 * 
 * @author Doreen Seider
 * @author Kathrin Schaffert
 */
public class CloseOutputsSection extends ValidatingWorkflowNodePropertySection {

    private Button neverCloseButton;

    private Button closeOnConditionButton;

    private Button closeOnNoMatchButton;

    private Combo conditionCombo;

    private PropertyChangeListener registeredListener;

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
        mainComposite.setLayout(new GridLayout(2, false));

        CloseOutputsSelectionListener closeOutListener = new CloseOutputsSelectionListener();

        // The implementation here with a Radio Button plus separate Label - instead of setting the Button's Text variable - is intentional.
        // The reason is GUI issues regarding the visibility of check marks on different (Linux) platforms with different desktop variants.
        // Kathrin Schaffert, 26.01.2022
        neverCloseButton = new Button(mainComposite, SWT.RADIO);
        neverCloseButton.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.NEVER_CLOSE_OUTPUTS_KEY);
        neverCloseButton.addSelectionListener(closeOutListener);

        Label neverCloseOutputLabel = new Label(mainComposite, SWT.NONE);
        neverCloseOutputLabel.setText("Never close outputs");
        neverCloseOutputLabel.setBackground(mainComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        closeOnConditionButton = new Button(mainComposite, SWT.RADIO);
        closeOnConditionButton.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.CLOSE_OUTPUTS_ON_CONDITION_NUMBER_KEY);
        closeOnConditionButton.addSelectionListener(closeOutListener);

        final Composite comboComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(mainComposite);
        comboComposite.setLayout(new GridLayout(2, false));

        Label closeOnConditionLabel = new Label(comboComposite, SWT.NONE);
        closeOnConditionLabel.setText("Close outputs on condition number: ");
        closeOnConditionLabel.setBackground(mainComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        ConditionComboListener condCombListener = new ConditionComboListener();

        conditionCombo = new Combo(comboComposite, SWT.READ_ONLY);
        conditionCombo.setEnabled(false);
        conditionCombo.pack();
        conditionCombo.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.SELECTED_CONDITION);
        conditionCombo.addSelectionListener(condCombListener);

        closeOnNoMatchButton = new Button(mainComposite, SWT.RADIO);
        closeOnNoMatchButton.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.CLOSE_OUTPUTS_ON_NO_MATCH_KEY);
        closeOnNoMatchButton.addSelectionListener(closeOutListener);

        Label closeOnNoMatchLabel = new Label(mainComposite, SWT.NONE);
        closeOnNoMatchLabel.setText("Close outputs if there is no match.");
        closeOnNoMatchLabel.setBackground(mainComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        
        Label noteLabel = new Label(mainComposite, SWT.NONE);
        noteLabel.setLayoutData(gridData);
        noteLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        noteLabel.setText("Use 'Never close outputs' if the switch component is used outside of a loop or "
            + "if it is not supposed to control the loop.\nUse one of the other options if the switch component "
            + "is supposed to control a loop.\nClosing outputs will close all of the inputs which are connected to the outputs. "
            + "A component is finished if all of its inputs are closed.");

    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);

        if (registeredListener == null) {

            ComponentInstanceProperties config = getConfiguration();
            registeredListener = evt -> {
                if (evt.getPropertyName().equals(SwitchComponentConstants.CONDITION_KEY_PROPERTY_ID)) {
                    refreshCombo();
                    refreshButtonActivation();
                }
            };
            config.addPropertyChangeListener(registeredListener);
        }
    }

    /**
     * {@link SelectionListener} for the radio buttons.
     * 
     */
    private class CloseOutputsSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent evt) {
            widgetSelected(evt);
        }

        @Override
        public void widgetSelected(SelectionEvent evt) {
            Button button = ((Button) evt.getSource());
            if (conditionCombo.getItems().length != 0) {
                conditionCombo.setEnabled(button == closeOnConditionButton);
            }
        }
    }

    /**
     * {@link SelectionListener} for the condition combo box.
     * 
     */
    private class ConditionComboListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent evt) {
            widgetSelected(evt);
        }

        @Override
        public void widgetSelected(SelectionEvent evt) {
            String selected = conditionCombo.getItem(conditionCombo.getSelectionIndex());
            setProperty(SwitchComponentConstants.SELECTED_CONDITION, selected);
        }
    }

    private void refreshCombo() {
        conditionCombo.removeAll();
        if (getProperty(SwitchComponentConstants.CONDITION_KEY) != null) {
            ComponentInstanceProperties config = getConfiguration();
            String conTable = config.getConfigurationDescription().getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);
            int numOfCon = SwitchConditionSection.getTableContentLength(conTable);
            if (numOfCon > 0) {
                for (int i = 1; i <= numOfCon; i++) {
                    conditionCombo.add(Integer.toString(i));
                }
            }
        }
    }

    private void refreshButtonActivation() {

        conditionCombo.setEnabled(closeOnConditionButton.getSelection() && (conditionCombo.getItems().length != 0));

        String selected = getProperty(SwitchComponentConstants.SELECTED_CONDITION);
        if (selected != null) {
            conditionCombo.select(Integer.parseInt(selected) - 1);
        }
    }

    @Override
    protected Controller createController() {
        return new CloseOutputsController();
    }

    /**
     * 
     * Close Outputs {@link DefaultController} implementation to handle the button activation.
     * 
     * @author Kathrin Schaffert
     *
     */
    private class CloseOutputsController extends DefaultController {

        @Override
        public void widgetSelected(final SelectionEvent event) {
            if (event.getSource() instanceof Button) {
                Button button = ((Button) event.getSource());
                if (button.getSelection()) {
                    String key1 = (String) (button).getData(CONTROL_PROPERTY_KEY);
                    for (Control control : button.getParent().getChildren()) {
                        if (!(control instanceof Button) || ((Button) control).equals(button)) {
                            continue;
                        }

                        final String key2 = (String) control.getData(CONTROL_PROPERTY_KEY);
                        Boolean val = Boolean.valueOf(getConfiguration().getConfigurationDescription().getConfigurationValue(key2));
                        if (Boolean.TRUE.equals(val)) {
                            setProperties(key1, String.valueOf(true), key2, String.valueOf(false));
                        }
                    }
                    refreshButtonActivation();
                }
            }
        }
    }

    @Override
    protected CloseOutputsUpdater createUpdater() {
        return new CloseOutputsUpdater();
    }

    /**
     * Close Outputs {@link DefaultUpdater} implementation of the handler to update the Close Outputs UI.
     * 
     */
    protected class CloseOutputsUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            super.updateControl(control, propertyName, newValue, oldValue);

            if (control instanceof Combo) {
                refreshCombo();
                if (newValue != null) {
                    conditionCombo.select(Integer.parseInt(newValue) - 1);
                }
            }
            if (control instanceof Button && ((Button) control).getData(CONTROL_PROPERTY_KEY)
                .equals(SwitchComponentConstants.CLOSE_OUTPUTS_ON_CONDITION_NUMBER_KEY)) {
                conditionCombo.setEnabled(
                    newValue != null && newValue.equals("true") && getProperty(SwitchComponentConstants.CONDITION_KEY) != null);
            }
        }
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refreshCombo();
        refreshButtonActivation();
    }

    @Override
    protected void beforeTearingDownModelBinding() {
        super.beforeTearingDownModelBinding();
        ComponentInstanceProperties config = getConfiguration();
        config.removePropertyChangeListener(registeredListener);
        registeredListener = null;
    }

}
