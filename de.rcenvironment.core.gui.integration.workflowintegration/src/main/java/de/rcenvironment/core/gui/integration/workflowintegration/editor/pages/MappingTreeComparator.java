/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.ComponentNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.InputMappingNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.MappingNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.OutputMappingNode;


/**
 * Comparator for generating a sorted order in the mapping tree.
 *
 * @author Jan Flink
 */
public class MappingTreeComparator extends ViewerComparator {

    @Override
    public int category(Object element) {
        if (element instanceof InputMappingNode) {
            return 1;
        }
        if (element instanceof OutputMappingNode) {
            return 2;
        }
        return super.category(element);
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (category(e1) != category(e2)) {
            return category(e1) - category(e2);
        }
        if (e1 instanceof ComponentNode && e2 instanceof ComponentNode) {
            return ((ComponentNode) e1).compareTo((ComponentNode) e2);
        }
        if (e1 instanceof MappingNode && e2 instanceof MappingNode) {
            return ((MappingNode) e1).compareTo((MappingNode) e2);
        }

        return super.compare(viewer, e1, e2);
    }

}
