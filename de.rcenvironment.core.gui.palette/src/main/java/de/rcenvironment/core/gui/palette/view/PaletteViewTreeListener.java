/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.palette.view;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;


/**
 * TreeListener implementation for the palette tree to handle expand and collapse events.
 *
 * @author Jan Flink
 */
public class PaletteViewTreeListener implements TreeListener {

    private TreeViewer treeViewer;

    public PaletteViewTreeListener(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    @Override
    public void treeExpanded(TreeEvent evt) {
        treeViewer.setExpandedState(evt.item.getData(), true);
        treeViewer.update(evt.item.getData(), new String[] { IBasicPropertyConstants.P_IMAGE });
    }

    @Override
    public void treeCollapsed(TreeEvent evt) {
        treeViewer.setExpandedState(evt.item.getData(), false);
        treeViewer.update(evt.item.getData(), new String[] { IBasicPropertyConstants.P_IMAGE });
    }

}
