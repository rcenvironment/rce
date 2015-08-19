/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Utility methods used during workflow execution.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public final class WorkflowExecutionUtils {

    /**
     * Common error message template for {@link #resolveWorkflowOrPlaceholderFileLocation(String, String)}.
     */
    public static final String DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_PLACEHOLDER_FILE =
        "Placeholder file %s does not exist or it can not be read";

    /**
     * Common error message template for {@link #resolveWorkflowOrPlaceholderFileLocation(String, String)}.
     */
    public static final String DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_WORKFLOW_FILE =
        "Workflow file %s does not exist or it can not be read";

    private static final int WORKFLOW_SUFFIX_NUMBER_MODULO = 100;

    private static final AtomicInteger GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER = new AtomicInteger();

    private WorkflowExecutionUtils() {};

    /**
     * Generates a new default name for the workflow, which will be executed now.
     * 
     * @param filename *.wf file
     * @param workflowDescription {@link WorkflowDescription} of the workflow
     * @return the generated default name including the timestamp
     */
    public static String generateDefaultNameforExecutingWorkflow(String filename, WorkflowDescription workflowDescription) {
        // determine the clean "root" name for the workflow
        String storedWorkflowName = workflowDescription.getName();
        if (storedWorkflowName == null || storedWorkflowName.isEmpty()) {
            // if no previous name was stored, use the name of workflow file without ".wf" extension
            storedWorkflowName = filename;
            if (storedWorkflowName.toLowerCase().endsWith(".wf")) {
                storedWorkflowName = storedWorkflowName.substring(0, storedWorkflowName.length() - 3);
            }
        } else {
            // if a previous name was stored, clean it of any previous timestamp
            storedWorkflowName = storedWorkflowName.replaceFirst("^(.*)_\\d+-\\d+-\\d+_\\d+:\\d+:\\d+(_\\d+)?$", "$1");
        }

        // make the last two digits sequentially increasing to reduce the likelihood of timestamp collisions
        int suffixNumber = GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER.incrementAndGet() % WORKFLOW_SUFFIX_NUMBER_MODULO;
        return String.format("%s_%s_%02d", storedWorkflowName, generateTimestampString(), suffixNumber);
    }

    /**
     * Resolves a user-given path to a workflow file to its actual location. This is mostly intended for workflow execution via console
     * commands (including headless batch execution using "--exec").
     * 
     * @param filename the given workflow filename/path
     * @param errorString the template for the exception message if the file can not be read
     * @return an absolute {@link File} pointing to the resolved workflow file, if it exists
     * @throws FileNotFoundException if the given file cannot be resolved (usually, because it does not exist), or if it cannot be read
     */
    public static File resolveWorkflowOrPlaceholderFileLocation(String filename, String errorString) throws FileNotFoundException {
        // TODO trivial implementation; improve with paths relative to workspace etc. - misc_ro
        File file = new File(filename).getAbsoluteFile();
        // validate
        if (!file.isFile() || !file.canRead()) {
            throw new FileNotFoundException(String.format(errorString,
                String.format("\"%s\" (resolved to \"%s\")", filename, file.getAbsolutePath())));
        }
        return file;
    }

    private static String generateTimestampString() {
        // format: full date and time, connected with underscore
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        return dateFormat.format(new Date());
    }
}
