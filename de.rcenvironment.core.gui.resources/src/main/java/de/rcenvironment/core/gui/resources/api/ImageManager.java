/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Abstract singleton holder for image manager implementations. Added to separate instance handling and bundle activation/disposal from
 * actual image management.
 * 
 * @author Robert Mischke
 */
public abstract class ImageManager {

    private static volatile ImageManager instance;

    protected ImageManager() {
        // TODO Auto-generated constructor stub
    }

    public static final ImageManager getInstance() {
        return instance;
    }

    public static void setInstance(ImageManager instance) {
        ImageManager.instance = instance;
    }

    /**
     * Retrieves a shared SWT {@link Image} for the given {@link ImageSource}; if it does not exist yet, it is created. All shared images
     * are automatically disposed on shutdown.
     * <p>
     * Callers of this method MUST NOT dispose the returned {@link Image}!
     * 
     * @param source an {@link ImageSource}
     * @return a shared SWT {@link Image} image file bytes for the given {@link ImageSource}, as a byte array
     */
    public abstract Image getSharedImage(ImageSource source);

    /**
     * @param source an {@link ImageSource}
     * @return an {@link ImageDescriptor} for the given {@link ImageSource}
     */
    public abstract ImageDescriptor getImageDescriptor(ImageSource source);

    /**
     * @param source an {@link ImageSource}
     * @return the raw image file bytes for the given {@link ImageSource}, as a byte array
     */
    public abstract byte[] getImageBytes(ImageSource source);

    protected abstract void dispose();
}
