/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

        SwitchDataInputSelectionPane dataInputPane = new SwitchDataInputSelectionPane(this);

        SwitchConditionInputSelectionPane conditionInputPane =
            new SwitchConditionInputSelectionPane(Messages.conditionInputString, EndpointType.INPUT,
                SwitchComponentConstants.CONDITION_INPUT_ID, this);

        EndpointSelectionPane dataOutputpane =
            new EndpointSelectionPane(Messages.dataOutputString, EndpointType.OUTPUT, null, new String[] {},
                new String[] { SwitchComponentConstants.FALSE_OUTPUT, SwitchComponentConstants.TRUE_OUTPUT }, this, true);
        setColumns(1);
        setPanes(dataInputPane, conditionInputPane, dataOutputpane);
        dataInputPane.setAllPanes(new EndpointSelectionPane[] { dataInputPane, conditionInputPane, dataOutputpane });

    }

}
