/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.properties;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;


/**
 * Filter for Destination instances.
 *
 * @author Markus Kunde
 * @author Arne Bachmann
 */
public class CpacsWriterComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(final String componentId) {
        boolean isMatch = false;
        for (String id : CpacsWriterComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
