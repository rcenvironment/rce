/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.xml.merger.gui;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.xpathchooser.XPathEditDialog;


/**
 * Constructor.
 *
 * @author Brigitte Boden
 */
public class XMLMergerEndpointEditDialog extends XPathEditDialog {


   

    public XMLMergerEndpointEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, Image icon, EndpointMetaDataDefinition metaData,
        Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, icon, metaData, metadataValues);
    }

    @Override
    protected void validateInput() {
        String name = getNameInputFromUI();
        // initialName is null if not set, so it will not be equal when naming a new endpoint
        boolean nameIsValid = name.equals(initialName);
        nameIsValid |= epManager.isValidEndpointName(name);
        
        //Do not allow to add a dynamic input with the name "mapping file", because it is reserved for the mapping file input.
        nameIsValid &= !name.equalsIgnoreCase(XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE);
        // enable/disable "ok"
        getButton(IDialogConstants.OK_ID).setEnabled(nameIsValid & validateMetaDataInputs());
    }
}
