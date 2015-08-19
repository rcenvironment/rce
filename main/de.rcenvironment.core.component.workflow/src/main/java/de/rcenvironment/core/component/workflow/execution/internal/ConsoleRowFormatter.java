/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Converts {@link ConsoleRow} entries to log file entries.
 * 
 * NOTE: This class is NOT thread-safe (for performance reasons).
 * 
 * @author Robert Mischke
 */
public final class ConsoleRowFormatter {

    private final DateFormat timeFormat = SimpleDateFormat.getDateTimeInstance(); // TODO 5.0: improve format

    /**
     * Returns a log entry to use in log files for a single, specific workflow execution.
     * 
     * @param row the {@link ConsoleRow} to format
     * @return the generated log entry
     */
    public String toSingleWorkflowLogFileFormat(ConsoleRow row) {
        String componentName = row.getComponentName();
        if (componentName == null || componentName.isEmpty()) {
            componentName = "-";
        }
        return StringUtils.format("[%s] [%s] [%s] %s%n", timeFormat.format(new Date(row.getTimestamp())), row.getType(), componentName,
            row.getPayload());
    }

    /**
     * Returns a log entry to use in a combined log file for multiple workflow executions.
     * 
     * @param row the {@link ConsoleRow} to format
     * @return the generated log entry
     */
    public String toCombinedLogFileFormat(ConsoleRow row) {
        return StringUtils.format("[%s] [%s] [%s] [%s] %s%n", new Date(row.getTimestamp()), row.getWorkflowName(), row.getComponentName(),
            row.getType(), row.getPayload());
    }
}
