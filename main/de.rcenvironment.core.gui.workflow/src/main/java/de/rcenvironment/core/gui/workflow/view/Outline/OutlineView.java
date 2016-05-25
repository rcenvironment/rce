/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.Outline;


import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Shows the workflow editor in a navigatable thumbnail view.
 *
 * @author Jan Flink
 */
public class OutlineView extends ContentOutlinePage {

    private ScrollableThumbnail editorThumbnail;

    private DisposeListener disposeListener;

    private GraphicalViewer viewer;

    private SashForm sash;

    public OutlineView(GraphicalViewer viewer) {
        super(viewer);
        this.viewer = viewer;
    }

    @Override
    public void createControl(Composite parent) {
        sash = new SashForm(parent, SWT.VERTICAL);
        Canvas canvas = new Canvas(sash, SWT.BORDER);
        LightweightSystem lws = new LightweightSystem(canvas);
        editorThumbnail = new ScrollableThumbnail(
            (Viewport) ((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getFigure());
        editorThumbnail.setSource(((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getLayer(LayerConstants.PRINTABLE_LAYERS));
        lws.setContents(editorThumbnail);

        disposeListener = new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (editorThumbnail != null) {
                    editorThumbnail.deactivate();
                    editorThumbnail = null;
                }
            }
        };

        viewer.getControl().addDisposeListener(disposeListener);
    }

    @Override
    public Control getControl() {
        return sash;
    }

    @Override
    public void dispose() {
        if (viewer != null) {
            if (viewer.getControl() != null && !viewer.getControl().isDisposed()) {
                viewer.getControl().removeDisposeListener(disposeListener);
            }
            super.dispose();
        }
    }
}
