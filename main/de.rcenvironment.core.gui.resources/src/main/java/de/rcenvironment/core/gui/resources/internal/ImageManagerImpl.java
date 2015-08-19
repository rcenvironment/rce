/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.ImageSource;

/**
 * Standard {@link ImageManager} implementation.
 * 
 * @author Robert Mischke
 */
public class ImageManagerImpl extends ImageManager {

    private final Map<ImageSource, Image> sharedImages = new HashMap<>();

    private final Log log = LogFactory.getLog(getClass());

    public ImageManagerImpl() {
        log.debug("Image manager initialized");
    }

    @Override
    public Image getSharedImage(ImageSource source) {
        synchronized (sharedImages) {
            Image image = sharedImages.get(source);
            if (image == null) {
                // TODO handle image failures explicitly?
                image = source.getImageDescriptor().createImage();
                sharedImages.put(source, image);
            }
            return image;
        }
    }

    @Override
    public ImageDescriptor getImageDescriptor(ImageSource source) {
        return source.getImageDescriptor();
    }

    @Override
    public byte[] getImageBytes(ImageSource source) {
        log.debug("Providing raw image data for " + source);
        // TODO this probably parses the image data (needlessly); optimize if called often
        return source.getImageDescriptor().getImageData().data;
    }

    @Override
    public void dispose() {
        synchronized (sharedImages) {
            log.debug("Disposing " + sharedImages.values().size() + " shared images");
            for (Image image : sharedImages.values()) {
                image.dispose();
            }
            sharedImages.clear();
        }
        log.debug("Image manager disposed");
    }
}
