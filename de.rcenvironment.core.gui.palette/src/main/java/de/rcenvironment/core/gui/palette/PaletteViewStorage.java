/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * 
 * Class to store the {@link ToolGroupAssignment} and the expanded Groups of the {@link PaletteView}.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class PaletteViewStorage {

    protected final ConfigurationService configService;

    private final PaletteViewContentProvider contentProvider;

    private final Log log = LogFactory.getLog(getClass());

    private Path assignmentFile;

    private Path expandedGroupsFile;

    private Path customizedGroupsFile;

    private ToolGroupAssignment assignment;

    public PaletteViewStorage(PaletteViewContentProvider contentProvider) {
        this.contentProvider = contentProvider;
        this.assignment = contentProvider.getAssignment();
        this.configService = ServiceRegistry.createAccessFor(this).getService(ConfigurationService.class);
        createStorageIfAbsent();
    }

    private void createStorageIfAbsent() {
        if (configService != null) {
            Path storeConfigurationPath = Paths.get(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA).toString(),
                PaletteViewConstants.DIR_CONFIGURATION_STORAGE);
            assignmentFile = Paths.get(storeConfigurationPath.toString(), PaletteViewConstants.FILE_TOOL_GROUP_ASSIGNMENT);
            expandedGroupsFile = Paths.get(storeConfigurationPath.toString(), PaletteViewConstants.FILE_EXPANDED_GROUPS);
            customizedGroupsFile = Paths.get(storeConfigurationPath.toString(), PaletteViewConstants.FILE_CUSTOMIZED_GROUPS);
            if (!storeConfigurationPath.toFile().exists()) {
                storeConfigurationPath.toFile().mkdir();
            }
            try {
                if (!assignmentFile.toFile().exists() && assignmentFile.toFile().createNewFile()) {
                    log.debug(StringUtils.format("Created customized assignments file: %s", assignmentFile.toFile().toString()));
                }
                if (!expandedGroupsFile.toFile().exists() && expandedGroupsFile.toFile().createNewFile()) {
                    log.debug(StringUtils.format("Created expanded groups file: %s", expandedGroupsFile.toFile().toString()));
                }
                if (!customizedGroupsFile.toFile().exists() && customizedGroupsFile.toFile().createNewFile()) {
                    log.debug(StringUtils.format("Created customized groups file: %s", customizedGroupsFile.toFile().toString()));
                }
            } catch (IOException e) {
                log.error("Error creating palette view storage files.", e);
            }
        } else {
            log.error(StringUtils.format("Could not create %s because ConfigurationService was not available.",
                PaletteViewConstants.DIR_CONFIGURATION_STORAGE));
        }
    }

    public List<String> loadFilesFromStorage() {
        loadCustomizedGroups();
        boolean assignmentLoaded = loadCustomizedAssignments();
        if (assignmentLoaded) {
            return loadExpandedGroups();
        }
        return new ArrayList<>();
    }

    protected boolean loadCustomizedAssignments() {

        if (!assignmentFileExists()) {
            return false;
        }

        Optional<List<String>> optional = readAssignmentFile();
        if (!optional.isPresent()) {
            return false;
        }

        log.debug(StringUtils.format("Loaded custom group assignments from file '%s'.", assignmentFile));
        List<String> lines = optional.get();

        boolean returnValue = false;
        try {
            Map<ToolIdentification, String[]> customizedAssignment = new HashMap<>();
            for (String line : lines) {
                String divider = ",";
                String[] splittedLine = line.split(";");
                String[] qualifiedGroupArray = splittedLine[1].split("/");
                String[] toolIdentificationString = splittedLine[0].split(divider);

                String toolID = toolIdentificationString[0];
                String displayName = toolIdentificationString[1];
                String type = toolIdentificationString[2];

                ToolIdentification toolIdentification;
                if (type.equals(ToolType.INTEGRATED_TOOL.toString())) {
                    toolIdentification =
                        ToolIdentification.createIntegratedToolIdentification(toolID, displayName);
                } else if (type.equals(ToolType.INTEGRATED_WORKFLOW.toString())) {
                    toolIdentification = ToolIdentification.createIntegratedWorkflowIdentification(toolID, displayName);
                } else {
                    toolIdentification = ToolIdentification.createStandardComponentIdentification(toolID, displayName);
                }
                customizedAssignment.put(toolIdentification, qualifiedGroupArray);
                log.debug(StringUtils.format("Restored custom group assignment: '%s' (%s) -> '%s'.",
                    displayName, toolID, splittedLine[1]));
            }
            assignment.setCustomizedAssignments(customizedAssignment);
            returnValue = true;
        } catch (PatternSyntaxException e) {
            log.warn(StringUtils.format(
                "Assignment of tools to groups could not be parsed correctly: %s. Default Palette View Configuration is loaded.",
                assignmentFile.toString()), e);
        }
        return returnValue;
    }

    protected Optional<List<String>> readAssignmentFile() {
        try {
            return Optional.of(Files.readAllLines(assignmentFile));
        } catch (IOException e) {
            log.error(
                StringUtils.format("Assignment of tools to groups could not be read: %s. "
                    + "Default Palette View Configuration is loaded.", assignmentFile.toString()),
                e);
            return Optional.empty();
        }
    }

    protected boolean assignmentFileExists() {
        return assignmentFile.toFile().exists();
    }

    private void loadCustomizedGroups() {
        if (!customizedGroupsFile.toFile().exists()) {
            log.error(StringUtils.format("Customized groups file '%s' does not exist.", customizedGroupsFile.toString()));
            return;
        }
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.readAllLines(customizedGroupsFile);
        } catch (IOException e) {
            log.error(StringUtils.format("Customized groups file: %s could not be read from profile dir: internal/%s",
                    customizedGroupsFile.toString(), PaletteViewConstants.DIR_CONFIGURATION_STORAGE), e);
        }
        log.debug(StringUtils.format("Loaded custom groups from file '%s'.", customizedGroupsFile));
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                String[] qualifiedGroupString = assignment.createPathArray(line.trim());
                PaletteTreeNode groupNode = contentProvider.getOrCreateGroupNode(qualifiedGroupString);
                groupNode.getGroupNode().setCustomGroup(true);
                log.debug(StringUtils.format("Restored custom group '%s'.", line.trim()));
            }
        }
    }

    private List<String> loadExpandedGroups() {
        if (!expandedGroupsFile.toFile().exists() || expandedGroupsFile.toFile().length() == 0) {
            return new ArrayList<>();
        }
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.readAllLines(expandedGroupsFile);
            log.debug(StringUtils.format("Loaded expanded groups from file '%s'.", expandedGroupsFile));
        } catch (IOException e) {
            log.error(StringUtils.format("Expanded groups could not be set. File: '%s' could not be read.",
                expandedGroupsFile), e);
        }
        try {
            String[] firstLine = lines.get(0).split(":");
            contentProvider.getPaletteView().setShowEmptyGroups(Boolean.valueOf(firstLine[1]));
            lines.remove(0);
        } catch (PatternSyntaxException e) {
            log.warn(
                "Configuration of \"Unhide empty groups\" on Palette View could not be parsed correctly. Checkbox is unchecked.");
        }
        return lines;
    }

    public void storeConfigurationFiles() {
        writeCustomizedAssignmentToFile();
        writeCustomizedGroupsFile();
        writeExpandedGroupsFile();
    }

    private void writeCustomizedAssignmentToFile() {
        List<String> linesToWrite = getAssignmentFileLinesToWrite();
        try {
            Files.deleteIfExists(assignmentFile);
            Files.write(assignmentFile, linesToWrite, StandardOpenOption.CREATE);
            log.debug(StringUtils.format("Stored group assignments to file '%s'.", assignmentFile));
        } catch (IOException e) {
            log.error(StringUtils.format(
                    "Assignment of tools to groups could not be saved. Could not write: %s into profile dir: internal/%s",
                    assignmentFile.toString(), PaletteViewConstants.DIR_CONFIGURATION_STORAGE), e);
        }
    }

    protected List<String> getAssignmentFileLinesToWrite() {
        Map<ToolIdentification, String[]> assignmentMap = assignment.getCustomizedAssignments();
        List<String> linesToWrite = new ArrayList<>();
        String divider = ",";
        for (Entry<ToolIdentification, String[]> entry : assignmentMap.entrySet()) {
            ToolIdentification toolIdentification = entry.getKey();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(toolIdentification.getToolID() + divider);
            stringBuilder.append(toolIdentification.getToolName() + divider);
            stringBuilder.append(toolIdentification.getType().toString());
            stringBuilder.append(";");
            String[] values = entry.getValue();
            stringBuilder.append(assignment.createQualifiedGroupName(values));
            linesToWrite.add(stringBuilder.toString());
            log.debug(StringUtils.format("Generated storage data for custom group assignment: '%s' (%s) -> '%s'",
                toolIdentification.getToolName(), toolIdentification.getToolID(), assignment.createQualifiedGroupName(values)));
        }
        return linesToWrite;
    }

    private void writeCustomizedGroupsFile() {
        PaletteTreeNode root = contentProvider.getRootNode();
        List<GroupNode> groups = root.getAllSubGroups().stream().filter(GroupNode::isCustomGroup).collect(Collectors.toList());
        List<String> linesToWrite = new ArrayList<>();
        for (GroupNode group : groups) {
            linesToWrite.add(group.getQualifiedGroupName().replace(PaletteViewConstants.ESCAPED_GROUP_STRING_SEPERATOR,
                PaletteViewConstants.GROUP_STRING_SEPERATOR));
            log.debug(StringUtils.format("Generated storage data for custom group '%s'.", group.getQualifiedGroupName()));
        }
        try {
            Files.deleteIfExists(customizedGroupsFile);
            Files.write(customizedGroupsFile, linesToWrite, StandardOpenOption.CREATE);
            log.debug(StringUtils.format("Stored custom groups to file '%s'", customizedGroupsFile));
        } catch (IOException e) {
            log.error(
                StringUtils.format("Customized groups could not be saved. Could not write file: %s", customizedGroupsFile.toString()), e);
        }
    }

    private void writeExpandedGroupsFile() {
        List<String> linesToWrite = new ArrayList<>();
        linesToWrite.add("Unhide empty groups checked:" + contentProvider.getPaletteView().isShowEmptyGroups());
        linesToWrite.addAll(contentProvider.getPaletteView().getExpandedGroupNameList());
        try {
            Files.deleteIfExists(expandedGroupsFile);
            Files.write(expandedGroupsFile, linesToWrite, StandardOpenOption.CREATE);
            log.debug(StringUtils.format("Stored expanded groups to file '%s'", expandedGroupsFile));
        } catch (IOException e) {
            log.error(
                StringUtils.format("Expanded groups could not be saved. Could not write file: %s", expandedGroupsFile.toString()), e);
        }
    }

}
