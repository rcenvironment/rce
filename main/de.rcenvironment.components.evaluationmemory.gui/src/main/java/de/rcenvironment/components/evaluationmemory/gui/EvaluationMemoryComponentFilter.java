/*
2 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for the Evaluation Memory Check component.
 *
 * @author Doreen Seider
 */
public class EvaluationMemoryComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith(EvaluationMemoryComponentConstants.COMPONENT_ID);
    }

}
