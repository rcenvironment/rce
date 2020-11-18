/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

public class WorkflowComponentEndpointSection extends DefaultEndpointPropertySection {
    public WorkflowComponentEndpointSection() {
        EndpointSelectionPane inputPane = new EndpointSelectionPane("Inputs", EndpointType.INPUT, "default",
            new String[] {}, null, this, true);

        EndpointSelectionPane outputPane = new EndpointSelectionPane("Outputs", EndpointType.OUTPUT, "default",
            new String[] {}, null, this, true);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
}
