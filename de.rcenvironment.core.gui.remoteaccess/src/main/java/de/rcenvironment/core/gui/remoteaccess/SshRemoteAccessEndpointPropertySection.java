/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.remoteaccess;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;


/**
 * Endpoint property section for remote access component.
 *
 * @author Brigitte Boden
 */
public class SshRemoteAccessEndpointPropertySection extends EndpointPropertySection {

    public SshRemoteAccessEndpointPropertySection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, "default", null, null, this, false);
        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, "default", null, null, this, false);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }

}
