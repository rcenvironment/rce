/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A dialog for editing a single endpoint configuration.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Tim Rosenbach
 */
public class EndpointInputWithOutputEditDialog extends EndpointEditDialog {

    public EndpointInputWithOutputEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    public EndpointInputWithOutputEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic,
        EndpointMetaDataDefinition metaData, Map<String, String> newMetaData, int readOnlyType) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, newMetaData, readOnlyType);
    }

    @Override
    protected void validateInput() {

        hideMessage();

        String name = getNameInputFromUI();
        // initialName is null if not set, so it will not be equal when naming a new endpoint
        boolean nameIsValid = name.equals(initialName);
        nameIsValid |= isValidForBothTypes(name);

        if (!nameIsValid) {
            if (name.isEmpty()) {
                updateMessage(StringUtils.format(Messages.missingNameMessage, type.toString()), true);
            } else {
                updateMessage(Messages.invalidNameMessage, true);
            }
        }

        // enable/disable "ok"
        getButton(IDialogConstants.OK_ID).setEnabled(nameIsValid & validateMetaDataInputs());
    }

    private boolean isValidForBothTypes(String name) {
        boolean valid = epManager.isValidEndpointName(name);
        EndpointDescriptionsManager otherEpManager;
        if (type == EndpointType.INPUT) {
            otherEpManager = configuration.getOutputDescriptionsManager();
        } else {
            otherEpManager = configuration.getInputDescriptionsManager();
        }
        valid &= otherEpManager.isValidEndpointName(name);
        return valid;
    }
}
