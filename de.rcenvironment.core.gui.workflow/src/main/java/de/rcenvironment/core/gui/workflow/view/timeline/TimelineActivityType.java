/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

/**
 * Contains the type of an activity.
 * @author Hendrik Abbenhaus
 */
public enum TimelineActivityType {

    /**
     * Component run.
     */
    COMPONENT_RUN("Component Run", "COMPONENT_RUN", new RGB(0xa6, 0x27, 0x36), new RGB(0xd3, 0x7a, 0x84)),
    
    /**
     * External tool run in a component run.
     */
    EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN("External Tool Run", "EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN",
        new RGB(0x39, 0x92, 0x22), new RGB(0x6d, 0xc8, 0x55)), // green
//        new RGB(0x35, 0x28, 0x78), new RGB(0x6d, 0x5d, 0xbb)), // blue
    
    /**
     * Component idling.
     */
    WAITING("Idle", "COMPONENT_WAIT", null, null);
    
    private static Map<String, TimelineActivityType> map = new HashMap<String, TimelineActivityType>();

    private String displayName = null;

    private String jsonName = null;

    private RGB color = null;

    private RGB previewColor = null;

    
    TimelineActivityType(String displayName, String jsonName, RGB color, RGB previewColor) {
        this.displayName = displayName;
        this.jsonName = jsonName;
        this.color = color;
        this.previewColor = previewColor;

    }
    
    static {
        for (TimelineActivityType t : TimelineActivityType.values()) {
            map.put(t.getJsonName(), t);
        }
    }
    /**
     * Reverse search-service: get the {@link TimelineActivityType} by it`s name.
     * @param jsonName the json Name of TimelineActivityType 
     * @return the activitytype
     */
    public static TimelineActivityType valueOfjsonName(String jsonName) {
        return map.get(jsonName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getJsonName() {
        return jsonName;
    }

    public RGB getColor() {
        return color;
    }

    public RGB getPreviewColor() {
        return previewColor;
    }

}
