/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.outputwriter.gui;

import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
/**
 * Componentfilter for Outputwriter.
 * @author Hendrik Abbenhaus
 *
 */
public class OutputWriterComponentFilter extends ComponentFilter {
    
    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith(OutputWriterComponentConstants.COMPONENT_ID);
        
    }
    
}
