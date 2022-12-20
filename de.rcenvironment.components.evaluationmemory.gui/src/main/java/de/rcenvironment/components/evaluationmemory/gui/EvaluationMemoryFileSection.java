/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants.OverlapBehavior;
import de.rcenvironment.core.component.model.endpoint.api.EndpointChange;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodePropertiesSection;

/**
 * Section to set the path to the memory file.
 * 
 * @author Doreen Seider
 * @author Alexander Weinert
 * @author Kathrin Schaffert
 */
public class EvaluationMemoryFileSection extends ValidatingWorkflowNodePropertySection {

    private Text memoryFilePathText;

    private Button selectFilePathButton;

    private Button strictButton;

    private Label strictLabel;

    private Button lenientButton;

    private Label lenientLabel;

    private PropertyChangeListener propertyListener = evt -> {
        if (evt.getNewValue() instanceof EndpointChange && !strictButton.isDisposed()) {
            if (containsTolerantInputs()) {
                enableToleranceOverlapButtons();
            } else {
                disableToleranceOverlapButtons();
            }
        }
    };

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
        memFileComposite.setLayout(new RowLayout());

        final Button selectAtWfStartButton = new Button(memFileComposite, SWT.CHECK);

        final Label selectAtWfStartLabel = new Label(memFileComposite, SWT.NONE);
        selectAtWfStartLabel.setText("Select at workflow start");
        selectAtWfStartLabel.setBackground(memFileComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        final Composite selectFileComposite = propSheetPage.getWidgetFactory().createComposite(parent);
        selectFileComposite.setLayout(new GridLayout(2, true));
        selectFileComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));


        selectAtWfStartButton.setData(WorkflowNodePropertiesSection.CONTROL_PROPERTY_KEY,
            EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START);

        memoryFilePathText = new Text(selectFileComposite, SWT.WRAP | SWT.BORDER | SWT.SINGLE);
        memoryFilePathText.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE);
        memoryFilePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        selectFilePathButton = new Button(selectFileComposite, SWT.PUSH);
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
        memOptionsComposite.setLayout(new RowLayout());

        final Button storeLoopFailures = new Button(memOptionsComposite, SWT.CHECK);
        storeLoopFailures.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_CONSIDER_LOOP_FAILURES);

        final Label storeLoopFailuresLabel = new Label(memOptionsComposite, SWT.NONE);
        storeLoopFailuresLabel.setText("Consider loop failures as valid loop results"
            + " (values of type not-a-value that are explicitly sent by components)");
        storeLoopFailuresLabel.setBackground(memFileComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        TabbedPropertySheetWidgetFactory factory = propSheetPage.getWidgetFactory();

        appendToleranceOverlapConfigurationSection(parent, factory);
    }

    private void appendToleranceOverlapConfigurationSection(final Composite parent, TabbedPropertySheetWidgetFactory factory) {

        final Section sectionOverlap = factory.createSection(parent, Section.TITLE_BAR);
        sectionOverlap.setLayout(new GridLayout());
        sectionOverlap.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        sectionOverlap.setText("Handling overlapping tolerance-intervals");

        final Composite compositeOverlap = factory.createFlatFormComposite(parent);

        appendLabel(factory, compositeOverlap);
        appendStrictButton(compositeOverlap);
        appendLenientButton(compositeOverlap);

    }

    private void appendLabel(final TabbedPropertySheetWidgetFactory factory, final Composite compositeOverlap) {
        compositeOverlap.setLayout(new GridLayout(2, false));
        Label label = factory.createLabel(compositeOverlap,
            "If the tolerance-interval around the input values contains multiple previously evaluated values:");
        final GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        label.setLayoutData(gridData);
    }

    private void appendStrictButton(final Composite compositeOverlap) {
        strictButton = new Button(compositeOverlap, SWT.RADIO);
        strictButton.setData(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR,
            OverlapBehavior.STRICT);
        strictButton.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR);
        strictLabel = new Label(compositeOverlap, SWT.NONE);
        strictLabel.setText("Strict: Evaluate with exact input-values");
        strictLabel.setBackground(compositeOverlap.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    }

    private void appendLenientButton(final Composite compositeOverlap) {
        lenientButton = new Button(compositeOverlap, SWT.RADIO);
        lenientButton.setData(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR,
            OverlapBehavior.LENIENT);
        lenientButton.setData(CONTROL_PROPERTY_KEY, EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR);
        lenientLabel = new Label(compositeOverlap, SWT.NONE);
        lenientLabel.setText("Lenient: Return any previous evaluation within tolerance");
        lenientLabel.setBackground(compositeOverlap.getDisplay().getSystemColor(SWT.COLOR_WHITE));
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

        ComponentInstanceProperties config = getConfiguration();
        config.addPropertyChangeListener(propertyListener);
    }

    private void enableFilePickerWidgets(boolean enabled) {
        memoryFilePathText.setEnabled(enabled);
        selectFilePathButton.setEnabled(enabled);
    }

    private void disableToleranceOverlapButtons() {
        this.strictButton.setEnabled(false);
        this.lenientButton.setEnabled(false);
        this.strictLabel.setEnabled(false);
        this.lenientLabel.setEnabled(false);
    }

    private void enableToleranceOverlapButtons() {
        this.strictButton.setEnabled(true);
        this.lenientButton.setEnabled(true);
        this.strictLabel.setEnabled(true);
        this.lenientLabel.setEnabled(true);
    }

    @Override
    public void refreshSection() {
        super.refreshSection();

        final boolean containsTolerantInputs = containsTolerantInputs();
        if (containsTolerantInputs) {
            enableToleranceOverlapButtons();
        } else {
            disableToleranceOverlapButtons();
        }

    }

    @Override
    protected void beforeTearingDownModelBinding() {
        super.beforeTearingDownModelBinding();
        ComponentInstanceProperties config = getConfiguration();
        config.removePropertyChangeListener(propertyListener);
    }

    @Override
    protected EvaluationMemoryFileDefaultUpdater createUpdater() {
        return new EvaluationMemoryFileDefaultUpdater();
    }

    /**
     * 
     * Evaluation Memory File {@link DefaultUpdater} implementation of the handler to update the Evaluation Memory UI.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected class EvaluationMemoryFileDefaultUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            super.updateControl(control, propertyName, newValue, oldValue);
            // update Button "Select at workflow start"
            if (control instanceof Button && propertyName.equals(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START)) {
                enableFilePickerWidgets(!Boolean.valueOf(newValue));
            }
            // update Buttons for "Handling overlapping tolerance-intervals"
            if (control instanceof Button
                && propertyName.equals(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR)) {
                // We set strict overlap handling as the default in order to maintain ``backwards compability'', i.e., in order to not break
                // user expectations
                final String overlapBehaviorString =
                    getConfiguration().getConfigurationDescription().getConfigurationValue(propertyName);
                switch (OverlapBehavior.parseConfigValue(overlapBehaviorString)) {
                case LENIENT:
                    lenientButton.setSelection(true);
                    break;
                case STRICT:
                    strictButton.setSelection(true);
                    break;
                default:
                    // This should never happen, unless new overlap behaviors are introduced. In order to avoid warnings, however, we
                    // explicitly
                    // check for this case as well. (A.W.)
                    strictButton.setSelection(true);
                    break;
                }
            }
        }
    }

    @Override
    protected Controller createController() {
        return new EvaluationMemoryController();
    }

    /**
     * 
     * Evaluation Memory {@link DefaultController} implementation to handle the button activation.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected class EvaluationMemoryController extends DefaultController {

        @Override
        public void widgetSelected(final SelectionEvent event) {

            if (!(event.getSource() instanceof Button)) {
                return;
            }

            Button button = (Button) event.getSource();
            String key = (String) button.getData(CONTROL_PROPERTY_KEY);

            if (key == null) {
                return;
            }

            // control for Button "Select at workflow start"
            if (key.equals(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START)) {
                enableFilePickerWidgets(!button.getSelection());
                setProperty(key, Boolean.toString(button.getSelection()));
            }
            // control for Button "Consider loop failures ..."
            if (key.equals(EvaluationMemoryComponentConstants.CONFIG_CONSIDER_LOOP_FAILURES)) {
                setProperty(key, Boolean.toString(button.getSelection()));
            }
            // control Buttons for "Handling overlapping tolerance-intervals"
            if (key.equals(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR)) {
                if (button.getData(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR)
                    .equals(OverlapBehavior.STRICT)) {
                    setThresholdOverlapBehavior(OverlapBehavior.STRICT);
                    strictButton.setSelection(true);
                } else {
                    setThresholdOverlapBehavior(OverlapBehavior.LENIENT);
                    lenientButton.setSelection(true);
                }
            }
        }

        private void setThresholdOverlapBehavior(OverlapBehavior behavior) {
            final String configKey = EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR;
            setProperty(configKey, behavior.toString());
        }
    }
}
