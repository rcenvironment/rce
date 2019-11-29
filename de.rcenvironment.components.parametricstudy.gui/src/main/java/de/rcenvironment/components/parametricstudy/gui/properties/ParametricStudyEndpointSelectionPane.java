/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.properties;

import java.util.Collections;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.ProcessEndpointsGroupCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * 
 * Endpoint Selection Pane. Changes behavior of editing the Design Variable and introduces a special {@link EndpointEditDialog} implementing
 * the needs of the parametric study.
 *
 * @author Jascha Riedel
 */
public class ParametricStudyEndpointSelectionPane extends EndpointSelectionPane {

    private EndpointSelectionPane inputPane;

    private final Executor executor;

    public ParametricStudyEndpointSelectionPane(String title, EndpointType direction, Executor executor,
        EndpointSelectionPane inputPane) {
        super(title, direction, null, new String[] {}, new String[] { ParametricStudyComponentConstants.OUTPUT_NAME_DV }, executor, false,
            true);
        this.inputPane = inputPane;
        this.executor = executor;
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = endpointManager.getEndpointDescription(name).getEndpointDefinition().isStatic();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        EndpointEditDialog dialog =
            new DesignVariableEndpointDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                dynEndpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);
        onEditClicked(name, dialog, newMetaData);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this);
        groupCommand.add(new EditDynamicEndpointCommand(super.endpointType, oldDescription, newDescription, this));

        createParameterCommand(groupCommand, oldDescription, newDescription,
            ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_FROM_VALUE);
        createParameterCommand(groupCommand, oldDescription, newDescription,
            ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_TO_VALUE);
        createParameterCommand(groupCommand, oldDescription, newDescription,
            ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_STEPSIZE_VALUE);

        execute(groupCommand);
    }

    private void createParameterCommand(ProcessEndpointsGroupCommand groupCommand,
        EndpointDescription oldDescription, EndpointDescription newDescription,
        String metaDataOutput) {
        boolean oldUseInputAsFromValue = oldDescription.getMetaData()
            .get(metaDataOutput) != null
            && Boolean.parseBoolean(oldDescription.getMetaData()
                .get(metaDataOutput));

        boolean newUseInputAsFromValue = newDescription.getMetaData()
            .get(metaDataOutput) != null
            && Boolean.parseBoolean(newDescription.getMetaData()
                .get(metaDataOutput));

        String inputName;
        switch (metaDataOutput) {
        case ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_FROM_VALUE:
            inputName = ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE;
            break;
        case ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_TO_VALUE:
            inputName = ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE;
            break;
        case ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_STEPSIZE_VALUE:
            inputName = ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE;
            break;
        default:
            return;
        }

        if (newUseInputAsFromValue && !oldUseInputAsFromValue) {
            groupCommand.add(new AddDynamicInputCommand(
                ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
                inputName,
                DataType.Float,
                Collections.<String, String> emptyMap(),
                ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS, this, inputPane));
        } else if (!newUseInputAsFromValue && oldUseInputAsFromValue) {
            groupCommand.add(new RemoveDynamicInputCommand(ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
                inputName,
                this, inputPane));
        }
    }

    /**
     * 
     * Special EndpointEditDialog for the Design variable.
     *
     * @author Jascha Riedel
     */
    private final class DesignVariableEndpointDialog extends EndpointEditDialog {

        DesignVariableEndpointDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
            EndpointType direction, String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
            super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
        }

        @Override
        protected void createSettings(Map<Integer, String> sortedKeyMap, Composite container) {
            if (sortedKeyMap.values().size() > 1) {
                Label useInput = new Label(container, SWT.NONE);
                useInput.setText("Use Input");
                GridData useInputGridData = new GridData(SWT.RIGHT, SWT.FILL, true, false);
                useInputGridData.horizontalSpan = 2;
                useInput.setLayoutData(useInputGridData);
            }
            for (String key : sortedKeyMap.values()) {

                if (!metaData.getVisibility(key).equals(Visibility.developerConfigurable)
                    && metadataIsActive(key, metaData.getActivationFilter(key))) {
                    String value = metadataValues.get(key);
                    if (value == null || value.equals("")) {
                        value = metaData.getDefaultValue(key);
                        metadataValues.put(key, value);
                    }
                    if (metaData.getDataType(key).equals(EndpointMetaDataConstants.TYPE_BOOL)) {
                        Label horizontalLine = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
                        GridData horizontalLineGD = new GridData(SWT.FILL, SWT.FILL, true, false);
                        horizontalLineGD.horizontalSpan = 2;
                        horizontalLine.setLayoutData(horizontalLineGD);
                        Button newCheckbox = createLabelAndCheckbox(container, metaData.getGuiName(key) + COLON, value);
                        newCheckbox.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true));
                        widgetToKeyMap.put(newCheckbox, key);
                        newCheckbox.addSelectionListener(new SelectionChangedListener());
                    } else if ((metaData.getPossibleValues(key) == null || metaData.getPossibleValues(key).contains("*"))) {

                        Text newTextfield = createLabelTextFieldAndCheckBox(container,
                            metaData.getGuiName(key) + COLON, metaData.getDataType(key), value, key);
                        widgetToKeyMap.put(newTextfield, key);
                        newTextfield.addModifyListener(new MethodPropertiesModifyListener());
                    }
                }
            }
        }

        private Text createLabelTextFieldAndCheckBox(Composite container, String label, String dataType, String value, String key) {
            new Label(container, SWT.NONE).setText(label);
            Composite container2 = new Composite(container, SWT.FILL);
            container2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            GridLayout textCheckBoxLayout = new GridLayout(2, false);
            textCheckBoxLayout.marginWidth = 0;
            textCheckBoxLayout.marginHeight = 0;
            container2.setLayout(textCheckBoxLayout);
            Text result = new Text(container2, SWT.SINGLE | SWT.BORDER);
            result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            result.setText(value);

            if (dataType.equals(EndpointMetaDataConstants.TYPE_INT)) {
                result.addVerifyListener(new NumericalTextConstraintListener(result,
                    NumericalTextConstraintListener.ONLY_INTEGER));
                if (value.equals(MINUS)) {
                    result.setText("");
                }
            }
            if (dataType.equals(EndpointMetaDataConstants.TYPE_FLOAT)) {
                result.addVerifyListener(new NumericalTextConstraintListener(result,
                    NumericalTextConstraintListener.ONLY_FLOAT));
                if (value.equals(MINUS)) {
                    result.setText("");
                }
            }
            if (dataType.equals(EndpointMetaDataConstants.TYPE_FLOAT_GREATER_ZERO)) {
                result.addVerifyListener(new NumericalTextConstraintListener(result,
                    NumericalTextConstraintListener.ONLY_FLOAT | NumericalTextConstraintListener.GREATER_OR_EQUAL_ZERO));
                // TODO "GREATER_OR_EQUAL_ZERO" is a quickfix to prohibit the user from entering a "-" sign,
                // since "GREATER_ZERO" did not work as expected. Should be changed when the underlying issue #14301 is fixed.
                if (value.equals(MINUS)) {
                    result.setText("");
                }
            }
            Button newButton = new Button(container2, SWT.CHECK | SWT.RIGHT);
            widgetToKeyMap.put(newButton, metaData.getGuiActivationFilter(key).keySet().iterator().next());
            newButton.addSelectionListener(new SelectionChangedListener());
            newButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
            if (metadataValues.get(metaData.getGuiActivationFilter(key).keySet().iterator().next()).equals("true")) {
                newButton.setSelection(true);
            } else {
                newButton.setSelection(false);
            }
            return result;
        }
    }

}
