/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

    /**
     * Creates an {@link ImageDescriptor} from a resource inside a bundle. The given class can be any class within the target bundle. Note
     * that Enum types can *NOT* be used for this!
     * 
     * @param bundleClass a non-enum class in the same bundle as the resource
     * @param resourcePath the path of the resource, typically starting with "/" for the root of the bundle namespace
     * @return the generated {@link ImageDescriptor}
     */
    public static ImageDescriptor createImageDescriptorFromBundleResource(Class<?> bundleClass, String resourcePath) {
        return ImageDescriptor.createFromFile(bundleClass, resourcePath);
    }

}
