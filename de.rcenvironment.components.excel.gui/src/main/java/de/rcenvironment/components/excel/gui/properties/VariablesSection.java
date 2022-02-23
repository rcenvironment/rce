/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.gui.properties;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * "Properties" view tab for configuring cells as additional endpoints (i.e. inputs and outputs).
 * 
 * @author Patrick Schaefer
 * @author Markus Kunde
 */
public class VariablesSection extends EndpointPropertySection {

    public VariablesSection() {
        super();
        VariablesSelectionPane inputPane =
            new VariablesSelectionPane(Messages.inputPaneName, EndpointType.INPUT, ExcelPropertiesConstants.ID_INPUT_PANE, this);
        VariablesSelectionPane outputPane =
            new VariablesSelectionPane(Messages.outputPaneName, EndpointType.OUTPUT, ExcelPropertiesConstants.ID_OUTPUT_PANE, this);
        setColumns(2);
        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
