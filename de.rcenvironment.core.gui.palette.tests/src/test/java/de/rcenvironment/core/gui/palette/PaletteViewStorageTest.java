/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.TreeNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.MockServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * 
 * Test cases for {@link PaletteViewStorage}.
 * 
 * @author Kathrin Schaffert
 *
 */
public class PaletteViewStorageTest {

    private static final String RCE_TOOL_NAME_OPTIMIZER = "Optimizer";

    private static final String RCE_TOOL_NAME_EXCEL = "Excel";

    private static final String TOOL_NAME = "myLocalTool";

    private static final String WF_NAME = "integratedWf";

    private static final String RCE_TOOL_INSTALLATION_ID = "de.rcenvironment.optimizer/8";

    private static final String EXCEL_INSTALLATION_ID = "de.rcenvironment.excel/3.1";

    private static final String TOOL_INSTALLATION_ID = "de.rcenvironment.integration.common.myLocalTool/4";

    private static final String WF_INSTALLATION_ID = "de.rcenvironment.integration.workflow.integratedWf/0.0";

    private static final String MY_GROUP = "MyGroup";

    private static final String MY_SUBGROUP = "MySubgroup";

    private static final String EXECUTION_GROUP = "Execution";

    private static final String EVALUATION_GROUP = "Evaluation";

    private static final String LOCAL_NODE_ID = "localNodeID";

    private class PaletteViewStorageUnderTest extends PaletteViewStorage {

        private boolean assignmentFileExists;

        private String resourceFilename;

        protected PaletteViewStorageUnderTest(PaletteViewContentProvider contentProvider) {
            super(contentProvider);
        }

        @Override
        protected boolean assignmentFileExists() {
            return assignmentFileExists;
        }

        protected void setAssignmentFileExists(boolean assignmentFileExists) {
            this.assignmentFileExists = assignmentFileExists;
        }

        protected void setResourceFilename(String resourceFilename) {
            this.resourceFilename = resourceFilename;
        }

