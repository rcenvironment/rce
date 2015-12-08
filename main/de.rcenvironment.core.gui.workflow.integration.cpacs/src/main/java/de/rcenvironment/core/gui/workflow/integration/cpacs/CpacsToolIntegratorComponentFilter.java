/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.integration.cpacs;

import de.rcenvironment.core.component.integration.cpacs.CpacsToolIntegrationConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for CPACS integrated tool components.
 * 
 * @author Jan Flink
 */
public class CpacsToolIntegratorComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : CpacsToolIntegrationConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
