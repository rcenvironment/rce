/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * {@link EndpointPropertySection} for all custom integrated tools.
 * 
 * @author Sascha Zur
 */
public class IntegrationEndpointPropertySection extends EndpointPropertySection {

    public IntegrationEndpointPropertySection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, null, new String[] {}, null, this, false);
        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, null, new String[] {}, null, this, false);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
}
