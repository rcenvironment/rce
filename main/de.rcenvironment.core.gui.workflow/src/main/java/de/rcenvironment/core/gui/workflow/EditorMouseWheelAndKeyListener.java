/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;

/**
 * Listener to uniform Editor's zooming behavior via mouse wheel and hotkeys.
 *
 * @author Oliver Seebach
 */
public class EditorMouseWheelAndKeyListener implements MouseWheelListener, KeyListener {

    private ZoomManager zoomManager;

    public EditorMouseWheelAndKeyListener(ZoomManager zoomManager) {
        this.zoomManager = zoomManager;
    }

    @Override
    public void mouseScrolled(MouseEvent mouseEvent) {
        if (mouseEvent.stateMask == SWT.CONTROL) {
            int notches = mouseEvent.count;
            if (notches < 0) {
                zoomOut();
            } else {
                zoomIn();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.stateMask == SWT.CTRL && keyEvent.keyCode == '+') {
            zoomIn();
        } else if (keyEvent.stateMask == SWT.CTRL && keyEvent.keyCode == '-') {
            zoomOut();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {}

    private void zoomIn() {
        zoomManager.zoomIn();
    }

    private void zoomOut() {
        zoomManager.zoomOut();
    }

}
