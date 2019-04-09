/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.xml.values.gui;

import de.rcenvironment.components.xml.values.common.XmlValuesComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyView;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyViewPane;

/**
 * EndpointPropertySection for XMLValues.
 *
 * @author Adrian Stock
 */

public class XMLValuesEndpointPropertySection extends XPathChooserPropertyView {

    public XMLValuesEndpointPropertySection() {

        XPathChooserPropertyViewPane inputPane = new XPathChooserPropertyViewPane("Inputs",
            EndpointType.INPUT, ID_INPUT_PANE, new String[] {},
            new String[] { XmlValuesComponentConstants.ENDPOINT_NAME_XML }, this);

        XPathChooserPropertyViewPane outputPane = new XPathChooserPropertyViewPane("Outputs",
            EndpointType.OUTPUT, ID_OUTPUT_PANE, new String[] {},
            new String[] { XmlValuesComponentConstants.ENDPOINT_NAME_XML }, this);

        setColumns(2);
        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
