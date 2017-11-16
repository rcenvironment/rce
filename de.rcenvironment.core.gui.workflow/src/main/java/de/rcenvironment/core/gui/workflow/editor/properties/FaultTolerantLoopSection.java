/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import org.eclipse.swt.widgets.Label;
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
public class FaultTolerantLoopSection extends ValidatingWorkflowNodePropertySection {

    private static final String TEXT_DISCARD = "Discard the evaluation loop run and continue with next one";

    private static final String TEXT_FINALLY_FAIL =
        "If an evaluation loop run was discarded, finally fail on loop termination (only applicable outside nested loops)";

    private static final String TEXT_ONLY_FAIL_LOOP =
        "Only fail the loop and forward failure to outer loop (only applicable if used in nested loop)";

    private Button failRadioButtonNAV;

    private Button discardAndContinueRadioButtonNAV;

    private Button rerunAndDiscardRadioButtonNAV;

    private Text rerunTimesAndFailTextNAV;

    private Button rerunAndFailRadioButtonNAV;

    private Text rerunTimesAndDiscardTextNAV;

    private Label rerunTimesAndDiscardLabelNAV;

    private Button failLoopIfAnyRunFailedCheckboxNAV;

    private Button onlyFailLoopCheckboxNAV;

    private Button failRadioButtonCmpFlr;

    private Button discardAndContinueRadioButtonCmpFlr;

    private boolean loopDriverSupportsDiscard;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        parent.setLayout(new GridLayout(1, true));

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        final Section sectionPropertiesNAV = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionPropertiesNAV.setText("Fault-tolerance in case of 'not-a-value' received");
        final Composite compositeNAV = factory.createFlatFormComposite(sectionPropertiesNAV);
        compositeNAV.setLayout(new GridLayout(3, false));
        factory.createLabel(compositeNAV, "If a component in the loop sent 'not-a-value':");

