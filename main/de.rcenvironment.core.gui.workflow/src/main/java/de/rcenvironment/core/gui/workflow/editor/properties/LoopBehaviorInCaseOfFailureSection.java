/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;

/**
 * Config gui for behavior in case of loop failure.
 * 
 * @author Doreen Seider
 */
public class LoopBehaviorInCaseOfFailureSection extends ValidatingWorkflowNodePropertySection {

    private Button failRadioButton;

    private Button discardAndContinueRadioButton;

    private Button rerunAndDiscardRadioButton;

    private Text rerunTimesAndFailText;

    private Button rerunAndFailRadioButton;

    private Text rerunTimesAndDiscardText;

    private Button failLoopCheckbox;

    private Button failLoopIfAnyRunFailedCheckbox;
    
    private boolean canHandleNaVValues;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        
        BehaviorInCaseOfFailureSelectionListener listener = new BehaviorInCaseOfFailureSelectionListener();
        
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText("Behavior in case of failure");
        final Composite composite = factory.createFlatFormComposite(sectionProperties);
        composite.setLayout(new GridLayout(3, false));
        factory.createLabel(composite, "If a component fails in the loop:");

        failRadioButton = factory.createButton(composite, "Fail", SWT.RADIO);
        spanHorizontal(failRadioButton);
        failRadioButton.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE, LoopBehaviorInCaseOfFailure.Fail);
        failRadioButton.addSelectionListener(listener);

        discardAndContinueRadioButton = factory.createButton(composite,
            "Discard the evaluation loop and continue with next one", SWT.RADIO);
        spanHorizontal(discardAndContinueRadioButton);
        discardAndContinueRadioButton.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE,
            LoopBehaviorInCaseOfFailure.Discard);
        discardAndContinueRadioButton.addSelectionListener(listener);

        rerunAndFailRadioButton = factory.createButton(composite, 
            "Rerun the evaluation loop at the maximum of", SWT.RADIO);
        rerunAndFailRadioButton.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE, LoopBehaviorInCaseOfFailure.RerunAndFail);
        rerunAndFailRadioButton.addSelectionListener(listener);
        
        final int width = 40;
        rerunTimesAndFailText = new Text(composite, SWT.BORDER | (SWT.CENTER & WidgetGroupFactory.ALIGN_CENTER));
        GridData gridData = new GridData(SWT.CENTER & WidgetGroupFactory.ALIGN_CENTER);
        gridData.widthHint = width;
        rerunTimesAndFailText.setLayoutData(gridData);
        rerunTimesAndFailText.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY, 
            LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_FAIL);
        rerunTimesAndFailText.addVerifyListener(new NumericalTextConstraintListener(rerunTimesAndFailText,
            NumericalTextConstraintListener.GREATER_ZERO));
        factory.createLabel(composite, "time(s) and fail if maximum exceeded");

        rerunAndDiscardRadioButton = factory.createButton(composite, 
            "Rerun the evaluation loop at the maximum of", SWT.RADIO);
        rerunAndDiscardRadioButton.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE,
            LoopBehaviorInCaseOfFailure.RerunAndDiscard);
        rerunAndDiscardRadioButton.addSelectionListener(listener);
        
        rerunTimesAndDiscardText = new Text(composite, SWT.BORDER | (SWT.CENTER & WidgetGroupFactory.ALIGN_CENTER));
        gridData = new GridData();
        gridData.widthHint = width;
        rerunTimesAndDiscardText.setLayoutData(gridData);
        rerunTimesAndDiscardText.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY, 
            LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_DISCARD);
        rerunTimesAndDiscardText.addVerifyListener(new NumericalTextConstraintListener(rerunTimesAndDiscardText,
            NumericalTextConstraintListener.GREATER_ZERO));
        factory.createLabel(composite, "time(s) and discard if maximum exceeded");
        
        factory.createLabel(composite, "");
        
        failLoopIfAnyRunFailedCheckbox = factory.createButton(composite, 
            "If any evaluation loop failed, finally fail on loop termination (only applicable if used outside nested loop)", SWT.CHECK);
        spanHorizontal(failLoopIfAnyRunFailedCheckbox);
        failLoopIfAnyRunFailedCheckbox.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            LoopComponentConstants.CONFIG_KEY_FINALLY_FAIL);
        failLoopCheckbox = factory.createButton(composite, 
            "In case of failure, fail only the loop and not the workflow (only applicable if used in nested loop)", SWT.CHECK);
        spanHorizontal(failLoopCheckbox);
        failLoopCheckbox.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            LoopComponentConstants.CONFIG_KEY_FAIL_LOOP);

        sectionProperties.setClient(composite);
    }
    
    private void spanHorizontal(Control control) {
        GridData gridData = new GridData();
        gridData.horizontalSpan = 3;
        control.setLayoutData(gridData);
    }
    
    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
        LoopBehaviorInCaseOfFailure loopBehaviorInCaseOfFailure = LoopBehaviorInCaseOfFailure.fromString(
            getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE));
        canHandleNaVValues = workflowNode.getComponentDescription().getComponentInterface().getCanHandleNotAValueDataTypes();

        failRadioButton.setSelection(loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.Fail));
        rerunAndFailRadioButton.setSelection(loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndFail));
        rerunTimesAndFailText.setEnabled(loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndFail));
        failLoopCheckbox.setEnabled(Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        
        discardAndContinueRadioButton.setEnabled(canHandleNaVValues);
        rerunAndDiscardRadioButton.setEnabled(canHandleNaVValues);
        rerunTimesAndDiscardText.setEnabled(canHandleNaVValues);
        failLoopIfAnyRunFailedCheckbox.setEnabled(canHandleNaVValues
            && !Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        if (canHandleNaVValues) {
            discardAndContinueRadioButton.setSelection(loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.Discard));
            rerunAndDiscardRadioButton.setSelection(loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard));
            rerunTimesAndDiscardText.setEnabled(loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard));
            failLoopIfAnyRunFailedCheckbox.setEnabled(failLoopIfAnyRunFailedCheckbox.getEnabled()
                && (loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.Discard)
                    || loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard)));            
        }
        
        setSelectionOfCheckbox();
        
    }
    
    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        failLoopIfAnyRunFailedCheckbox.setEnabled(failLoopIfAnyRunFailedCheckbox.getEnabled()
            && !Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        failLoopCheckbox.setEnabled(Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        setSelectionOfCheckbox();
    }
    
    private void setSelectionOfCheckbox() {
        if (!failLoopIfAnyRunFailedCheckbox.isEnabled()) {
            failLoopIfAnyRunFailedCheckbox.setSelection(false);
        }
        if (!failLoopCheckbox.isEnabled()) {
            failLoopCheckbox.setSelection(false);
        }
    }
    
    /**
     * {@link SelectionListener} for the radio buttons.
     * 
     * @author Doreen Seider
     */
    private class BehaviorInCaseOfFailureSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            Button button = ((Button) event.getSource());
            setProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE, 
                button.getData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE).toString());
            rerunTimesAndFailText.setEnabled(button == rerunAndFailRadioButton);
            rerunTimesAndDiscardText.setEnabled(button == rerunAndDiscardRadioButton);
            failLoopIfAnyRunFailedCheckbox.setEnabled(!Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP))
                && (button == discardAndContinueRadioButton || button == rerunAndDiscardRadioButton));
            setSelectionOfCheckbox();
        }

    }

}
