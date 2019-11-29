/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Returns a list of all TreeNodes to be shown in the TreeViewer.
 *
 * @author Goekhan Guerkan
 */

public class TreeContentProvider implements ITreeContentProvider {

    private static final String VERSION = " (Version: ";

    private List<String> componentsWithOneVersion;

    private List<String> componentsWithMoreVersions;

    @Override
    public Object[] getElements(Object element) {

        List<TreeNode> nodes = prepareTreeNodes(((WorkflowDescription) element));

        return nodes.toArray();

    }

    private List<TreeNode> prepareTreeNodes(WorkflowDescription description) {
        List<TreeNode> treeNodes = new ArrayList<TreeNode>();
        List<WorkflowNode> nodes = description.getWorkflowNodes();

        componentsWithOneVersion = new ArrayList<String>();
        componentsWithMoreVersions = new ArrayList<String>();

        // 1. Step: get all different component types.

        for (WorkflowNode node : nodes) {

            String componentName = node.getComponentDescription().getName();
            VersionHelper versions = checkIfSameComponentHasDifferentVersion(nodes, componentName);
            if (versions.getVersionList().size() == 1) {

                if (!componentsWithOneVersion.contains(componentName)) {

                    componentsWithOneVersion.add(componentName);

                }

            } else {

                for (String version : versions.getVersionList()) {

                    String finalCompName = node.getComponentDescription().getName() + VERSION + version + ")";

                    if (!componentsWithMoreVersions.contains(finalCompName)) {

                        componentsWithMoreVersions.add(finalCompName);

                    }

                }

            }

        }

        // 2. Step: get all components to the collected types.
        // TreeNodeFather is the Component type which has a TreeNode List with TreeNode Children for each Component

        for (String type : componentsWithOneVersion) {

            TreeNode treeNodeFather = new TreeNode(type, false);

            for (WorkflowNode node : nodes) {

                if (node.getComponentDescription().getName().equals(type)) {

                    TreeNode treeNodeChild = new TreeNode(node.getName(), true, node, treeNodeFather);
                    treeNodeFather.addChildNode(treeNodeChild);

                }

            }

            treeNodes.add(treeNodeFather);

        }

        for (String type : componentsWithMoreVersions) {

            TreeNode treeNodeFather = new TreeNode(type, false);

            for (WorkflowNode node : nodes) {
                String temp = node.getComponentDescription().getName() + VERSION + node.getComponentDescription().getVersion() + ")";
                if (temp.equals(type)) {

                    TreeNode treeNodeChild = new TreeNode(node.getName(), true, node, treeNodeFather);
                    treeNodeFather.addChildNode(treeNodeChild);

                }

            }

            treeNodes.add(treeNodeFather);

        }

        return treeNodes;

    }

    private VersionHelper checkIfSameComponentHasDifferentVersion(List<WorkflowNode> nodes, String componentString) {

        VersionHelper versionHelper = new VersionHelper();

        for (WorkflowNode node : nodes) {

            if (node.getComponentDescription().getName().equals(componentString)) {
                versionHelper.addVersion(node.getComponentDescription().getVersion());
            }

        }

        return versionHelper;

    }

    /**
     * Helper class to collect the versions of a component type.
     *
     * @author Goekhan Guerkan
     */

    public class VersionHelper {

        private List<String> versionList = new ArrayList<String>();

        /**
         * Adds a version to a Component.
         * 
         * @param version Version to add.
         */
        public void addVersion(String version) {

            if (!versionList.contains(version)) {
                versionList.add(version);
            }
        }

        public List<String> getVersionList() {
            return versionList;
        }

    }

    @Override
    public Object getParent(Object element) {
        TreeNode node = (TreeNode) element;

        if (node.isChildElement()) {
            return node.getFatherNode();
        }

        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        TreeNode node = (TreeNode) element;
        return !node.isChildElement();
    }

    @Override
    public Object[] getChildren(Object element) {

        TreeNode node = (TreeNode) element;

        if (!node.isChildElement()) {
            return node.getChildrenNodes().toArray();

        }

        return null;

    }

    @Override
    public void dispose() {

    }

    @Override
    public void inputChanged(Viewer tree, Object oldInput, Object newInput) {

    }

    
    /**
     * Returns number of component types.
     * 
     * @return number of types.
     */
    public int getNodeTypeCount() {
        if (componentsWithMoreVersions != null) {
            return componentsWithMoreVersions.size() + componentsWithOneVersion.size();

        }
        return 0;

    }
}
