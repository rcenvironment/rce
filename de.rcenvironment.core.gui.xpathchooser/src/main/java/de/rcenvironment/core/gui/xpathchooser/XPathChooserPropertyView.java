/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * A "Properties" view tab for configuring dynamic endpoints. Allows new channels to be added via XPathChooser.
 *
 * @author Markus Kunde
 * @author Markus Litz
 */
public class XPathChooserPropertyView extends EndpointPropertySection {

    protected static final String ID_INPUT_PANE = "default";

    /** ID of output pane. */
    protected static final String ID_OUTPUT_PANE = "default";

    public XPathChooserPropertyView() {
        super();
        XPathChooserPropertyViewPane inputPane = new XPathChooserPropertyViewPane(
            Messages.inputPaneName, EndpointType.INPUT, ID_INPUT_PANE, new String[] {}, new String[] {}, this);
        XPathChooserPropertyViewPane outputPane = new XPathChooserPropertyViewPane(
            Messages.outputPaneName, EndpointType.OUTPUT, ID_OUTPUT_PANE, new String[] {}, new String[] {}, this);
        setColumns(2);

        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
