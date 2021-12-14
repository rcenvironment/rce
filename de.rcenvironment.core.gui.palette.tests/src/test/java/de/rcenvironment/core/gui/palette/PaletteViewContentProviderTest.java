/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.TreeNode;
import org.junit.Test;

import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.OfflineComponentNode;

/**
 * 
 * Test cases for {@link PaletteViewContentProvider}.
 * 
 * @author Kathrin Schaffert
 *
 */
public class PaletteViewContentProviderTest {

    private static final String USER_INTEGRATED_WORKFLOWS = "User Integrated Workflows";

    private static final String USER_INTEGRATED_TOOLS = "User Integrated Tools";

    private static final String TOOL_INSTALLATION_ID = "de.rcenvironment.integration.common.myTool/1";

    private static final String TOOL_NAME = "MyTool";

    private static final String NODE_ID_1 = "nodeID1";

    private static final String NODE_ID_2 = "nodeID2";

    private static final String MY_GROUP = "MyGroup";

    private static final String GROUP_1 = "Group1";

    private static final String GROUP_2 = "Group2";

    private static final String WF_INSTALLATION_ID = "de.rcenvironment.integration.workflow.myWf/0.0";

    private static final String WF_NAME = "MyWf";

    private static final String CUSTOMIZED_GROUP = "customized Group";

// Tests for PaletteViewContentProvider:updateTree

    @Test
    public void testUpdateTree1() {

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(null);
        ToolGroupAssignmentMock assignment = (ToolGroupAssignmentMock) contentProvider.getAssignment();

        assignment.addToolIdentificationCustomizedAssignment(TOOL_INSTALLATION_ID, TOOL_NAME, ToolType.INTEGRATED_TOOL, CUSTOMIZED_GROUP);

        contentProvider.updateTree(new HashSet<>(), null);

        TreeNode[] children = contentProvider.getRootNode().getChildren();
        assertEquals(1, contentProvider.getRootNode().getChildren().length);

        assertTrue(children[0] instanceof GroupNode);
        GroupNode group = (GroupNode) children[0];
        assertEquals(CUSTOMIZED_GROUP, group.getNodeName());
        assertTrue(group.isCustomized());

        TreeNode[] toolNodes = group.getChildren();
        assertEquals(1, toolNodes.length);
        assertTrue(toolNodes[0] instanceof OfflineComponentNode);

        Set<DistributedComponentEntry> installationToAdd1 = new HashSet<>();
        addToolInstallation(installationToAdd1, TOOL_NAME, NODE_ID_2,
            DistributedComponentEntryType.LOCAL, TOOL_INSTALLATION_ID, GROUP_2);

        contentProvider.updateTree(installationToAdd1, null);

        assertTrue(children[0] instanceof GroupNode);
        GroupNode group1 = (GroupNode) children[0];
        assertEquals(CUSTOMIZED_GROUP, group1.getNodeName());
        assertTrue(group1.isCustomized());

        TreeNode[] toolNodes1 = group1.getChildren();
        assertEquals(1, toolNodes1.length);
        assertTrue(toolNodes1[0] instanceof AccessibleComponentNode);
        AccessibleComponentNode tool = (AccessibleComponentNode) toolNodes1[0];
        assertTrue(tool.isLocal());
    }

    @Test
    public void testUpdateTree2() {

        Set<DistributedComponentEntry> installationToAdd1 = new HashSet<>();
        addToolInstallation(installationToAdd1, TOOL_NAME, NODE_ID_2,
            DistributedComponentEntryType.REMOTE, TOOL_INSTALLATION_ID, GROUP_2);

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(installationToAdd1);
        contentProvider.updateTree(installationToAdd1, null);

        assertContentProviderContainsEntry(contentProvider, GROUP_2);

        Set<DistributedComponentEntry> installationToAdd2 = new HashSet<>();
        addToolInstallation(installationToAdd2, TOOL_NAME, NODE_ID_1,
            DistributedComponentEntryType.LOCAL, TOOL_INSTALLATION_ID, GROUP_1);

        Set<DistributedComponentEntry> installationToRemove = new HashSet<>();
        addToolInstallation(installationToRemove, TOOL_NAME, NODE_ID_2,
            DistributedComponentEntryType.REMOTE, TOOL_INSTALLATION_ID, GROUP_2);

        contentProvider.updateTree(installationToAdd2, installationToRemove);

        assertContentProviderContainsEntry(contentProvider, GROUP_1);

    }

