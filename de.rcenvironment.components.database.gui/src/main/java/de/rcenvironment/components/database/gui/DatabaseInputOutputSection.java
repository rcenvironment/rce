/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.database.gui;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;


/**
 * Database component input output section.
 *
 * @author Oliver Seebach
 * @author Doreen Seider
 */
public class DatabaseInputOutputSection extends EndpointPropertySection {

    public DatabaseInputOutputSection() {
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, "default",
            new String[] {}, new String[] {}, this);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, "default",
            new String[] {}, new String[] { DatabaseComponentConstants.OUTPUT_NAME_SUCCESS }, this);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
}
