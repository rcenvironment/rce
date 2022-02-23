/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;

/**
 * Comparator for sorting nodes in the PaletteView´s TreeViewer.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class PaletteTreeViewerComparator extends ViewerComparator {

    /** Constant. */
    public static final int SORT_BY_NAME_ASC = 1;

    /** Constant. */
    public static final int SORT_BY_NAME_DESC = -1;

    private static final String PREFIX_UNDERSCORE = "_";

    private int direction = 1;

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    @Override
    public int category(Object element) {
        if (element instanceof PaletteTreeNode) {
            PaletteTreeNode node = (PaletteTreeNode) element;
            if (isTopLevelEditorToolTool(node)) {
                return 0;
            }
            if ((node.getPaletteParent().isRoot() && !isPredefinedTopLevelGroup(node))
                || node.getNodeName().startsWith(PREFIX_UNDERSCORE)) {
                return 3;
            }
            if (node.isGroup()) {
                return 2;
            }
        }
        return 1;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (!(e1 instanceof PaletteTreeNode && e2 instanceof PaletteTreeNode)) {
            return 0;
        }
        if (category(e1) != category(e2)) {
            return category(e1) - category(e2);
        }
        PaletteTreeNode p1 = (PaletteTreeNode) e1;
        PaletteTreeNode p2 = (PaletteTreeNode) e2;

        if (isTopLevelEditorToolTool(p1) && isTopLevelEditorToolTool(p2)) {
            return 0;
        }
        if (isPredefinedTopLevelGroup(p1) && isPredefinedTopLevelGroup(p2)) {
            return p1.compareTo(p2);
        }
        if (p1.isGroup() && p2.isGroup()) {
            return p1.compareTo(p2) * direction;
        }
        return p1.compareTo(p2);
    }

    private boolean isTopLevelEditorToolTool(PaletteTreeNode node) {
        if (!node.getPaletteParent().isRoot()) {
            return false;
        }
        return node.isCreationTool();
    }

    private boolean isPredefinedTopLevelGroup(PaletteTreeNode node) {
        if (!node.isGroup() || !node.getPaletteParent().isRoot()) {
            return false;
        }
        return ToolType.getTopLevelGroupNames().contains(node.getDisplayName());
    }
}
