/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import de.rcenvironment.core.gui.palette.toolidentification.ToolType;

/**
 * Palette View constants.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 * 
 */
public final class PaletteViewConstants {
    

    /** Constant. */
    public static final String SELECT = "Select";

    /** Constant. */
    public static final String DRAW_CONNECTION = "Draw Connection";

    /** Constant. */
    public static final String ADD_LABEL = "Add Label";

    /** Constant. */
    public static final String ROOT_NODE_NAME = "<root>";

    /** Constant. */
    public static final String ESCAPED_GROUP_STRING_SEPERATOR = "\\/";

    /** Constant. */
    public static final String GROUP_STRING_SEPERATOR = "/";

    /** Constant. */
    public static final String DIR_CONFIGURATION_STORAGE = "paletteViewStorage";

    /** Constant. */
    public static final String FILE_TOOL_GROUP_ASSIGNMENT = "toolGroupAssignment.dat";

    /** Constant. */
    public static final String FILE_EXPANDED_GROUPS = "expandedGroups.dat";

    /** Constant. */
    public static final String FILE_CUSTOMIZED_GROUPS = "customizedGroups.dat";

    /** RCE standard group names. */
    public static final String[] RCE_GROUPS = { "CPACS", "Data", "Data Flow", "Evaluation", "Execution", "XML", "_Internal" };

    /**
     * RCE standard group paths.
     */
    public static final String[] RCE_STANDARD_GROUPS = {
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "CPACS",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "Data",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "Data Flow",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "Evaluation",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "Execution",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "XML",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "_Internal",
        ToolType.STANDARD_COMPONENT.getTopLevelGroupName() + GROUP_STRING_SEPERATOR + "_Examples",
    };

    private PaletteViewConstants() {}
}
