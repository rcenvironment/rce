/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.properties;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyView;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyViewPane;

/**
 * Inputs/Outputs section.
 * 
 * @author Doreen Seider
 */
public class CPACSWriterInputsOutputsSection extends XPathChooserPropertyView {

    public CPACSWriterInputsOutputsSection() {
        super();
        XPathChooserPropertyViewPane inputPane = new XPathChooserPropertyViewPane(
            "Inputs", EndpointType.INPUT, ID_INPUT_PANE, new String[] {}, new String[] { CpacsWriterComponentConstants.INPUT_NAME_CPACS },
            this);
        XPathChooserPropertyViewPane outputPane = new XPathChooserPropertyViewPane(
            "Outputs", EndpointType.OUTPUT, ID_OUTPUT_PANE, new String[] {},
            new String[] { CpacsWriterComponentConstants.OUTPUT_NAME_CPACS },
            this);
        setColumns(2);

        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
