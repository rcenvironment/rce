/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.cluster.gui.properties;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * Showing pre-defined inputs and outputs.
 * 
 * @author Doreen Seider
 */
public class ClusterComponentEndpointSection extends EndpointPropertySection {

    public ClusterComponentEndpointSection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, null,
            new String[] {}, new String[] { ClusterComponentConstants.INPUT_JOBCOUNT, ClusterComponentConstants.INPUT_JOBINPUTS,
                ClusterComponentConstants.INPUT_SHAREDJOBINPUT }, this);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, null,
            new String[] {}, new String[] { ClusterComponentConstants.OUTPUT_JOBOUTPUTS }, this, true);

        setColumns(2);
        setPanes(inputPane, outputPane);
    }

}
