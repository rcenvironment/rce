/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

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

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss,SSS");

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
        return StringUtils.format("[%s] [%s] [%s] [%s] %s%n", timeFormat.format(new Date(row.getTimestamp())), row.getWorkflowName(),
            row.getComponentName(), row.getType(), row.getPayload());
    }

    /**
     * Formats a simple "meta information" text line.
     * 
     * @param content the text content to format
     * @return the generated log entry
     */
    public String toMetaInformationLine(String content) {
        return StringUtils.format("[%s] [META] [-] %s%n", timeFormat.format(new Date()), content);
    }

    /**
     * Returns a log entry to use in an error log file for one single workflow execution.
     * 
     * @param row the {@link ConsoleRow} to format
     * @return the generated log entry
     */
    public String toWorkflowErrorLogFileFormat(ConsoleRow row) {
        if (row.getType().equals(ConsoleRow.Type.WORKFLOW_ERROR)) {
            return StringUtils.format("%s %s: %s", timeFormat.format(row.getTimestamp()),
                row.getType().getDisplayName(), row.getPayload());
        } else {
            if (row.getComponentRun() > 0) {
                return StringUtils.format("%s %s - %s [run %d]: %s", timeFormat.format(row.getTimestamp()),
                    row.getType().getDisplayName(), row.getComponentName(), row.getComponentRun(), row.getPayload());
            } else {
                return StringUtils.format("%s %s - %s: %s", timeFormat.format(row.getTimestamp()),
                    row.getType().getDisplayName(), row.getComponentName(), row.getPayload());
            }
        }
    }

    /**
     * Returns a log entry to use in a complete log file for one single component execution.
     * 
     * @param row the {@link ConsoleRow} to format
     * @return the generated log entry
     */
    public String toComponentCompleteLogFileFormat(ConsoleRow row) {
        return StringUtils.format("[%d] %s %s: %s", row.getSequenzNumber(), timeFormat.format(row.getTimestamp()),
            row.getType().getDisplayName(), row.getPayload());
    }

    /**
     * Returns a log entry to use in a complete log file for one single component execution.
     * 
     * @param row the {@link ConsoleRow} to format
     * @return the generated log entry
     */
    public String toComponentErrorLogFileFormat(ConsoleRow row) {
        return StringUtils.format("%s %s: %s", timeFormat.format(row.getTimestamp()),
            row.getType().getDisplayName(), row.getPayload());
    }
}
