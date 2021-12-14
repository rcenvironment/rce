/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.palette.toolidentification;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool type identification for the palette view representing their corresponding top level groups.
 *
 * @author Jan Flink
 */
public enum ToolType {

    /**
     * Tool type for standard RCE components.
     */
    STANDARD_COMPONENT("Standard Component", "Standard Components"),
    /**
     * Tool type for user integrated tools.
     */
    INTEGRATED_TOOL("User Integrated Tool", "User Integrated Tools"),
    /**
     * Tool type for user integrated workflows.
     */
    INTEGRATED_WORKFLOW("User Integrated Workflow", "User Integrated Workflows");

    private String displayName;

    private String topLevelGroup;

    ToolType(String displayName, String topLevelGroup) {
        this.displayName = displayName;
        this.topLevelGroup = topLevelGroup;
    }

    public String getName() {
        return displayName;
    }

    public String getTopLevelGroupName() {
        return topLevelGroup;
    }

    public static List<String> getTopLevelGroupNames() {
        List<ToolType> typeList = new ArrayList<>(EnumSet.allOf(ToolType.class));
        return typeList.stream().map(ToolType::getTopLevelGroupName).collect(Collectors.toList());
    }
}
