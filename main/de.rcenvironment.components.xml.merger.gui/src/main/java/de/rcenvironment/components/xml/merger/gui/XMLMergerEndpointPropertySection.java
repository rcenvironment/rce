/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyView;

/**
 * EndpointPropertySection for XMLMerger.
 *
 * @author Brigitte Boden
 */
public class XMLMergerEndpointPropertySection extends XPathChooserPropertyView {

    public XMLMergerEndpointPropertySection() {

        XMLMergerEndpointSelectionPane inputPane = new XMLMergerEndpointSelectionPane("Inputs", EndpointType.INPUT, ID_INPUT_PANE,
            new String[] { XmlMergerComponentConstants.INPUT_ID_MAPPING_FILE },
            new String[] { XmlMergerComponentConstants.ENDPOINT_NAME_XML, XmlMergerComponentConstants.INPUT_NAME_XML_TO_INTEGRATE }, this);
        XMLMergerEndpointSelectionPane outputPane = new XMLMergerEndpointSelectionPane("Outputs", EndpointType.OUTPUT, ID_OUTPUT_PANE,
            new String[] {}, new String[] { XmlMergerComponentConstants.ENDPOINT_NAME_XML }, this);

        setColumns(2);
        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }

}
