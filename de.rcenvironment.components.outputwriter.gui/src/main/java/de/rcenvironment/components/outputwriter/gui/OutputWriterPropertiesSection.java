/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;

/**
 * Creates a "Properties" view tab for configuring endpoints (only inputs).
 * 
 * @author Hendrik Abbenhaus
 * @author Sascha Zur
 * @author Brigitte Boden
 * 
 */
public class OutputWriterPropertiesSection extends DefaultEndpointPropertySection {

    public OutputWriterPropertiesSection() {

        OutputWriterEndpointSelectionPane outputPane =
            new OutputWriterEndpointSelectionPane(Messages.inputs, EndpointType.INPUT, "default", this);
        setColumns(1);
        setPanes(outputPane);

    }

}
