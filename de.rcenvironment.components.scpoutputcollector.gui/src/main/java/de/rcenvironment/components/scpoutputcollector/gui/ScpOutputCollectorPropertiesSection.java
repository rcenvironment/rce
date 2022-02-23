/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.scpoutputcollector.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * "Properties" view tab for configuring only inputs.
 * 
 * @author Brigitte Boden
 * 
 */
public class ScpOutputCollectorPropertiesSection extends DefaultEndpointPropertySection {

    

    public ScpOutputCollectorPropertiesSection() {

        EndpointSelectionPane outputPane =
            new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, "default", new String[] {}, new String[] {}, this);
        setColumns(1);
        setPanes(outputPane);
    }
}
