/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Listener for {@link TableColumn}'s to control the minimal width of the column.
 * 
 * @author Kathrin Schaffert
 */
public class TableColumnMinimalWidthControlListener implements ControlListener {

    @Override
    public void controlMoved(ControlEvent arg0) {
        // nothing to do here
    }

    @Override
    public void controlResized(ControlEvent event) {
        TableColumn source = (TableColumn) event.getSource();
        final int columnWeight = 20;
        if (source.getWidth() < columnWeight) {
            source.setWidth(columnWeight);
        }
    }
}
