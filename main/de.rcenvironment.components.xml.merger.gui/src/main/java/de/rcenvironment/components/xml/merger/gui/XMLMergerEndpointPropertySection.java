/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

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

        XMLMergerEndpointSelectionPane inputPane = new XMLMergerEndpointSelectionPane(
            "Input", EndpointType.INPUT, this, ID_INPUT_PANE);
        XMLMergerEndpointSelectionPane outputPane = new XMLMergerEndpointSelectionPane(
            "Output", EndpointType.OUTPUT, this, ID_OUTPUT_PANE);

        setColumns(2);
        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }

}
