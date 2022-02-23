/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.joiner.execution;

import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Component to join n inputs to 1.
 *
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class JoinerComponent extends DefaultComponent {

    private ComponentContext componentContext;
    
    private Set<String> indefiniteTDsSent = new HashSet<>();
    
    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }
    
    @Override
    public void processInputs() throws ComponentException {
        for (String inputName : componentContext.getInputsWithDatum()) {
            TypedDatum datum = componentContext.readInput(inputName);
            if (datum.getDataType().equals(DataType.NotAValue)) {
                if (indefiniteTDsSent.contains(((NotAValueTD) datum).getIdentifier())) {
                    continue;
                } else {
                    indefiniteTDsSent.add(((NotAValueTD) datum).getIdentifier());
                }
            }
            TypedDatum inputValue = componentContext.readInput(inputName);
            componentContext.writeOutput(JoinerComponentConstants.OUTPUT_NAME, inputValue);
            componentContext.getLog().componentInfo(StringUtils.format("Forwarded (joined) from input '%s': %s", inputName, inputValue));
        }
    }

}
