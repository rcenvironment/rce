/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants.OverlapBehavior;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodePropertiesSection;

/**
 * Section to set the path to the memory file.
 * 
 * @author Doreen Seider
 * @author Alexander Weinert
 */
public class EvaluationMemoryFileSection extends ValidatingWorkflowNodePropertySection {

    /**
     * Listener for the radio buttons governing overlapping tolerance intervals.
     *
     * @author Alexander Weinert
     */
    public class ToleranceOverlapBehaviorListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            /* Since we only use this listener in this class and only for radio buttons, we know the source to be a button */
            final Button button = (Button) event.getSource();
            final String configKey = EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR;
            final OverlapBehavior behavior = (OverlapBehavior) button.getData(configKey);
            setThresholdOverlapBehavior(behavior);
        }

        private void setThresholdOverlapBehavior(OverlapBehavior behavior) {
            final String configKey = EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR;
            setProperty(configKey, behavior.toString());
        }
    }

    private Text memoryFilePathText;

    private Button selectFilePathButton;

    private Button strictButton;

    private Button lenientButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage propSheetPage) {
        super.createCompositeContent(parent, propSheetPage);
        parent.setLayout(new GridLayout(1, false));
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));

        Section memFileSection = propSheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        memFileSection.setLayout(new GridLayout());
        memFileSection.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        memFileSection.setText("Evaluation Memory File");

        final Composite memFileComposite = propSheetPage.getWidgetFactory().createComposite(parent);
        memFileComposite.setLayout(new GridLayout(2, true));
        memFileComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));

        final Button selectAtWfStartButton = new Button(memFileComposite, SWT.CHECK);
        selectAtWfStartButton.setText("Select at workflow start");
        selectAtWfStartButton.setBackground(memFileComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        selectAtWfStartButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                enableFilePickerWidgets(!selectAtWfStartButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        selectAtWfStartButton.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        selectAtWfStartButton.setLayoutData(gridData);

        memoryFilePathText = new Text(memFileComposite, SWT.WRAP | SWT.BORDER | SWT.SINGLE);
        memoryFilePathText.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE);
        memoryFilePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        selectFilePathButton = new Button(memFileComposite, SWT.PUSH);
        selectFilePathButton.setText("...");
        selectFilePathButton.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event event) {
                FileDialog dialog = new FileDialog(parent.getShell(), SWT.OPEN);
                String path = dialog.open();
                if (path != null) {
                    memoryFilePathText.setText(path);
                }
            }
        });

        Section memOptionsSection = propSheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        memOptionsSection.setLayout(new GridLayout());
        memOptionsSection.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        memOptionsSection.setText("Evaluation Memory Options");

        final Composite memOptionsComposite = propSheetPage.getWidgetFactory().createComposite(parent);
        memOptionsComposite.setLayout(new GridLayout(1, false));
        memOptionsComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));

        final Button storeLoopFailures = new Button(memOptionsComposite, SWT.CHECK);
        storeLoopFailures.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_CONSIDER_LOOP_FAILURES);
        storeLoopFailures.setText("Consider loop failures as valid loop results"
            + " (values of type not-a-value that are explicitly sent by components)");
        storeLoopFailures.setBackground(memOptionsComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        TabbedPropertySheetWidgetFactory factory = propSheetPage.getWidgetFactory();

        appendToleranceOverlapConfigurationSection(parent, factory);

        parent.addPaintListener(new PaintListener() {

            @Override
            public void paintControl(PaintEvent event) {
                final boolean containsTolerantInputs = containsTolerantInputs();
                if (containsTolerantInputs) {
                    enableToleranceOverlapButtons();
                } else {
                    disableToleranceOverlapButtons();
                }

            }
        });
    }

    private void appendToleranceOverlapConfigurationSection(final Composite parent, TabbedPropertySheetWidgetFactory factory) {
        final Section sectionOverlap = appendTitleBar(parent, factory);
        final Composite compositeOverlap = factory.createFlatFormComposite(sectionOverlap);

        appendLabel(factory, compositeOverlap);

        ToleranceOverlapBehaviorListener listener = new ToleranceOverlapBehaviorListener();

        appendStrictButton(factory, compositeOverlap, listener);
        appendLenientButton(factory, compositeOverlap, listener);

        sectionOverlap.setClient(compositeOverlap);
    }

    private Section appendTitleBar(final Composite parent, final TabbedPropertySheetWidgetFactory factory) {
        final Section sectionOverlap = factory.createSection(parent, Section.TITLE_BAR);
        sectionOverlap.setLayout(new GridLayout());
        sectionOverlap.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        sectionOverlap.setText("Handling overlapping tolerance-intervals");
        return sectionOverlap;
    }

    private void appendLabel(final TabbedPropertySheetWidgetFactory factory, final Composite compositeOverlap) {
        compositeOverlap.setLayout(new GridLayout(3, false));
        factory.createLabel(compositeOverlap,
            "If the tolerance-interval around the input values contains multiple previously evaluated values:");
    }

    private void appendStrictButton(final TabbedPropertySheetWidgetFactory factory, final Composite compositeOverlap,
        ToleranceOverlapBehaviorListener listener) {
        final GridData gridData = new GridData();
        gridData.horizontalSpan = 3;

        strictButton = factory.createButton(compositeOverlap, "Strict: Evaluate with exact input-values", SWT.RADIO);
        strictButton.setLayoutData(gridData);
        strictButton.setData(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR,
            OverlapBehavior.STRICT);
        strictButton.addSelectionListener(listener);
    }

    private void appendLenientButton(final TabbedPropertySheetWidgetFactory factory, final Composite compositeOverlap,
        final ToleranceOverlapBehaviorListener listener) {
        final GridData gridData = new GridData();
        gridData.horizontalSpan = 3;

        lenientButton = factory.createButton(compositeOverlap, "Lenient: Return any previous evaluation within tolerance", SWT.RADIO);
        lenientButton.setLayoutData(gridData);
        lenientButton.setData(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR,
            OverlapBehavior.LENIENT);
        lenientButton.addSelectionListener(listener);
    }

    private boolean containsTolerantInputs() {
        final Set<EndpointDescription> inputs = getInputs();
        for (final EndpointDescription input : inputs) {
            final Map<String, String> metaData = input.getMetaData();
            final boolean containsToleranceKey = metaData.containsKey(EvaluationMemoryComponentConstants.META_TOLERANCE);
            if (containsToleranceKey) {
                final String toleranceValue = metaData.get(EvaluationMemoryComponentConstants.META_TOLERANCE);
                final boolean toleranceValueIsNonDefault = (!toleranceValue.isEmpty());
                if (toleranceValueIsNonDefault) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
        Boolean selectAtWfStart = Boolean.valueOf(getConfiguration().getConfigurationDescription()
            .getConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START));
        enableFilePickerWidgets(!selectAtWfStart);

        // We set strict overlap handling as the default in order to maintain ``backwards compability'', i.e., in order to not break user
        // expectations
        final String overlapBehaviorKey = EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR;
        final String overlapBehaviorString = getConfiguration().getConfigurationDescription().getConfigurationValue(overlapBehaviorKey);


        switch (OverlapBehavior.parseConfigValue(overlapBehaviorString)) {
        case LENIENT:
            lenientButton.setSelection(true);
            break;
        case STRICT:
            strictButton.setSelection(true);
            break;
        default:
            // This should never happen, unless new overlap behaviors are introduced. In order to avoid warnings, however, we explicitly
            // check for this case as well.
            strictButton.setSelection(true);
            break;
        }
    }

    private void enableFilePickerWidgets(boolean enabled) {
        memoryFilePathText.setEnabled(enabled);
        selectFilePathButton.setEnabled(enabled);
    }

    private void disableToleranceOverlapButtons() {
        this.strictButton.setEnabled(false);
        this.lenientButton.setEnabled(false);
    }

    private void enableToleranceOverlapButtons() {
        this.strictButton.setEnabled(true);
        this.lenientButton.setEnabled(true);
    }
}
