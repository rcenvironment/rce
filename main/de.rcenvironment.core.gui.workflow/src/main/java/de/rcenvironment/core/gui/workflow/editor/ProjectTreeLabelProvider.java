/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * LabelProvider for the ProjectTreeViewer of the WorkflowProjectWizard. It
 * provides an Eclipse-style project icon for root nodes and an Eclipse-style
 * folder icon for non-root nodes.
 * 
 * @author Oliver Seebach
 * 
 */
public class ProjectTreeLabelProvider extends LabelProvider {

    private static final Image FOLDER = ImageDescriptor.createFromURL(
            WorkflowPaletteFactory.class.getResource("/resources/icons/folder16.png")).createImage();
    private static final Image PROJECT = ImageDescriptor.createFromURL(
            WorkflowPaletteFactory.class.getResource("/resources/icons/project16.png")).createImage();

    @Override
    public String getText(Object element) {
        if (element instanceof IProject) {
            return ((IProject) element).getName();
        }
        if (element instanceof File) {
            if (((File) element).isDirectory()) {
                return ((File) element).getName();
            }
        }
        return element.toString();
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IProject) {
            return PROJECT;
        }
        return FOLDER;
    }

}
