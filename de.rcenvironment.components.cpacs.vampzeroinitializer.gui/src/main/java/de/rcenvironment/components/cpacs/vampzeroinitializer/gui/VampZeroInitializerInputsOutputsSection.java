/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyView;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyViewPane;

/**
 * Inputs/Outputs section.
 * 
 * @author Doreen Seider
 */
public class VampZeroInitializerInputsOutputsSection extends XPathChooserPropertyView {

    public VampZeroInitializerInputsOutputsSection() {
        super();
        XPathChooserPropertyViewPane inputPane = new XPathChooserPropertyViewPane(
            "Inputs", EndpointType.INPUT, ID_INPUT_PANE, new String[] {}, new String[] {}, this);
        XPathChooserPropertyViewPane outputPane = new XPathChooserPropertyViewPane(
            "Outputs", EndpointType.OUTPUT, ID_OUTPUT_PANE, new String[] {},
            new String[] { VampZeroInitializerComponentConstants.OUTPUT_NAME_CPACS }, this);
        setColumns(2);

        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
