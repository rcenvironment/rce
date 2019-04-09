/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * 
 * Filter for the switch component.
 *
 * @author David Scholz
 */
public class SwitchComponentFilter extends ComponentFilter {

    public SwitchComponentFilter() {

    }

    @Override
    public boolean filterComponentName(String componentId) {

        return componentId.startsWith(SwitchComponentConstants.COMPONENT_ID);
    }

}