        @Override
        protected Optional<List<String>> readAssignmentFile() {
            InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream(resourceFilename);

            List<String> lines = null;
            if (inputStream != null) {
                try {
                    lines = IOUtils.readLines(inputStream, "UTF-8");
                } catch (IOException | NullPointerException e) {
                    LogFactory.getLog(getClass()).info(StringUtils.format("Test resource %s could not be read.", resourceFilename), e);
                }
            }
            return Optional.ofNullable(lines);
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setServiceRegistryAccessFactory() {
        ServiceRegistry.setAccessFactory(new MockServiceRegistry());
    }

    @Test
    public void loadAssignmentTest() {

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(new HashSet<>());

        PaletteViewStorageUnderTest storage = new PaletteViewStorageUnderTest(contentProvider);
        storage.setAssignmentFileExists(true);
        storage.setResourceFilename("ToolGroupAssignment");

        storage.loadCustomizedAssignments();

        // prepare expected entries
        ToolIdentification identification1 =
            ToolIdentification.createStandardComponentIdentification(RCE_TOOL_INSTALLATION_ID, RCE_TOOL_NAME_OPTIMIZER);
        ToolIdentification wfIdentification =
            ToolIdentification.createIntegratedWorkflowIdentification(WF_INSTALLATION_ID, WF_NAME);
        ToolIdentification toolIdentification =
            ToolIdentification.createIntegratedToolIdentification(TOOL_INSTALLATION_ID, TOOL_NAME);

        String[] qualifiedGroupString = new String[1];
        qualifiedGroupString[0] = MY_GROUP;

        String[] qualifiedSubGroupString = new String[2];
        qualifiedSubGroupString[0] = MY_GROUP;
        qualifiedSubGroupString[1] = MY_SUBGROUP;

        // verify results
        ToolGroupAssignmentMock assignment = (ToolGroupAssignmentMock) contentProvider.getAssignment();

        assignment.assertCustomizedAssignmentContainsEntry(identification1, qualifiedGroupString);
        assignment.assertCustomizedAssignmentContainsEntry(wfIdentification, qualifiedGroupString);
        assignment.assertCustomizedAssignmentContainsEntry(toolIdentification, qualifiedSubGroupString);

    }

    @Test
    public void writeAssignmentTest() {

        Set<DistributedComponentEntry> currentToolInstallations = new HashSet<>();
        DistributedComponentEntry entry1 =
            new DistributedComponentEntryMock(RCE_TOOL_NAME_EXCEL, LOCAL_NODE_ID, DistributedComponentEntryType.LOCAL,
                EXCEL_INSTALLATION_ID,
                EXECUTION_GROUP);
        DistributedComponentEntry entry2 =
            new DistributedComponentEntryMock(RCE_TOOL_NAME_OPTIMIZER, LOCAL_NODE_ID, DistributedComponentEntryType.LOCAL,
                RCE_TOOL_INSTALLATION_ID,
                EVALUATION_GROUP);
        currentToolInstallations.add(entry1);
        currentToolInstallations.add(entry2);

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(currentToolInstallations);

        PaletteViewStorageUnderTest storage = new PaletteViewStorageUnderTest(contentProvider);
        storage.setAssignmentFileExists(true);
        storage.setResourceFilename("ToolGroupAssignment");

        // load Assignment
        storage.loadCustomizedAssignments();

        // update Tree
        contentProvider.updateTree(currentToolInstallations, null);

        // prepare expected entry
        ToolIdentification identification =
            ToolIdentification.createStandardComponentIdentification(RCE_TOOL_INSTALLATION_ID, RCE_TOOL_NAME_OPTIMIZER);
        String[] qualifiedGroupString = new String[1];
        qualifiedGroupString[0] = MY_GROUP;

        // verify results
        ToolGroupAssignmentMock assignment = (ToolGroupAssignmentMock) contentProvider.getAssignment();
        assignment.assertCustomizedAssignmentContainsEntry(identification, qualifiedGroupString);

        AccessibleComponentNode nodeToReset = null;
        AccessibleComponentNode nodeToCustomize = null;
        GroupNode myGroup = null;
        List<GroupNode> list = contentProvider.getRootNode().getAllSubGroups();
        for (GroupNode l : list) {
            if (l.getNodeName().equals(MY_GROUP)) {
                myGroup = l;
                TreeNode[] children = l.getChildren();
                assertEquals(3, children.length);
                for (TreeNode child : children) {
                    if (child instanceof AccessibleComponentNode
                        && ((AccessibleComponentNode) child).getNodeName().equals(RCE_TOOL_NAME_OPTIMIZER)) {
                        nodeToReset = (AccessibleComponentNode) child;
                    }
                }
            }
            if (l.getNodeName().equals("Standard Components")) {
                TreeNode[] children = l.getChildren();
                assertEquals(1, children.length);
                assertTrue(children[0] instanceof GroupNode);
                TreeNode[] toolNodes = children[0].getChildren();
                assertTrue(toolNodes[0] instanceof AccessibleComponentNode);
                nodeToCustomize = (AccessibleComponentNode) toolNodes[0];
                assertEquals(RCE_TOOL_NAME_EXCEL, nodeToCustomize.getNodeName());
            }
        }
        // reset Group
        contentProvider.resetGroup(nodeToReset);

        // customize entry1
        contentProvider.updateGroup(nodeToCustomize, myGroup);

        // get lines to write
        List<String> linesToWrite = storage.getAssignmentFileLinesToWrite();
        List<String> sortedLinesToWrite = linesToWrite.stream().sorted().collect(Collectors.toList());

        // get verification file
        InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream("ToolGroupAssignmentVerification");
        List<String> linesForVerification = null;
        try {
            linesForVerification = IOUtils.readLines(inputStream, "UTF-8");
        } catch (IOException e) {
            LogFactory.getLog(getClass()).info(
                "Test resource ToolGroupAssignmentVerification could not be read.", e);
        }
        List<String> sortedlinesToVerification = linesForVerification.stream().sorted().collect(Collectors.toList());

        // verify lines to write
        int i = 0;
        for (String line : sortedLinesToWrite) {
            assertEquals(sortedlinesToVerification.get(i), line);
            i++;
        }
    }

    @Test
    public void whenAssignmentFileNotExisting() {

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(new HashSet<>());
        PaletteViewStorageUnderTest storage = new PaletteViewStorageUnderTest(contentProvider);
        storage.setAssignmentFileExists(false);

        // load Assignment
        boolean returnValue = storage.loadCustomizedAssignments();
        assertFalse(returnValue);
    }

    @Test
    public void whenAssignmentFileCouldNotBeRead() {

        PaletteViewContentProviderMock contentProvider = new PaletteViewContentProviderMock(new HashSet<>());
        PaletteViewStorageUnderTest storage = new PaletteViewStorageUnderTest(contentProvider);
        storage.setAssignmentFileExists(true);
        storage.setResourceFilename("ToolGroupAssignmentNotReadable");

        // load Assignment
        boolean returnValue = storage.loadCustomizedAssignments();
        assertFalse(returnValue);
    }

}
