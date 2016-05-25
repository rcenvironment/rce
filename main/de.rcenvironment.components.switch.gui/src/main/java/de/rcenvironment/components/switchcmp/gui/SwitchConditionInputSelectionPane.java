/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.configuration.VariableNameVerifyListener;
import de.rcenvironment.core.gui.utils.common.endpoint.EndpointHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * 
 * Implementation of {@link EndpointSelectionPane}.
 * 
 * @author David Scholz
 */
public class SwitchConditionInputSelectionPane extends EndpointSelectionPane {

    public SwitchConditionInputSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor, boolean readonly,
        String dynamicEndpointIdToManage) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, true, true);
    }

    @Override
    protected void onAddClicked() {
        SwitchConditionEndpointEditDialog dialog =
            new SwitchConditionEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, endpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(endpointIdToManage)
                    .getMetaDataDefinition(), new HashMap<String, String>());
        onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = EndpointHelper.getStaticEndpointNames(endpointType, configuration).contains(name);
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        SwitchConditionEndpointEditDialog dialog =
            new SwitchConditionEndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                endpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(), newMetaData);

        onEditClicked(name, dialog, newMetaData);
    }

    /**
     * 
     * Implementation of {@link EndpointEditDialog}.
     * 
     * @author David Scholz
     */
    private class SwitchConditionEndpointEditDialog extends EndpointEditDialog {

        SwitchConditionEndpointEditDialog(Shell parentShell, EndpointActionType actionType,
            ComponentInstanceProperties configuration,
            EndpointType direction, String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
            super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
        }

        @Override
        protected void createEndpointSettings(Composite parent) {
            super.createEndpointSettings(parent);
            textfieldName.addListener(SWT.Verify, new VariableNameVerifyListener(VariableNameVerifyListener.NO_LEADING_NUMBERS,
                this.textfieldName));
        }

        @Override
        protected void validateInput() {
            super.validateInput();
            String text = textfieldName.getText();
            getButton(IDialogConstants.OK_ID).setEnabled(
                getButton(IDialogConstants.OK_ID).isEnabled() && (text != null && !text.isEmpty() && !Character.isDigit(text.charAt(0)))
                    && !Arrays.asList(SwitchComponentConstants.OPERATORS).contains(text));

        }
    }

}
