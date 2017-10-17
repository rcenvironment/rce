/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.gui.properties;

import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputSelectionPane;

/**
 * An extended "Properties" view tab for configuring endpoints (ie inputs and outputs) and using initial Variables.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class DOEEndpointPropertySection extends EndpointPropertySection {

    public DOEEndpointPropertySection() {
        super();

        EndpointSelectionPane inputPane =
            new EndpointSelectionPane("Inputs (evaluation results received from loop)", EndpointType.INPUT, "default",
                new String[] {}, new String[] {}, this, false, true);

        DOEEndpointSelectionPane outputPane = new DOEEndpointSelectionPane("Outputs (values to evaluate)", this);

        EndpointSelectionPane startInputPane =
            new EndpointSelectionPane("Start Inputs", EndpointType.INPUT, "startTable",
                new String[] {}, new String[] {}, this, EndpointSelectionPane.NAME_AND_TYPE_READ_ONLY, true);

        EndpointSelectionPane outputForwardedPane =
            new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                new String[] {}, new String[] {}, this, true, true);

        InputCoupledWithAnotherInputAndOutputSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputSelectionPane("Inputs (to forward)", LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this,
                outputForwardedPane);

        EndpointSelectionPane outputPaneOthers = new EndpointSelectionPane("Outputs (other)", EndpointType.OUTPUT, null,
            new String[] {}, new String[] { LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, DOEConstants.OUTPUT_NAME_NUMBER_OF_SAMPLES },
            this, true, true);

        setColumns(2);
        setPanes(inputPane, outputPane, startInputPane, inputToForwardPane, outputForwardedPane, outputPaneOthers);
    }
}
