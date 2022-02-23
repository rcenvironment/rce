/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.view.PaletteView;

/**
 * Class to assign tools to its groups/subgroups to display the {@link PaletteView}'s tree.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class ToolGroupAssignment {


    private Map<ToolIdentification, String[]> customizedAssignments;

    public ToolGroupAssignment() {
        this.customizedAssignments = new HashMap<>();
    }


    public String[] createPathArray(String groupName) {
        return groupName.split(PaletteViewConstants.GROUP_STRING_SEPERATOR);
    }
    
    public String createQualifiedGroupName(String[] pathArray) {
        return String.join(PaletteViewConstants.GROUP_STRING_SEPERATOR, pathArray);
    }


    public Map<ToolIdentification, String[]> getCustomizedAssignments() {
        return customizedAssignments;
    }

    public void setCustomizedAssignments(Map<ToolIdentification, String[]> customizedAssignments) {
        this.customizedAssignments = (HashMap<ToolIdentification, String[]>) customizedAssignments;
    }


}
