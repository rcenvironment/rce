/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * "Properties" view tab for endpoints.
 * 
 * @author Doreen Seider
 */
public class ScriptEndpointSection extends DefaultEndpointPropertySection {
    
    public ScriptEndpointSection() {
        super();
        EndpointSelectionPane inputPane = new ScriptInputSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, false, ScriptComponentConstants.GROUP_NAME_AND, false);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, false, "default", false);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
    
}
