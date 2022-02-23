/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;

class ToolIdentificationMock extends ToolIdentification {

    protected ToolIdentificationMock(String toolID, String toolName, ToolType toolType) {
        super(toolID, toolName, toolType);
    }

}
