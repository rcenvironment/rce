/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.inputprovider.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * "Properties" view tab for configuring cells as additional endpoints (i.e. inputs and outputs).
 * 
 * @author Sascha Zur
 * @author Mark Geiger
 */
// TODO rename class - stam_mr, June 2015.
public class InputProviderInputSection extends DefaultEndpointPropertySection {

    public InputProviderInputSection() {

        EndpointSelectionPane inputPane = new InputProviderEndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, "default", this);

        setColumns(1);
        setPanes(inputPane);

    }

}
