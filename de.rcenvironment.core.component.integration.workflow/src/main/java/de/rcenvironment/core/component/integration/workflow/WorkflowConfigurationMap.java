/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.workflow;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * 
 * Extension of the {@link ConfigurationMap} to handle workflow specific configurations.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class WorkflowConfigurationMap extends ConfigurationMap {

 // This functionality will be added in a future release. 
 // K.Schaffert, 27.07
//    public boolean isAddImageFile() {
//        return (Boolean) this.rawConfigurationMap.getOrDefault(WorkflowIntegrationConstants.KEY_WORKFLOW_IMAGE_FILE, false);
//    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getEndpointAdapters() throws JsonProcessingException {
        return JsonUtils.getDefaultObjectMapper().readValue(
            (String) this.rawConfigurationMap.getOrDefault(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTERS, ""),
            LinkedList.class);
    }
}
