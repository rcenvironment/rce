/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility methods used during workflow execution.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Brigitte Boden
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

    private static final int PATTERN_DATETIME_LENGTH = 19;

    /**
     * Structure of this regular expression:<br>
     * 1. year<br>
     * \d{4} -> random number between 0 and 9 exactly 4 times. \- -> hyphen 2. month<br>
     * [01]{1} -> 0 or 1 (a year only has 12 months) \d{1} -> random number between 0 and 9 exactly once. \- -> hyphen 3. day<br>
     * [0123]{1} \d{1} -> random number between 0 and 9 exactly once. \_ -> underline character 4. hour<br>
     * [012]{1} \d -> random number between 0 and 9 exactly once. \: -> colon 5. minutes<br>
     * [0123456]{1} -> random number between 0 and 6 exactly once (a hour only has 60min) \d{1} -> random number between 0 and 9 exactly
     * once. \: -> colon 6. seconds<br>
     * [0123456]{1} -> random number between 0 and 6 exactly once (a minute only has 60sec) \d{1} -> random number between 0 and 9 exactly
     * once.
     * 
     */
    private static final String PATTERN_DATETIME_STRING =
        "\\d{4}\\-[01]{1}\\d{1}\\-[0123]{1}\\d{1}\\_[012]{1}\\d\\:[0123456]{1}\\d{1}\\:[0123456]{1}\\d{1}";

    private static final int PATTERN_NUMBER_LENGTH = 3;

    private static final String PATTERN_NUMBER_STRING = "^\\_\\d{2}";

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
        String wfDescriptionName = workflowDescription.getName();
        if (wfDescriptionName == null) {
            return generateWorkflowName(filename);
        }
        Pattern pattern = Pattern.compile(PATTERN_DATETIME_STRING);
        Matcher matcher = pattern.matcher(wfDescriptionName);
        if (!matcher.find()) {
            return wfDescriptionName;
        }
        int position = matcher.start();

        String dateAndTime = wfDescriptionName.substring(position, position + PATTERN_DATETIME_LENGTH);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            dateFormat.setLenient(false);
            dateFormat.parse(dateAndTime);
        } catch (ParseException e) {
            return wfDescriptionName;
        }

        String newName = wfDescriptionName.substring(0, position)
            + generateTimestampString();

        String rest = wfDescriptionName.substring(position + PATTERN_DATETIME_LENGTH);
        pattern = Pattern.compile(PATTERN_NUMBER_STRING);
        matcher = pattern.matcher(rest);
        if (matcher.find()) {
            newName = StringUtils.format("%s_%02d", newName, generateNewSuffixNumber());
            newName += rest.substring(PATTERN_NUMBER_LENGTH);
        } else {
            newName += rest;
        }

        return newName;
    }

    private static String generateWorkflowName(String filename) {

        String storedWorkflowName = filename;
        if (storedWorkflowName.toLowerCase().endsWith(".wf")) {
            storedWorkflowName = storedWorkflowName.substring(0, storedWorkflowName.length() - 3);
        }

        // make the last two digits sequentially increasing to reduce the likelihood of timestamp collisions
        return StringUtils.format("%s_%s_%02d", storedWorkflowName, generateTimestampString(), generateNewSuffixNumber());
    }

    private static int generateNewSuffixNumber() {
        return GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER.incrementAndGet() % WORKFLOW_SUFFIX_NUMBER_MODULO;
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
            throw new FileNotFoundException(StringUtils.format(errorString,
                StringUtils.format("\"%s\" (resolved to \"%s\")", filename, file.getAbsolutePath())));
        }
        return file;
    }

    private static String generateTimestampString() {
        // format: full date and time, connected with underscore
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        return dateFormat.format(new Date());
    }

    /**
     * Replaces null {@link InstanceNodeSessionId} for controller and components with local {@link InstanceNodeSessionId}.
     * 
     * @param wfDescription {@link WorkflowDescription}
     * @param localNodeId local {@link InstanceNodeSessionId}
     * @param compKnowledge latest {@link DistributedComponentKnowledge}
     * @throws WorkflowExecutionException if a component affected is not installed locally
     */
    public static void replaceNullNodeIdentifiersWithActualNodeIdentifier(WorkflowDescription wfDescription,
        LogicalNodeId localNodeId, DistributedComponentKnowledge compKnowledge)
        throws WorkflowExecutionException {

        for (WorkflowNode node : wfDescription.getWorkflowNodes()) {
            // replace null (representing localhost) with the actual host name
            // TODO review: can this actually still be null at this point?
            if (node.getComponentDescription().getNode() == null) {
                Collection<DistributedComponentEntry> installations = compKnowledge.getAllLocalInstallations();
                final String componentIdentifier = node.getComponentDescription().getIdentifier();
                ComponentInstallation installation = ComponentUtils.getExactMatchingComponentInstallationForNode(
                    componentIdentifier, installations, localNodeId);
                if (installation == null) {
                    throw new WorkflowExecutionException(StringUtils.format("Component '%s' (%s) not installed on node %s ",
                        node.getName(), componentIdentifier, node.getComponentDescription().getNode()));
                }
                node.getComponentDescription().setComponentInstallationAndUpdateConfiguration(installation);
                node.getComponentDescription().setIsNodeIdTransient(true);
            }
        }

        if (wfDescription.getControllerNode() == null) {
            wfDescription.setControllerNode(localNodeId);
            wfDescription.setIsControllerNodeIdTransient(true);
        }
    }

    /**
     * Set {@link InstanceNodeSessionId}s to transient if they point to the local node.
     * 
     * @param wfDescription {@link WorkflowDescription}
     * @param localNodeId local {@link InstanceNodeSessionId}
     */
    public static void setNodeIdentifiersToTransientInCaseOfLocalOnes(WorkflowDescription wfDescription,
        LogicalNodeId localNodeId) {

        for (WorkflowNode node : wfDescription.getWorkflowNodes()) {
            node.getComponentDescription().setIsNodeIdTransient(node.getComponentDescription().getNode() == null
                || node.getComponentDescription().getNode().equals(localNodeId));
        }

        wfDescription.setIsControllerNodeIdTransient(wfDescription.getControllerNode() == null
            || wfDescription.getControllerNode().equals(localNodeId));
    }

    /**
     * Removed disabled workflow nodes from given {@link WorkflowDescription}.
     * 
     * @param workflowDescription to remove the disabled {@link WorkflowNode}s from
     * @return {@link WorkflowDescription} without disabled {@link WorkflowNode}s
     */
    public static WorkflowDescription removeDisabledWorkflowNodesWithoutNotify(WorkflowDescription workflowDescription) {
        List<WorkflowNode> disabledWorkflowNodes = getDisabledWorkflowNodes(workflowDescription);
        if (!disabledWorkflowNodes.isEmpty()) {
            workflowDescription.removeWorkflowNodesAndRelatedConnectionsWithoutNotify(disabledWorkflowNodes);
        }
        return workflowDescription;
    }

    /**
     * Returns the workflow nodes that are disabled from given {@link WorkflowDescription}.
     * 
     * @param workflowDescription to remove the disabled {@link WorkflowNode}s from
     * @return list of {@link WorkflowNode}s disabled
     */
    public static List<WorkflowNode> getDisabledWorkflowNodes(WorkflowDescription workflowDescription) {
        List<WorkflowNode> nodes = new ArrayList<>();
        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            if (!node.isEnabled()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * @param wfNodes list of {@link WorkflowNode}s to check
     * @return <code>true</code> if one of the {@link WorkflowNode}s is considered as referring to a non-available component, otherwise
     *         <code>false</code>
     */
    public static boolean hasMissingWorkflowNode(List<WorkflowNode> wfNodes) {
        for (WorkflowNode wfNode : wfNodes) {
            if (wfNode.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param wfExeCtx {@link WorkflowExecutionContext} of related workflow
     * @return text containing workflow instance name and workflow execution id that can be used in log messages
     */
    public static String substituteWorkflowNameAndExeId(WorkflowExecutionContext wfExeCtx) {
        return StringUtils.format("workflow '%s' (%s)", wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier());
    }

}