    private void assertContentProviderContainsEntry(PaletteViewContentProviderMock contentProvider, String groupName) {
        TreeNode[] children = contentProvider.getRootNode().getChildren();
        assertEquals(1, contentProvider.getRootNode().getChildren().length);

        assertTrue(children[0] instanceof GroupNode);
        GroupNode group = (GroupNode) children[0];
        assertEquals(USER_INTEGRATED_TOOLS, group.getNodeName());

        TreeNode[] subgroups = group.getChildren();
        assertEquals(1, subgroups.length);
        assertTrue(subgroups[0] instanceof GroupNode);
        GroupNode subgroup = (GroupNode) subgroups[0];
        assertEquals(groupName, subgroup.getNodeName());

        TreeNode[] toolNodes = subgroup.getChildren();
        assertEquals(1, toolNodes.length);
        assertTrue(toolNodes[0] instanceof AccessibleComponentNode);
    }



    protected DistributedComponentEntry addToolInstallation(Set<DistributedComponentEntry> toolInstallations, String displayName,
        String nodeID,
        DistributedComponentEntryType type, String installationID, String predefinedGroup) {
        DistributedComponentEntry entry = new DistributedComponentEntryMock(displayName, nodeID, type, installationID, predefinedGroup);
        toolInstallations.add(entry);
        return entry;
    }

    @Test
    public void testResetGroup() {
        Set<DistributedComponentEntry> currentToolInstallations = new HashSet<>();
        addToolInstallation(currentToolInstallations, WF_NAME, NODE_ID_1, DistributedComponentEntryType.LOCAL, WF_INSTALLATION_ID,
            MY_GROUP);
        addToolInstallation(currentToolInstallations, TOOL_NAME, NODE_ID_1, DistributedComponentEntryType.LOCAL,
            TOOL_INSTALLATION_ID, GROUP_1);

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(currentToolInstallations);
        ToolGroupAssignmentMock assignment = (ToolGroupAssignmentMock) contentProvider.getAssignment();

        assignment.addToolIdentificationCustomizedAssignment(WF_INSTALLATION_ID, WF_NAME, ToolType.INTEGRATED_WORKFLOW, CUSTOMIZED_GROUP);
        assignment.addToolIdentificationCustomizedAssignment(TOOL_INSTALLATION_ID, TOOL_NAME, ToolType.INTEGRATED_TOOL, CUSTOMIZED_GROUP);

        contentProvider.updateTree(currentToolInstallations, null);

        assertEquals(2, assignment.getCustomizedAssignments().size());

        TreeNode[] children = contentProvider.getRootNode().getChildren();
        assertEquals(1, children.length);
        assertTrue(children[0] instanceof GroupNode);
        GroupNode subgroup = (GroupNode) children[0];
        assertEquals(CUSTOMIZED_GROUP, subgroup.getNodeName());

        TreeNode[] toolNodes = subgroup.getChildren();
        assertEquals(2, toolNodes.length);

        for (TreeNode node : toolNodes) {
            assertTrue(toolNodes[0] instanceof AccessibleComponentNode);
            contentProvider.resetGroup((AccessibleComponentNode) node);
        }

        assertEquals(0, assignment.getCustomizedAssignments().size());

        TreeNode[] children1 = contentProvider.getRootNode().getChildren();
        assertEquals(3, children1.length);
        for (TreeNode child : children1) {
            assertTrue(child instanceof GroupNode);
            GroupNode group = (GroupNode) child;
            assertTrue(group.getNodeName().equals(USER_INTEGRATED_TOOLS) || group.getNodeName().equals(USER_INTEGRATED_WORKFLOWS)
                || group.getNodeName().equals(CUSTOMIZED_GROUP));
            if (group.getNodeName().equals(USER_INTEGRATED_TOOLS)) {
                assertContentProviderContainsGroup(group, GROUP_1);
            }
            if (group.getNodeName().equals(USER_INTEGRATED_WORKFLOWS)) {
                assertContentProviderContainsGroup(group, MY_GROUP);
            }
            if (group.getNodeName().equals(CUSTOMIZED_GROUP)) {
                assertNull(group.getChildren());
            }
        }
    }

    private void assertContentProviderContainsGroup(GroupNode group, String groupName) {
        TreeNode[] subgroups = group.getChildren();
        assertEquals(1, subgroups.length);
        assertTrue(subgroups[0] instanceof GroupNode);
        GroupNode subgroup1 = (GroupNode) subgroups[0];
        assertEquals(groupName, subgroup1.getNodeName());

        TreeNode[] toolNodes1 = subgroup1.getChildren();
        assertEquals(1, toolNodes1.length);
        assertTrue(toolNodes1[0] instanceof AccessibleComponentNode);
    }
}
