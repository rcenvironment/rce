/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.Optional;

import de.rcenvironment.core.gui.palette.PaletteViewConstants;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link PaletteTreeNode} for the Root Node.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class RootNode extends PaletteTreeNode {
    
    private PaletteViewContentProvider contentProvider;

    public RootNode(PaletteViewContentProvider contentProvider) {
        super(null, PaletteViewConstants.ROOT_NODE_NAME);
        this.contentProvider = contentProvider;
    }

    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected doubleclick event on %s", this.getClass().getCanonicalName()));
    }
    
    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected select event on %s", this.getClass().getCanonicalName()));
    }

    @Override
    public void handleEditEvent() {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected edit event on %s", this.getClass().getCanonicalName()));
    }

    @Override
    public boolean canHandleEditEvent() {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected method call canHandleEditEvent on %s", this.getClass().getCanonicalName()));
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected method call getDisplayName on %s", this.getClass().getCanonicalName()));
    }

    @Override
    public boolean isCustomized() {
        return false;
    }

    @Override
    public Optional<String> getHelpContextID() {
        return Optional.empty();
    }

    public PaletteViewContentProvider getContentProvider() {
        return contentProvider;
    }
}
