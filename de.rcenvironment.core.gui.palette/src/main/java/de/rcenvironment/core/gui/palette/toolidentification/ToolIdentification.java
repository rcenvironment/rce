/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette.toolidentification;

import java.util.Optional;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;

/**
 * Tool identification parameter for the palette view.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class ToolIdentification {

    private static final String INTEGRATION_ID_PREFIX = "de.rcenvironment.integration.";

    private static final String WORKFLOW_ID_PREFIX = "de.rcenvironment.integration.workflow";

    private String toolID;

    private String toolName;

    private ToolType toolType;

    private Optional<String> integrationType = Optional.empty();

    protected ToolIdentification(String toolID, String toolName, ToolType toolType) {
        this.toolID = toolID;
        this.toolName = toolName;
        this.toolType = toolType;
    }

    public static ToolIdentification createToolIdentification(DistributedComponentEntry componentEntry) {

        String toolID = componentEntry.getComponentInterface().getIdentifierAndVersion();
        String toolName = componentEntry.getDisplayName();
        ToolType toolType;
        Optional<String> integrationType = Optional.empty();

        if (toolID.startsWith(WORKFLOW_ID_PREFIX)) {
            toolType = ToolType.INTEGRATED_WORKFLOW;
        } else if (toolID.startsWith(INTEGRATION_ID_PREFIX)) {
            toolType = ToolType.INTEGRATED_TOOL;
            integrationType =
                Optional.of(
                    toolID.substring(INTEGRATION_ID_PREFIX.length(), toolID.indexOf(".", INTEGRATION_ID_PREFIX.length())));
        } else {
            toolType = ToolType.STANDARD_COMPONENT;
        }
        ToolIdentification identification = new ToolIdentification(toolID, toolName, toolType);
        if (integrationType.isPresent()) {
            identification.setIntegrationType(integrationType);
        }

        return identification;

    }

    public static ToolIdentification createIntegratedToolIdentification(String toolID, String toolName) {
        return new ToolIdentification(toolID, toolName, ToolType.INTEGRATED_TOOL);
    }

    public static ToolIdentification createIntegratedWorkflowIdentification(String toolID, String toolName) {
        return new ToolIdentification(toolID, toolName, ToolType.INTEGRATED_WORKFLOW);
    }

    public static ToolIdentification createStandardComponentIdentification(String toolID, String toolName) {
        return new ToolIdentification(toolID, toolName, ToolType.STANDARD_COMPONENT);
    }

    public String getToolID() {
        return toolID;
    }

    public String getToolName() {
        return toolName;
    }

    public ToolType getType() {
        return toolType;
    }

    @Override
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(getToolName());
        builder.append(getType());
        builder.append(ComponentUtils.getComponentInterfaceIdentifierWithoutVersion(getToolID()));
        // Since components with different versions
        // are not handled as different components within
        // the workflow, ToolIdentifications are also equal
        // with different versions. Needs to be fixed if
        // version handling will be improved generally - jf 09.21
        return builder.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ToolIdentification)) {
            return false;
        }
        ToolIdentification other = (ToolIdentification) obj;
        if (getToolName() == null) {
            if (other.getToolName() != null) {
                return false;
            }
        } else if (!getToolName().equals(other.getToolName())) {
            return false;
        }
        if (getType() == null) {
            if (other.getType() != null) {
                return false;
            }
        } else if (!getType().equals(other.getType())) {
            return false;
        }
        if (toolID == null) {
            if (other.toolID != null) {
                return false;
            }
        } else if (!ComponentUtils.getComponentInterfaceIdentifierWithoutVersion(toolID)
            .equals(ComponentUtils.getComponentInterfaceIdentifierWithoutVersion(other.toolID))) {
            // Since components with different versions
            // are not handled as different components within
            // the workflow, ToolIdentifications are also equal
            // with different versions. Needs to be fixed if
            // version handling will be improved generally - jf 09.21
            return false;
        }
        return true;
    }

    public Optional<String> getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(Optional<String> integrationType) {
        this.integrationType = integrationType;
    }

}
