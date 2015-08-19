/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.regex.Pattern;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Acceptance filter for {@link ConsoleRow}s. Use {@link #accept(ConsoleRow)} to apply the filter criteria. This class is mutable; use the
 * {@link #clone()} method to create independent copies.
 * 
 * @author Robert Mischke
 */
public class ConsoleRowFilter implements Cloneable {

    // workflow filter value; null disables filtering
    private String workflow;

    // component filter value; null disables filtering
    private String component;

    // include workflow events of type "component finish"?
    private boolean includeLifecycleEvents;

    // include "meta info" type rows?
    private boolean includeMetaInfo;

    // include "stdout" type rows?
    private boolean includeStdout;

    // include "stderr" type rows?
    private boolean includeStderr;

    // regular expression representing the text filter; null disables filtering
    private Pattern searchTermPattern;

    /**
     * Constructs a default filter that accepts all row types and leaves all other filter values at "null".
     */
    public ConsoleRowFilter() {
        includeLifecycleEvents = true;
        includeMetaInfo = true;
        includeStderr = true;
        includeStdout = true;
    }

    /**
     * Set the workflow filter value; null disables filtering.
     * 
     * @param workflow The workflow to set.
     */
    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    /**
     * Set the component filter value; null disables filtering.
     * 
     * @param component The component to set.
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * Set whether "meta info" type lines should be included.
     * 
     * @param includeMeta The includeMeta to set.
     */
    public void setIncludeMetaInfo(boolean includeMeta) {
        this.includeMetaInfo = includeMeta;
    }
    
    /**
     * Set whether "life cycle" type lines should be included.
     * 
     * @param includeLifecycle The includeLifecycle to set.
     */
    public void setIncludeLifecylceEvents(boolean includeLifecycle) {
        this.includeLifecycleEvents = includeLifecycle;
    }

    /**
     * Set whether "stdout" type lines should be included.
     * 
     * @param includeStdout The includeStdout to set.
     */
    public void setIncludeStdout(boolean includeStdout) {
        this.includeStdout = includeStdout;
    }

    /**
     * Set whether "stderr" type lines should be included.
     * 
     * @param includeStderr The includeStderr to set.
     */
    public void setIncludeStderr(boolean includeStderr) {
        this.includeStderr = includeStderr;
    }

    /**
     * Set the search term filter; null or an empty string disables filtering.
     * 
     * @param searchTerm The searchTerm to set.
     */
    public void setSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.length() == 0) {
            searchTermPattern = null;
            return;
        }

        // escape all regexp-relevant characters
        searchTerm = searchTerm.replaceAll("(\\(|\\)|\\[|\\]|\\.|\\*|\\\\|\\^|\\$|\\||\\?|\\+|\\{|\\})", "\\\\$1");

        // compile the sanitized string to a case-insensitive regexp
        searchTermPattern = Pattern.compile(searchTerm, Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param row the {@link ConsoleRow} to decide on
     * @return true if the {@link ConsoleRow} matches this filter
     */
    public boolean accept(ConsoleRow row) {

        // Note that this method is slightly awkward due to the Checkstyle rule of max. 3 returns.
        boolean accept;

        // check type
        switch (row.getType()) {
        case STDOUT:
            accept = includeStdout;
            break;
        case STDERR:
            accept = includeStderr;
            break;
        case COMPONENT_OUTPUT:
            accept = includeMetaInfo;
            break;
        case LIFE_CYCLE_EVENT:
            // TODO not an elegant solution; improve? - misc_ro
            if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name())) {
                return false;
            } else if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED.name())) {
                accept = includeLifecycleEvents;
            } else {
                accept = false;
            }
            break;
        default:
            accept = false;
        }

        // check workflow
        if (accept && workflow != null && !workflow.equals(row.getWorkflowName())) {
            // workflow filter is set but did not match
            accept = false;
        }
        // check component
        if (accept && component != null && !component.equals(row.getComponentName())) {
            // component filter is set but did not match
            accept = false;
        }
        // check search pattern if set
        if (accept && searchTermPattern != null) {
            if (!searchTermPattern.matcher(row.getPayload()).find()) {
                // does not contain search term
                accept = false;
            }
        }
        return accept;
    }

    @Override
    public ConsoleRowFilter clone() {
        ConsoleRowFilter clone = new ConsoleRowFilter();
        clone.component = component;
        clone.workflow = workflow;
        clone.includeMetaInfo = includeMetaInfo;
        clone.includeLifecycleEvents = includeLifecycleEvents;
        clone.includeStderr = includeStderr;
        clone.includeStdout = includeStdout;
        // Pattern is immutable, so this is safe
        clone.searchTermPattern = searchTermPattern;
        return clone;
    }

}
