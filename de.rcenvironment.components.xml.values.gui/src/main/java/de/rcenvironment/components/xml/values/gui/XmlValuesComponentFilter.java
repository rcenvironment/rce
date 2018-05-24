/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.values.gui;

import de.rcenvironment.components.xml.values.common.XmlValuesComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for XML Values component.
 *
 * @author Adrian Stock
 */

public class XmlValuesComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : XmlValuesComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
