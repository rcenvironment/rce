/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.scpoutputcollector.gui;

import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;
import de.rcenvironment.components.scpoutputcollector.common.ScpOutputCollectorComponentConstants;;
/**
 * Componentfilter for ScpOutputCollector.
 * @author Brigitte Boden
 *
 */
public class ScpOutputCollectorComponentFilter extends ComponentFilter {
    
    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith(ScpOutputCollectorComponentConstants.COMPONENT_ID);
        
    }
    
}
