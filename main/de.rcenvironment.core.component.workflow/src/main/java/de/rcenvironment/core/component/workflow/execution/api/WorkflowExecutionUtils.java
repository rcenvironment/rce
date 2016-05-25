/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;

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

        if (workflowDescription.getName() == null) {
            return generateWorkflowName(filename);
        }

        if (workflowDescription.getName().contains("_20")) {

            int index = workflowDescription.getName().indexOf("_20");

            try {
                String dateAndNumber = workflowDescription.getName().substring(index + 1);

                if (dateAndNumber.contains("_")) {

                    int indexOfunderLine = dateAndNumber.lastIndexOf("_");
                    String dateOnly = dateAndNumber.substring(0, indexOfunderLine);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                    dateFormat.setLenient(false);
                    dateFormat.parse(dateOnly);

                }

                return generateWorkflowName(filename);

            } catch (ParseException e) {
                return workflowDescription.getName();
            }

        }

        return workflowDescription.getName();

    }

    private static String generateWorkflowName(String filename) {

        String storedWorkflowName = filename;
        if (storedWorkflowName.toLowerCase().endsWith(".wf")) {
            storedWorkflowName = storedWorkflowName.substring(0, storedWorkflowName.length() - 3);
        }

        // make the last two digits sequentially increasing to reduce the likelihood of timestamp collisions
        int suffixNumber = GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER.incrementAndGet() % WORKFLOW_SUFFIX_NUMBER_MODULO;
        return StringUtils.format("%s_%s_%02d", storedWorkflowName, generateTimestampString(), suffixNumber);
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
     * Replaces null {@link NodeIdentifier} for controller and components with local {@link NodeIdentifier}.
     * 
     * @param wfDescription {@link WorkflowDescription}
     * @param localNodeId local {@link NodeIdentifier}
     * @param compKnowledge latest {@link DistributedComponentKnowledge}
     * @throws WorkflowExecutionException if a component affected is not installed locally
     */
    public static void replaceNullNodeIdentifiersWithActualNodeIdentifier(WorkflowDescription wfDescription,
        NodeIdentifier localNodeId, DistributedComponentKnowledge compKnowledge)
        throws WorkflowExecutionException {

        for (WorkflowNode node : wfDescription.getWorkflowNodes()) {
            // replace null (representing localhost) with the actual host name
            if (node.getComponentDescription().getNode() == null) {
                Collection<ComponentInstallation> installations = compKnowledge.getLocalInstallations();
                ComponentInstallation installation = ComponentUtils.getExactMatchingComponentInstallationForNode(
                    node.getComponentDescription().getIdentifier(), installations, localNodeId);
                if (installation == null) {
                    throw new WorkflowExecutionException(StringUtils.format("Component '%s' not installed on node %s "
                        + node.getName(), node.getComponentDescription().getNode()));
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
     * Set {@link NodeIdentifier}s to transient if they point to the local node.
     * 
     * @param wfDescription {@link WorkflowDescription}
     * @param localNodeId local {@link NodeIdentifier}
     */
    public static void setNodeIdentifiersToTransientInCaseOfLocalOnes(WorkflowDescription wfDescription,
        NodeIdentifier localNodeId) {

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
     * @param wfExeCtx {@link WorkflowExecutionContext} of related workflow
     * @return text containing workflow instance name and workflow execution id that can be used in log messages
     */
    public static String substituteWorkflowNameAndExeId(WorkflowExecutionContext wfExeCtx) {
        return String.format("workflow '%s' (%s)", wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier());
    }

}
