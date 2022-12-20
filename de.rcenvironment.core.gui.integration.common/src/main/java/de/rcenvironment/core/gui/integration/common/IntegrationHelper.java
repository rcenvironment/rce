/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Helper class for the integration of tools and workflows.
 * 
 * @author Kathrin Schaffert
 */
public class IntegrationHelper {

    private final ServiceRegistryAccess serviceRegistryAccess;

    private ToolIntegrationService integrationService;

    public IntegrationHelper() {
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        integrationService = serviceRegistryAccess.getService(ToolIntegrationService.class);
    }

    public List<String> updateGroupNames(IntegrationContextType contextType) {
        LogicalNodeId localNode = serviceRegistryAccess.getService(PlatformService.class)
            .getLocalDefaultLogicalNodeId();
        Collection<DistributedComponentEntry> installations = getInitialComponentKnowledge().getAllInstallations();
        installations = ComponentUtils.eliminateComponentInterfaceDuplicates(installations, localNode);
        List<String> groupNames = new ArrayList<>();
        for (DistributedComponentEntry ci : installations) {
            ComponentInterface componentInterface = ci.getComponentInterface();
            String toolID = componentInterface.getIdentifier();
            if (contextType.equals(IntegrationContextType.WORKFLOW)) {
                if (toolID.startsWith("de.rcenvironment.integration.workflow") || toolID.startsWith("workflow")) {
                    addGroupsAndSubgroupsToList(groupNames, componentInterface);
                }
            } else {
                if (toolID.startsWith("de.rcenvironment.integration.common") || toolID.startsWith("de.rcenvironment.integration.cpacs")
                    || toolID.startsWith("common") || toolID.startsWith("cpacs")) {
                    addGroupsAndSubgroupsToList(groupNames, componentInterface);
                }
            }
        }
        Collections.sort(groupNames, String.CASE_INSENSITIVE_ORDER);
        return groupNames;
    }

    private void addGroupsAndSubgroupsToList(List<String> groupNames, ComponentInterface componentInterface) {
        List<String> groupList = getAllSubgroups(componentInterface.getGroupName());
        for (String groupName : groupList) {
            if (!groupNames.contains(groupName)) {
                groupNames.add(groupName);
            }
        }
    }

    private List<String> getAllSubgroups(String groupPath) {
        List<String> strList = new ArrayList<>();
        strList.add(groupPath);
        while (groupPath.contains("/")) {
            int i = groupPath.lastIndexOf("/");
            groupPath = groupPath.substring(0, i);
            strList.add(groupPath);
        }
        return strList;
    }

    public DistributedComponentKnowledge getInitialComponentKnowledge() {
        DistributedComponentKnowledgeService registry = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class);
        return registry.getCurrentSnapshot();
    }

    public List<String> getAlreadyIntegratedComponentNames() {
        List<String> toolNames = new LinkedList<>();

        for (String id : integrationService.getIntegratedComponentIds()) {
            toolNames.add(
                integrationService.getToolConfiguration(id).get(IntegrationConstants.KEY_COMPONENT_NAME).toString());
        }
        return toolNames;
    }
    
    public Optional<File> tryFindConfigurationFile(IntegrationContext context, File toolFolder) {
        return Arrays.stream(toolFolder.listFiles())
            .filter(f -> f.getName().equals(context.getConfigurationFilename()))
            .findAny();
    }
}
