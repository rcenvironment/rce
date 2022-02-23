/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link PaletteTreeNode} for Group Nodes.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class GroupNode extends PaletteTreeNode {

    private boolean customGroup = false;

    protected GroupNode(PaletteTreeNode parent, String nodeName) {
        super(parent, nodeName);
    }

    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        paletteView.setExpandedState(this, !paletteView.getPaletteTreeViewer().getExpandedState(this));
    }

    @Override
    public void handleEditEvent() {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected edit event on %s", this.getClass().getCanonicalName()));
    }

    @Override
    public boolean canHandleEditEvent() {
        return false;
    }

    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        editor.getViewer().getEditDomain().loadDefaultTool();

    }

    @Override
    public Optional<Image> getIcon() {
        boolean showExpanded =
            getContentProvider().getExpandedState(this) && isExpandable();
        return Optional.ofNullable(getIcon(showExpanded));
    }

    private boolean isExpandable() {
        return getContentProvider().isExpandable(this);
    }

    public Image getIcon(boolean showExpanded) {
        Image icon;
        if (showExpanded) {
            icon = ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_16);
        } else {
            icon = ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_CLOSED_16);
        }
        if (isCustomGroup()) {
            icon = getOverlayIcon(icon);
        }
        return icon;
    }

    private Image getOverlayIcon(Image icon) {
        DecorationOverlayIcon overlayIcon =
            new DecorationOverlayIcon(icon,
                ImageManager.getInstance().getImageDescriptor(StandardImages.DECORATOR_CUSTOM), IDecoration.BOTTOM_LEFT);
        return overlayIcon.createImage();
    }

    public boolean isExpanded() {
        return getContentProvider().getExpandedState(this);
    }
    
    public boolean isCustomGroup() {
        return customGroup;
    }

    public void setCustomGroup(boolean isCustomGroup) {
        this.customGroup = isCustomGroup;
    }

    @Override
    public boolean isCustomized() {
        if (isCustomGroup()) {
            return true;
        }
        if (!hasChildren()) {
            return false;
        }
        PaletteTreeNode[] children = (PaletteTreeNode[]) this.getChildren();
        return Stream.of(children).anyMatch(PaletteTreeNode::isCustomized);
    }

    @Override
    public void removeChild(TreeNode child) {
        super.removeChild(child);
        if (!hasChildren()) {
            getContentProvider().setExpandedState(this, false);
        }
    }

    public Image getMenuIcon() {
        Image icon = ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_CLOSED_16);
        if (isCustomGroup() && icon != null) {
            icon = getOverlayIcon(icon);
        }
        return icon;
    }

    @Override
    public Optional<String> getHelpContextID() {
        return Optional.empty();
    }

}
