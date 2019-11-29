/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.evaluationmemory.gui;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;


/**
 * 
 * The dialog shown for adding and editing inputs to evaluate for the evaluation memory component. We require a custom dialog since we want
 * to allow the user to provide tolerances on the values.
 *
 * @author Alexander Weinert
 */
public class EvaluationMemoryEndpointEditDialog extends EndpointEditDialog {

    private Button useToleranceButton;

    private Label toleranceFieldLabel;

    private Text toleranceField;

    private DataType previousDataType = null;

    private Label percentageSignLabel;

    public EvaluationMemoryEndpointEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    @Override
    protected void createEndpointSettings(Composite parent) {
        super.createEndpointSettings(parent);
        comboDataType.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                final DataType newDataType = getTypeSelectionFromUI();
                updateControlsAndDataType(newDataType);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
    }

    @Override
    protected Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        appendUseToleranceButton(container);

        appendToleranceFieldAndLabel(container, text);

        // Since the only metadata for which we create an input control in this component is the tolerance, we can skip checking the text
        // and the data type, but only need to check whether some tolerance has been set, i.e., whether the value is nonempty
        if (value.isEmpty()) {
            enableUseToleranceButton();
            disableToleranceField();
        } else {
            enableUseToleranceButton();
            useToleranceButton.setSelection(true);
            toleranceField.setText(value);
        }

        previousDataType = getTypeSelectionFromUI();

        return toleranceField;
    }
    
    private boolean useTolerance() {
        return useToleranceButton.getSelection();
    }

    private String getToleranceFieldText() {
        return toleranceField.getText();
    }

    @Override
    protected void okPressed() {
        if (useTolerance()) {
            setToleranceInMetadata(getToleranceFieldText());
        } else {
            removeToleranceFromMetadata();
        }
        super.okPressed();
    }

    private void setToleranceInMetadata(String tolerance) {
        metadataValues.put(EvaluationMemoryComponentConstants.META_TOLERANCE, tolerance);
    }
    
    private void removeToleranceFromMetadata() {
        metadataValues.put(EvaluationMemoryComponentConstants.META_TOLERANCE, "");
    }

    private void appendUseToleranceButton(Composite container) {
        useToleranceButton = new Button(container, SWT.CHECK);
        useToleranceButton.setText("Use relative tolerance");
        useToleranceButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                final boolean useTolerance = useToleranceButton.getSelection();
                if (useTolerance) {
                    enableToleranceField();
                } else {
                    disableToleranceField();
                }
                // We need to explicitly validate the inputs at this point, as the change listener usually doing this upon changes to
                // the interface only fires when the text value in some field has changed
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        final GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        useToleranceButton.setLayoutData(gridData);
    }

    private void appendToleranceFieldAndLabel(Composite container, String text) {
        toleranceFieldLabel = new Label(container, SWT.NONE);
        toleranceFieldLabel.setText(text);
        toleranceFieldLabel.setVisible(true);

        final Composite toleranceFieldContainer = new Composite(container, SWT.NONE);
        toleranceFieldContainer.setLayout(new GridLayout(2, false));
        toleranceFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        toleranceField = new Text(toleranceFieldContainer, SWT.BORDER | SWT.RIGHT);
        toleranceField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        toleranceField.setVisible(true);
        
        percentageSignLabel = new Label(toleranceFieldContainer, SWT.NONE);
        percentageSignLabel.setText(EvaluationMemoryComponentConstants.PERCENTAGE_SIGN);
        percentageSignLabel.setVisible(true);

        // Since tolerance is always given relative, we have to expect and enforce floats in this field
        toleranceField.addVerifyListener(new NumericalTextConstraintListener(toleranceField, WidgetGroupFactory.ONLY_FLOAT));
    }

    private boolean dataTypeSupportsTolerance(DataType dataType) {
        /*
         * Only Integers and Floats, i.e., numerical values support tolerance. If we later want to support tolerances for other values, we
         * need to adapt these lines to enable specification of tolerance in the GUI for those values
         */
        final boolean previousIsInt = dataType.equals(DataType.Integer);
        final boolean previousIsFloat = dataType.equals(DataType.Float);
        return previousIsInt || previousIsFloat;
    }

    private void updateControlsAndDataType(DataType newDataType) {
        final boolean previousSupportsTolerance = dataTypeSupportsTolerance(previousDataType);
        final boolean currentSupportsTolerance = dataTypeSupportsTolerance(newDataType);

        if (previousSupportsTolerance && !currentSupportsTolerance) {
            disableUseToleranceButton();
            disableToleranceField();
        } else if (!previousSupportsTolerance && currentSupportsTolerance) {
            enableUseToleranceButton();
            disableToleranceField();
        }
        /*
         * Only remaining cases: supporting tolerance did not change. In this case, we also do not change the controls: If the previous and
         * current type both support tolerance, the user may want to keep their setting. If neither value supports tolerance, nothing needs
         * to change, as the controls were disabled previously.
         */

        previousDataType = newDataType;
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * We add custom validation to the metadata since we only want to validate the given tolerance value if the user actually wants to use
     * tolerance. This information is not present anymore in the stored metadata.
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog#validateMetaDataInputs()
     */
    @Override
    protected boolean validateMetaDataInputs() {
        // Read this condition as "useTolerance -> !toleranceValue.isEmpty". Since Java does not support implication, we rewrite this to
        // "!useTolerance || !toleranceValue.isEmpty"
        final String toleranceValue = toleranceField.getText();
        final boolean validTolerance = !useTolerance() || !toleranceValue.isEmpty();

        return validTolerance && super.validateMetaDataInputs();
    }

    private void disableUseToleranceButton() {
        useToleranceButton.setSelection(false);
        useToleranceButton.setEnabled(false);
    }

    private void enableUseToleranceButton() {
        useToleranceButton.setSelection(false);
        useToleranceButton.setEnabled(true);
    }

    private void disableToleranceField() {
        toleranceFieldLabel.setEnabled(false);
        toleranceField.setEnabled(false);
        toleranceField.setEditable(false);
        percentageSignLabel.setEnabled(false);
    }

    private void enableToleranceField() {
        toleranceFieldLabel.setEnabled(true);
        toleranceField.setEnabled(true);
        toleranceField.setEditable(true);
        percentageSignLabel.setEnabled(true);
    }

}
