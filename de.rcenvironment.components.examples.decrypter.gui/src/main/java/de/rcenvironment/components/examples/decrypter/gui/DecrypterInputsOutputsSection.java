/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.examples.decrypter.gui;

import de.rcenvironment.components.examples.decrypter.common.DecrypterComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * Inputs/Outputs section.
 * 
 * @author Doreen Seider
 */
public class DecrypterInputsOutputsSection extends EndpointPropertySection {

    public DecrypterInputsOutputsSection() {
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, null,
            new String[] {}, new String[] { DecrypterComponentConstants.INPUT_NAME_FILE }, this, true);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, null,
            new String[] {}, new String[] { DecrypterComponentConstants.OUTPUT_NAME_RESULT }, this, true);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
}
