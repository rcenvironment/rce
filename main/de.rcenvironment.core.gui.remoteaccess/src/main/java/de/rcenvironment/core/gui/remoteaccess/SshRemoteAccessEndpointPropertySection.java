/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
            EndpointType.INPUT, null, new String[] {}, null, this, true);
        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, null, new String[] {}, null, this, true);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }

}
