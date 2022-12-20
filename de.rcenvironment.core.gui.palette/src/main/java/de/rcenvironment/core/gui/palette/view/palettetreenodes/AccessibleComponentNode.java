/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.integration.toolintegration.ShowIntegrationEditWizardHandler;
import de.rcenvironment.core.gui.integration.workflowintegration.handlers.EditWorkflowIntegrationHandler;
import de.rcenvironment.core.gui.palette.PaletteViewConstants;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.PaletteCreationTool;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.WorkflowNodeFactory;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link PaletteTreeNode} implementation for workflow component nodes.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class AccessibleComponentNode extends ComponentNode {

    private static final String DEPRECATED_STRING =
        "Component Status 'DEPRECATED': This component will no longer be available in future versions of RCE.";

    private static final String AUTHENTICATION_GROUP_STRING = "Accessible via group(s): %s";

    private static final String INTEGRATION_TYPE_STRING = "Component Integration Type: %s";

    private static final String LOCAL_EXECUTION_ONLY_STRING = "Local Execution: This component is only available for local execution.";

    private static final String TYPE_STRING = "Component Type: %s";

    private static final String COMPONENT_STRING = "Component Name: %s";

    private static final String VERSION_STRING = "Component Version: %s";

    private static final String LOCATION_STRING = "Component Location: %s";

    private static final String DEFAULT_GROUP_STRING = "Default Group: %s";

    private static final String LOOP_DRIVER_STRING = "Loop Driver: This component is a driver workflow component.";

    private static final String STRING_LOCAL = "Local";

    private static final String STRING_REMOTE = "Remote";

    private static final Object STRING_MAPPED = "Remote (via Uplink or SSH connection)";

    private static final String LINE_BREAK = "\n";

    private final DistributedComponentEntry componentEntry;

    private PaletteCreationTool tool;

    public AccessibleComponentNode(PaletteTreeNode parent, DistributedComponentEntry toolEntry, ToolIdentification toolIdentification) {
        super(parent, toolIdentification);
        ComponentInterface componentInterface = toolEntry.getComponentInstallation().getComponentInterface();
        if (componentInterface.getIcon16() != null) {
            Image toolImage = new Image(Display.getCurrent(), new ByteArrayInputStream(componentInterface.getIcon16()));
            setIcon(toolImage);
        }
        this.componentEntry = toolEntry;
    }

    @Override
    public Optional<Image> getIcon() {
        Optional<Image> icon = super.getIcon();
        if (icon.isPresent() && componentEntry.getComponentInterface().getIsDeprecated()) {
            return Optional.of(getOverlayIcon(icon.get()));
        }
        return icon;
    }

    private Image getOverlayIcon(Image icon) {
        DecorationOverlayIcon overlayIcon =
            new DecorationOverlayIcon(icon,
                ImageManager.getInstance().getImageDescriptor(StandardImages.DECORATOR_DEPRECATED), IDecoration.BOTTOM_LEFT);
        return overlayIcon.createImage();
    }

    @Override
    public boolean isLocal() {
        return componentEntry.getType().isLocal() && !isMapped();
    }

    public boolean isMapped() {
        return componentEntry.getComponentInstallation().isMappedComponent();
    }

    public DistributedComponentEntry getComponentEntry() {
        return componentEntry;
    }

    @Override
    public void handleEditEvent() {
        if (getToolIdentification().getType().equals(ToolType.INTEGRATED_TOOL)) {
            ShowIntegrationEditWizardHandler handler = new ShowIntegrationEditWizardHandler(this.getNodeName());
            try {
                handler.execute(new ExecutionEvent());
            } catch (ExecutionException e) {
                LogFactory.getLog(PaletteTreeNode.class).error("Opening Tool Edit wizard failed", e);
            }
        } else if (getToolIdentification().getType().equals(ToolType.INTEGRATED_WORKFLOW)) {
            EditWorkflowIntegrationHandler handler = new EditWorkflowIntegrationHandler(this.getNodeName());
            try {
                handler.execute(null);
            } catch (ExecutionException e) {
                LogFactory.getLog(PaletteTreeNode.class).error("Opening Edit Workflow failed", e);
            }
        } else {
            throw new UnsupportedOperationException(
                StringUtils.format("Unexpected edit event on %s", this.getClass().getCanonicalName()));
        }

    }

    @Override
    public boolean canHandleEditEvent() {
        return (getToolIdentification().getType().equals(ToolType.INTEGRATED_TOOL)
            || getToolIdentification().getType().equals(ToolType.INTEGRATED_WORKFLOW)) && this.isLocal();
    }

    @Override
    public String getDisplayName() {
        // As long as we do not have a consistent concept for tool handling with different versions,
        // we will not display the version in the Palette View.
        // This will change in the future along with the tool versioning concept.
        // 02.02.2021; K.Schaffert
//        if (this.type.equals(Type.INTEGRATED_TOOL)) {
//            return this.getNodeNameWithVersionAppendix();
//        } else {
//            return this.getNodeName();
//        }
        return this.getNodeName();
    }

    // see comment on method getDisplayName()
    // 02.02.2021; K.Schaffert
//    private String getNodeNameWithVersionAppendix() {
//        ComponentInterface componentInterface = componentEntry.getComponentInstallation().getComponentInterface();
//        return StringUtils.format("%s (%s)", getNodeName(), componentInterface.getVersion());
//    }

    public String getPredefinedGroup() {
        return componentEntry.getComponentInstallation().getComponentInterface().getGroupName();
    }

    public String getGroupPathPrefix() {
        ToolType toolType = getToolIdentification().getType();
        if (toolType.equals(ToolType.INTEGRATED_TOOL) && getPredefinedGroup().equals(ToolType.INTEGRATED_TOOL.getTopLevelGroupName())) {
            return "";
        }
        if (toolType.equals(ToolType.INTEGRATED_WORKFLOW)
            && getPredefinedGroup().equals(ToolType.INTEGRATED_WORKFLOW.getTopLevelGroupName())) {
            return "";
        }
        return getToolIdentification().getType().getTopLevelGroupName() + PaletteViewConstants.GROUP_STRING_SEPERATOR;
    }

    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        editor.getViewer().getEditDomain().setActiveTool(getTool());
    }

    public PaletteCreationTool getTool() {
        if (tool == null) {
            this.tool = new PaletteCreationTool(new WorkflowNodeFactory(componentEntry.getComponentInstallation()));
        }
        return tool;
    }

    @Override
    public boolean isCustomized() {
        return !(getGroupPathPrefix() + getPredefinedGroup()).equals(getParentGroupNode().getQualifiedGroupName());
    }

    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        Optional<WorkflowEditor> optional = paletteView.getWorkflowEditor();
        if (!optional.isPresent()) {
            return;
        }

        WorkflowEditor editor = optional.get();
        editor.onPaletteDoubleClick(getTool());
    }

    @Override
    public Optional<String> getHelpContextID() {
        return Optional.of(ComponentUtils.getComponentInterfaceIdentifierWithoutVersion(getToolIdentification().getToolID()));
    }

    public String getComponentInformation() {

        ComponentInterface componentInterface = getComponentEntry().getComponentInterface();
        StringBuilder text = new StringBuilder();
        appendComponentNameString(text);
        appendVersionString(componentInterface, text);
        appendComponentTypeString(text);
        appendIntegrationTypeString(text);
        appendLocationString(text);
        appendAuthenticationGroupString(text);
        appendCurrentGroupString(text);
        appendDefaultGroupString(text);
        appendLoopDriverString(componentInterface, text);
        appendLocalExecutionString(componentInterface, text);
        appendDeprecatedString(componentInterface, text);
        return text.toString();
    }

    public String getComponentTooltip() {
        ComponentInterface componentInterface = getComponentEntry().getComponentInterface();
        StringBuilder text = new StringBuilder();
        appendComponentNameString(text);
        appendVersionString(componentInterface, text);
        appendComponentTypeString(text);
        appendLocationString(text);
        appendDefaultGroupString(text);
        appendDeprecatedString(componentInterface, text);
        return text.toString();
    }

    private void appendDeprecatedString(ComponentInterface componentInterface, StringBuilder text) {
        if (componentInterface.getIsDeprecated()) {
            text.append(LINE_BREAK);
            text.append(DEPRECATED_STRING);
        }
    }

    private void appendLocalExecutionString(ComponentInterface componentInterface, StringBuilder text) {
        if (componentInterface.getLocalExecutionOnly()) {
            text.append(LINE_BREAK);
            text.append(LOCAL_EXECUTION_ONLY_STRING);
        }
    }

    private void appendLoopDriverString(ComponentInterface componentInterface, StringBuilder text) {
        if (componentInterface.getIsLoopDriver()) {
            text.append(LINE_BREAK);
            text.append(LOOP_DRIVER_STRING);
        }
    }

    private void appendDefaultGroupString(StringBuilder text) {
        String predefindedGroupPath = getGroupPathPrefix() + getPredefinedGroup();
        if (!predefindedGroupPath.equals(getParentGroupNode().getQualifiedGroupName())) {
            text.append(LINE_BREAK);
            text.append(String.format(DEFAULT_GROUP_STRING, predefindedGroupPath));
        }
    }

    private void appendCurrentGroupString(StringBuilder text) {
        text.append(LINE_BREAK);
        text.append(StringUtils.format("Current Palette Group: %s", getParentGroupNode().getQualifiedGroupName()));
    }

    private void appendAuthenticationGroupString(StringBuilder text) {
        if (!isLocal() && !isMapped()) {
            text.append(LINE_BREAK);
            text.append(StringUtils.format(AUTHENTICATION_GROUP_STRING,
                String.join(", ", getComponentEntry().getDeclaredPermissionSet().getAccessGroups().stream()
                    .map(AuthorizationAccessGroup::getName).toArray(String[]::new))));
        }
    }

    private void appendIntegrationTypeString(StringBuilder text) {
        if (getToolIdentification().getIntegrationType().isPresent()) {
            text.append(LINE_BREAK);
            text.append(
                StringUtils.format(INTEGRATION_TYPE_STRING, getToolIdentification().getIntegrationType().get()));
        }
    }

    private void appendComponentTypeString(StringBuilder text) {
        text.append(LINE_BREAK);
        text.append(String.format(TYPE_STRING, getType().getName()));
    }

    private void appendVersionString(ComponentInterface componentInterface, StringBuilder text) {
        if (getType() == ToolType.INTEGRATED_TOOL || getType() == ToolType.INTEGRATED_WORKFLOW) {
            text.append(LINE_BREAK);
            text.append(StringUtils.format(VERSION_STRING, componentInterface.getVersion()));
        }
    }

    private void appendComponentNameString(StringBuilder text) {
        text.append(StringUtils.format(COMPONENT_STRING, getDisplayName()));
    }

    private void appendLocationString(StringBuilder text) {
        text.append(LINE_BREAK);
        if (isMapped()) {
            text.append(StringUtils.format(LOCATION_STRING, STRING_MAPPED));
            return;
        }
        if (!isLocal()) {
            text.append(StringUtils.format(LOCATION_STRING, STRING_REMOTE));
            return;
        }
        text.append(StringUtils.format(LOCATION_STRING, STRING_LOCAL));
    }
}
