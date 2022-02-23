/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;

class ToolGroupAssignmentMock extends ToolGroupAssignment {


    protected ToolGroupAssignmentMock(PaletteViewContentProviderMock contentProvider) {
        super();
    }

    protected ToolIdentification addToolIdentificationCustomizedAssignment(String installationID,
        String toolName, ToolType toolType, String groupName) {
        ToolIdentification identification =
            new ToolIdentificationMock(installationID, toolName, toolType);
        String[] qualifiedGroupString = new String[1];
        qualifiedGroupString[0] = groupName;

        this.getCustomizedAssignments().put(identification, qualifiedGroupString);

        return identification;
    }

    protected void assertCustomizedAssignmentContainsEntry(ToolIdentification identification, String[] qualifiedGroupString) {
        assertTrue(this.getCustomizedAssignments().containsKey(identification));
        assertArrayEquals(qualifiedGroupString, this.getCustomizedAssignments().get(identification));
    }

}
