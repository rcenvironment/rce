/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cluster.common;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Common constants for cluster component.
 * 
 * @author Doreen Seider
 */
public final class ClusterComponentConstants {

    /** Configuration key. */
    public static final String CONFIG_KEY_QUEUINGSYSTEM = "queuingSystem";
    
    /** Configuration key. */
    public static final String CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS = "pathToQueuingSystemCommands";
    
    /** Configuration key. */
    public static final String KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR = "isScriptProvided";

    /** Input name. */
    public static final String INPUT_JOBCOUNT = "Job count";
    
    /** Input name. */
    public static final String INPUT_JOBINPUTS = "Job inputs";
    
    /** Output name. */
    public static final String OUTPUT_JOBOUTPUTS = "Job outputs";

    /** Input name. */
    public static final String INPUT_SHAREDJOBINPUT = "Shared job input";

    /** Configuration key. */
    public static final String CLUSTER_FETCHING_FAILED = "cluster fetching failed";

    /** Component ID. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "cluster";
    
    /** Component ID. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID, 
        "de.rcenvironment.components.cluster.execution.ClusterComponent_Cluster" };

    /** Name of the job script. */
    public static final String JOB_SCRIPT_NAME = "run_cluster_job.sh";
    
    private ClusterComponentConstants() {}
    
    /**
     * Converts the plain property string (paths to the queuing system commands) into a map (key is command, value is path).
     * @param paths paths as property string. format: command=path;command=path;...
     * @return map with commands and their paths
     */
    public static Map<String, String> extractPathsToQueuingSystemCommands(String paths) {
        Map<String, String> pathMap = new HashMap<>();
        for (String path : paths.split(";")) {
            if (!path.isEmpty()) {
                String[] keyValue = path.split("=");
                if (keyValue.length == 2) {
                    pathMap.put(keyValue[0], keyValue[1]);                
                }
            }
        }
        return pathMap;
    }
    
    /**
     * Put map (key is command, value is path) to a plain property string (paths to the queuing system commands).
     * @param paths map with commands and their paths
     * @return property string. format: command=path;command=path;...
     */
    public static String getCommandsPathsAsPropertyString(Map<String, String> paths) {
        StringBuilder builder = new StringBuilder();
        for (String command : paths.keySet()) {
            builder.append(StringUtils.format("%s=%s;", command, paths.get(command)));
        }
        return builder.toString();
    }
    

}
