/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.sql.gui.properties;

import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for SqlComponent instances.
 * 
 * @author Christian Weiss
 */
public class SqlComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        return new SqlCommandComponentFilter().filterComponentName(componentId)
                || new SqlReaderComponentFilter().filterComponentName(componentId)
                || new SqlWriterComponentFilter().filterComponentName(componentId);
    }

}
