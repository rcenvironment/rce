/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.dialogs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;

import de.rcenvironment.core.gui.palette.view.palettetreenodes.ComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;

/**
 * Content Provider for {@link ManageCustomGroupsDialog}'s TreeViewer.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class ManageGroupsContentProvider implements ITreeContentProvider {

    public ManageGroupsContentProvider() {
        super();
    }

    @Override
    public Object[] getChildren(Object parent) {

        if (!(parent instanceof PaletteTreeNode) || parent instanceof ComponentNode) {
            return new PaletteTreeNode[0];
        }

        final PaletteTreeNode node = (PaletteTreeNode) parent;

        PaletteTreeNode[] children = (PaletteTreeNode[]) node.getChildren();
        if (children == null) {
            return new PaletteTreeNode[0];
        }
        Set<PaletteTreeNode> customChildren = new HashSet<>();
        for (PaletteTreeNode child : children) {
            if (child.isCustomized()) {
                customChildren.add(child);
            }
        }
        return customChildren.toArray();
    }

    @Override
    public Object[] getElements(Object object) {
        return getChildren(object);
    }

    @Override
    public Object getParent(Object object) {
        if (object instanceof PaletteTreeNode) {
            return ((PaletteTreeNode) object).getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object object) {
        if (object instanceof PaletteTreeNode) {
            PaletteTreeNode node = (PaletteTreeNode) object;
            return node.isGroup() && node.hasChildren();
        }
        return false;
    }

}
