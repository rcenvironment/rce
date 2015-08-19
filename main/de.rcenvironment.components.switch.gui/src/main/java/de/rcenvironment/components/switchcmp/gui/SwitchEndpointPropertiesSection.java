/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * 
 * Properties section for switch component.
 *
 * @author David Scholz
 */
public class SwitchEndpointPropertiesSection extends EndpointPropertySection {

    public SwitchEndpointPropertiesSection() {

        SwitchDataInputSelectionPane dataInputPane =
            new SwitchDataInputSelectionPane(Messages.dataInputString, EndpointType.INPUT, this, false,
                SwitchComponentConstants.DATA_INPUT_NAME);

        SwitchConditionInputSelectionPane conditionInputPane =
            new SwitchConditionInputSelectionPane(Messages.conditionInputString, EndpointType.INPUT, this, false,
                SwitchComponentConstants.CONDITION_INPUT_ID);

        EndpointSelectionPane dataOutputpane =
            new EndpointSelectionPane(Messages.dataOutputString, EndpointType.OUTPUT, this, true, null, false);

        
        setColumns(1);
        setPanes(dataInputPane, conditionInputPane, dataOutputpane);
        dataInputPane.setAllPanes(new EndpointSelectionPane[] {dataInputPane, conditionInputPane, dataOutputpane}); 
        
    }

}
