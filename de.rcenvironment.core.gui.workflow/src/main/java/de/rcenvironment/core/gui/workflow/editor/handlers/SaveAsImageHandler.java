/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.WorkflowZoomManager;
import de.rcenvironment.core.gui.workflow.view.WorkflowRunEditor;

/**
 * Handler for saving a workflow as image. Supported file formats are JPEG, BMP and PNG.
 *
 * @author Jan Flink
 */
public class SaveAsImageHandler extends AbstractHandler {

    protected GraphicalViewer viewer;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        final IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

        if (activeEditor instanceof WorkflowEditor) {
            saveWorkflowAsImage(((WorkflowEditor) activeEditor).getViewer(),
                activeEditor.getTitle().substring(0, activeEditor.getTitle().lastIndexOf('.')));
        } else if (activeEditor instanceof WorkflowRunEditor) {
            saveWorkflowAsImage(((WorkflowRunEditor) activeEditor).getViewer(),
                activeEditor.getTitle().substring(0, activeEditor.getTitle().lastIndexOf(':')).replaceAll(":", "-"));
        }
        return null;
    }

    private static int getFormat(String path) {
        int format = SWT.IMAGE_BMP;
        if (path.endsWith(".jpg")) {
            format = SWT.IMAGE_JPEG;
        } else if (path.endsWith(".png")) {
            format = SWT.IMAGE_PNG;
        }
        return format;
    }

    private static String getPath(String filename) {
        String[] filterExtensions = new String[] { "*.bmp", "*.jpg", "*.png", };
        FileDialog fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
        fileDialog.setFilterExtensions(filterExtensions);
        fileDialog.setFilterIndex(2);
        fileDialog.setText("Save As Image");
        fileDialog.setOverwrite(true);
        fileDialog.setFileName(filename);
        return fileDialog.open();
    }

    private static void saveWorkflowAsImage(GraphicalViewer viewer, String filename) {

        String path = getPath(filename);
        ScalableFreeformRootEditPart editPart =
            (ScalableFreeformRootEditPart) viewer.getEditPartRegistry().get(LayerManager.ID);
        editPart.getZoomManager().setZoom(1);
        IFigure figure = editPart.getLayer(LayerConstants.PRINTABLE_LAYERS);
        figure.setBackgroundColor(viewer.getControl().getBackground());
        boolean opaque = figure.isOpaque();
        figure.setOpaque(true);
        Rectangle bounds = figure.getBounds();

        Image image = new Image(Display.getDefault(), bounds.width, bounds.height);
        Graphics graphics = new SWTGraphics(new GC(image));
        graphics.translate(-bounds.x, -bounds.y);
        figure.paint(graphics);

        ImageLoader imageLoader = new ImageLoader();
        imageLoader.data = new ImageData[] { image.getImageData() };
        if (path != null) {
            imageLoader.save(path, getFormat(path));
        }

        figure.setOpaque(opaque);
        graphics.dispose();
        image.dispose();
        if (editPart.getZoomManager() instanceof WorkflowZoomManager) {
            ((WorkflowZoomManager) editPart.getZoomManager()).restorePreviousZoomLevel();
        }
        viewer.getControl().setFocus();
    }

}
