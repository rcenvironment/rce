/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.gui.properties;

import de.rcenvironment.components.xml.loader.common.XmlLoaderComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyView;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyViewPane;

/**
 * Inputs/Outputs section.
 * 
 * @author Doreen Seider
 */
public class XMLLoaderInputsOutputsSection extends XPathChooserPropertyView {

    public XMLLoaderInputsOutputsSection() {
        super();
        XPathChooserPropertyViewPane inputPane = new XPathChooserPropertyViewPane(
            "Inputs", EndpointType.INPUT, ID_INPUT_PANE, new String[] {}, new String[] {}, this);
        XPathChooserPropertyViewPane outputPane = new XPathChooserPropertyViewPane(
            "Outputs", EndpointType.OUTPUT, ID_OUTPUT_PANE, new String[] {}, new String[] { XmlLoaderComponentConstants.OUTPUT_NAME_XML },
            this);
        setColumns(2);

        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
