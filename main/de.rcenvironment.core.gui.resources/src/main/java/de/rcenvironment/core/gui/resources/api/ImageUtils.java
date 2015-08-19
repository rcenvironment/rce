/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

/**
 * Common image utilities for SWT/JFace.
 * 
 * @author Robert Mischke
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Fetches the {@link ImageDescriptor} for a shared image of the underlying Eclipse platform, identified by its Eclipse string id.
     * 
     * @param eclipseId the Eclipse string image id; see Eclipse interfaces like "ISharedImages"
     * @return the {@link ImageDescriptor}
     */
    public static ImageDescriptor getEclipseImageDescriptor(String eclipseId) {
        return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(eclipseId);
    }

}