        BehaviorInCaseOfFailureSelectionListenerNAV listenerNAV = new BehaviorInCaseOfFailureSelectionListenerNAV();
        failRadioButtonNAV = factory.createButton(compositeNAV, "Fail", SWT.RADIO);
        spanHorizontal(failRadioButtonNAV);
        failRadioButtonNAV.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV, LoopBehaviorInCaseOfFailure.Fail);
        failRadioButtonNAV.addSelectionListener(listenerNAV);

        discardAndContinueRadioButtonNAV = factory.createButton(compositeNAV, TEXT_DISCARD, SWT.RADIO);
        spanHorizontal(discardAndContinueRadioButtonNAV);
        discardAndContinueRadioButtonNAV.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopBehaviorInCaseOfFailure.Discard);
        discardAndContinueRadioButtonNAV.addSelectionListener(listenerNAV);

        rerunAndFailRadioButtonNAV = factory.createButton(compositeNAV,
            "Rerun the evaluation loop at the maximum of", SWT.RADIO);
        rerunAndFailRadioButtonNAV.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopBehaviorInCaseOfFailure.RerunAndFail);
        rerunAndFailRadioButtonNAV.addSelectionListener(listenerNAV);

        final int width = 40;
        rerunTimesAndFailTextNAV = new Text(compositeNAV, SWT.BORDER | (SWT.CENTER & WidgetGroupFactory.ALIGN_CENTER));
        GridData gridData = new GridData(SWT.CENTER & WidgetGroupFactory.ALIGN_CENTER);
        gridData.widthHint = width;
        rerunTimesAndFailTextNAV.setLayoutData(gridData);
        rerunTimesAndFailTextNAV.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV);
        rerunTimesAndFailTextNAV.addVerifyListener(new NumericalTextConstraintListener(rerunTimesAndFailTextNAV,
            NumericalTextConstraintListener.GREATER_ZERO | NumericalTextConstraintListener.ONLY_INTEGER));
        factory.createLabel(compositeNAV, "time(s) and fail if maximum exceeded");

        rerunAndDiscardRadioButtonNAV = factory.createButton(compositeNAV,
            "Rerun the evaluation loop at the maximum of", SWT.RADIO);
        rerunAndDiscardRadioButtonNAV.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopBehaviorInCaseOfFailure.RerunAndDiscard);
        rerunAndDiscardRadioButtonNAV.addSelectionListener(listenerNAV);

        rerunTimesAndDiscardTextNAV = new Text(compositeNAV, SWT.BORDER | (SWT.CENTER & WidgetGroupFactory.ALIGN_CENTER));
        gridData = new GridData();
        gridData.widthHint = width;
        rerunTimesAndDiscardTextNAV.setLayoutData(gridData);
        rerunTimesAndDiscardTextNAV.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_DISCARD_NAV);
        rerunTimesAndDiscardTextNAV.addVerifyListener(new NumericalTextConstraintListener(rerunTimesAndDiscardTextNAV,
            NumericalTextConstraintListener.GREATER_ZERO | NumericalTextConstraintListener.ONLY_INTEGER));
        rerunTimesAndDiscardLabelNAV = factory.createLabel(compositeNAV, "time(s) and discard if maximum exceeded");

        failLoopIfAnyRunFailedCheckboxNAV = factory.createButton(compositeNAV, TEXT_FINALLY_FAIL, SWT.CHECK);
        spanHorizontal(failLoopIfAnyRunFailedCheckboxNAV);
        failLoopIfAnyRunFailedCheckboxNAV.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            LoopComponentConstants.CONFIG_KEY_FINALLY_FAIL_IF_DISCARDED_NAV);
        onlyFailLoopCheckboxNAV = factory.createButton(compositeNAV, TEXT_ONLY_FAIL_LOOP, SWT.CHECK);
        spanHorizontal(onlyFailLoopCheckboxNAV);
        onlyFailLoopCheckboxNAV.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            LoopComponentConstants.CONFIG_KEY_FAIL_LOOP_ONLY_NAV);

        sectionPropertiesNAV.setClient(compositeNAV);

        final Section sectionPropertiesCmpFlr = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionPropertiesCmpFlr.setText("Fault-tolerance in case of component failure");
        final Composite compositeCmpFlr = factory.createFlatFormComposite(sectionPropertiesCmpFlr);
        compositeCmpFlr.setLayout(new GridLayout(1, false));
        compositeCmpFlr.setLayoutData(gridData);
        factory.createLabel(compositeCmpFlr, "If a component in the loop fails:");

        BehaviorInCaseOfFailureSelectionListenerCompFailure listenerCmpFlr = new BehaviorInCaseOfFailureSelectionListenerCompFailure();

        failRadioButtonCmpFlr = factory.createButton(compositeCmpFlr, "Fail component", SWT.RADIO);
        spanHorizontal(failRadioButtonCmpFlr);
        failRadioButtonCmpFlr.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE,
            LoopBehaviorInCaseOfFailure.Fail);
        failRadioButtonCmpFlr.addSelectionListener(listenerCmpFlr);

        discardAndContinueRadioButtonCmpFlr = factory.createButton(compositeCmpFlr, TEXT_DISCARD, SWT.RADIO);
        spanHorizontal(discardAndContinueRadioButtonCmpFlr);
        discardAndContinueRadioButtonCmpFlr.setData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE,
            LoopBehaviorInCaseOfFailure.Discard);
        discardAndContinueRadioButtonCmpFlr.addSelectionListener(listenerCmpFlr);

        sectionPropertiesCmpFlr.setClient(compositeCmpFlr);
    }

    private void spanHorizontal(Control control) {
        GridData gridData = new GridData();
        gridData.horizontalSpan = 3;
        control.setLayoutData(gridData);
    }

    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
        LoopBehaviorInCaseOfFailure loopBehaviorInCaseOfFailureNAV = LoopBehaviorInCaseOfFailure.fromString(
            getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV));
        LoopBehaviorInCaseOfFailure loopBehaviorInCaseOfFailureCmpFlr = LoopBehaviorInCaseOfFailure.fromString(
            getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE));
        loopDriverSupportsDiscard = workflowNode.getComponentDescription().getComponentInterface().getLoopDriverSupportsDiscard();

        failRadioButtonNAV.setSelection(loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.Fail));
        failRadioButtonCmpFlr.setSelection(loopBehaviorInCaseOfFailureCmpFlr.equals(LoopBehaviorInCaseOfFailure.Fail));
        rerunAndFailRadioButtonNAV.setSelection(loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.RerunAndFail));
        rerunTimesAndFailTextNAV.setEnabled(loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.RerunAndFail));
        onlyFailLoopCheckboxNAV.setEnabled((loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.Fail)
            || loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.RerunAndFail))
            && Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));

        discardAndContinueRadioButtonNAV.setEnabled(loopDriverSupportsDiscard);
        discardAndContinueRadioButtonCmpFlr.setEnabled(loopDriverSupportsDiscard);
        rerunAndDiscardRadioButtonNAV.setEnabled(loopDriverSupportsDiscard);
        rerunTimesAndDiscardTextNAV.setEnabled(loopDriverSupportsDiscard);
        rerunTimesAndDiscardLabelNAV.setEnabled(loopDriverSupportsDiscard);
        failLoopIfAnyRunFailedCheckboxNAV.setEnabled(loopDriverSupportsDiscard
            && !Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        if (loopDriverSupportsDiscard) {
            discardAndContinueRadioButtonNAV.setSelection(loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.Discard));
            discardAndContinueRadioButtonCmpFlr.setSelection(loopBehaviorInCaseOfFailureCmpFlr.equals(LoopBehaviorInCaseOfFailure.Discard));
            rerunAndDiscardRadioButtonNAV.setSelection(loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard));
            rerunTimesAndDiscardTextNAV.setEnabled(loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard));
            failLoopIfAnyRunFailedCheckboxNAV.setEnabled(failLoopIfAnyRunFailedCheckboxNAV.getEnabled()
                && (loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.Discard)
                    || loopBehaviorInCaseOfFailureNAV.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard)));
        }

        setSelectionOfCheckbox();
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        failLoopIfAnyRunFailedCheckboxNAV.setEnabled(failLoopIfAnyRunFailedCheckboxNAV.getEnabled()
            && !Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        onlyFailLoopCheckboxNAV.setEnabled(Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
        setSelectionOfCheckbox();
    }

    private void setSelectionOfCheckbox() {
        if (!failLoopIfAnyRunFailedCheckboxNAV.isEnabled()) {
            failLoopIfAnyRunFailedCheckboxNAV.setSelection(false);
        }
        if (!onlyFailLoopCheckboxNAV.isEnabled()) {
            onlyFailLoopCheckboxNAV.setSelection(false);
        }
    }

    /**
     * {@link SelectionListener} for the radio buttons.
     * 
     * @author Doreen Seider
     */
    private class BehaviorInCaseOfFailureSelectionListenerNAV implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            Button button = ((Button) event.getSource());
            setProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
                button.getData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV).toString());
            rerunTimesAndFailTextNAV.setEnabled(button == rerunAndFailRadioButtonNAV);
            rerunTimesAndDiscardTextNAV.setEnabled(button == rerunAndDiscardRadioButtonNAV);
            failLoopIfAnyRunFailedCheckboxNAV.setEnabled(!Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP))
                && (button == discardAndContinueRadioButtonNAV || button == rerunAndDiscardRadioButtonNAV));
            onlyFailLoopCheckboxNAV
                .setEnabled((button == failRadioButtonNAV || button == rerunAndFailRadioButtonNAV)
                    && Boolean.valueOf(getProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)));
            setSelectionOfCheckbox();
        }

    }

    /**
     * {@link SelectionListener} for the radio buttons.
     * 
     * @author Doreen Seider
     */
    private class BehaviorInCaseOfFailureSelectionListenerCompFailure implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            Button button = ((Button) event.getSource());
            setProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE,
                button.getData(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE).toString());
        }

    }

}
