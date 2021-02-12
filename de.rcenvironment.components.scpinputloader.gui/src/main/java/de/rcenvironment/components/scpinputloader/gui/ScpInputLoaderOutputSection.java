/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.scpinputloader.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * Output section for SCP input loader.
 * 
 * @author Brigitte Boden
 */
public class ScpInputLoaderOutputSection extends DefaultEndpointPropertySection {

    public ScpInputLoaderOutputSection() {
        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, "default", new String[] {}, new String[] {}, this);

        setColumns(1);
        setPanes(outputPane);
        
    }


}
