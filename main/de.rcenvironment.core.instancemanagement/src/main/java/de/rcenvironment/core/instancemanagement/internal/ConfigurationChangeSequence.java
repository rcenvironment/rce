/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sequence of {@link ConfigurationChangeEntry} instances.
 * 
 * Introduced for migration from previous code; could also be handled with a simple List.
 * 
 * @author Robert Mischke
 */
public class ConfigurationChangeSequence {

    private final List<ConfigurationChangeEntry> entries = new ArrayList<>();

    /**
     * Appends an entry.
     * 
     * @param entry the configuration entry to append
     */
    public void append(ConfigurationChangeEntry entry) {
        entries.add(entry);
    }

    public List<ConfigurationChangeEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }
}
