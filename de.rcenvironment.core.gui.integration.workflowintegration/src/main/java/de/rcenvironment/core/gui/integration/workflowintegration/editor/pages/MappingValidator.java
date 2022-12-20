/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TreeViewer;

import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.MappingNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for the mapping page of the workflow integration editor.
 *
 * @author Jan Flink
 */
public class MappingValidator implements ICellEditorValidator {

    private TreeViewer treeViewer;
    private MappingTreeContentProvider contentProvider;

    public MappingValidator(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
        contentProvider = (MappingTreeContentProvider) treeViewer.getContentProvider();
    }

    @Override
    public String isValid(Object value) {
        if (value.toString().trim().isEmpty()) {
            return "The mapped name must have at least one character.";
        }
        MappingNode node = (MappingNode) treeViewer.getStructuredSelection().getFirstElement();
        if (!node.getExternalName().trim().equalsIgnoreCase(value.toString().trim())
            && contentProvider.getMappedNamesOfOtherCheckedNodes(node).contains(value.toString().trim().toLowerCase())) {
            return StringUtils.format("The mapped name \"%s\" already exists. Note: Names are not case sensitive.",
                value.toString().trim());
        }
        return null;
    }
}
