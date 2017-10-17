/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import de.rcenvironment.core.instancemanagement.InstanceManagementService.ConfigurationFlag;

/**
 * A configuration change entry.
 * 
 * @author Robert Mischke (redesign of ConfigurationPropertiesKey<T> by David Scholz)
 */
public class ConfigurationChangeEntry {

    private final ConfigurationFlag flag;

    private final Class<?> type;

    private final Object value;

    public ConfigurationChangeEntry(ConfigurationFlag flag, Class<?> type, Object value) {
        this.flag = flag;
        this.type = type;
        this.value = value;
    }

    public Class<?> getType() {
        return type;
    }

    public ConfigurationFlag getFlag() {
        return flag;
    }

    public Object getValue() {
        return value;
    }
}
