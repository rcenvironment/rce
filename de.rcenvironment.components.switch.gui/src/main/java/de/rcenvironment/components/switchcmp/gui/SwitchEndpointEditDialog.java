/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.configuration.VariableNameVerifyListener;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * 
 * Implementation of {@link EndpointEditDialog}.
 * 
 * @author David Scholz
 * @author Kathrin Schaffert
 */
public class SwitchEndpointEditDialog extends EndpointEditDialog {

    SwitchEndpointEditDialog(Shell parentShell, EndpointActionType actionType,
        ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    @Override
    protected void createEndpointSettings(Composite parent) {
        super.createEndpointSettings(parent);
        textfieldName.addListener(SWT.Verify, new VariableNameVerifyListener(VariableNameVerifyListener.PYTHON_VIABLE,
            this.textfieldName));
    }

    @Override
    protected void validateInput() {
        super.validateInput();
        String text = textfieldName.getText();
        getButton(IDialogConstants.OK_ID).setEnabled(
            getButton(IDialogConstants.OK_ID).isEnabled() && (text != null && !text.isEmpty() && !Character.isDigit(text.charAt(0)))
                && !Arrays.asList(SwitchComponentConstants.OPERATORS).contains(text)
                && !Arrays.asList(SwitchComponentConstants.PYTHON_KEYWORDS).contains(text));

    }
}
