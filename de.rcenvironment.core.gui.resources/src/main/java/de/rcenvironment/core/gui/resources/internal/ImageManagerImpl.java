/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

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
    public Image getSharedImage(final ImageSource source) {
        final AtomicReference<Image> ref = new AtomicReference<>();
        // always run the code triggering potential Image creation within the SWT thread; see Mantis #15416
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                synchronized (sharedImages) { // probably redundant, but doesn't hurt either
                    Image image = sharedImages.get(source);
                    if (image == null) {
                        // TODO handle image failures explicitly?
                        image = source.getImageDescriptor().createImage();
                        sharedImages.put(source, image);
                    }
                    ref.set(image);
                }
            }
        });
        final Image image = ref.get();
        // sanity check
        if (image == null) {
            throw new IllegalStateException("Image reference null at the end of getSharedImage()");
        }
        return image;
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
        synchronized (sharedImages) { // probably redundant, but doesn't hurt either
            log.debug("Disposing " + sharedImages.values().size() + " shared images");
            for (Image image : sharedImages.values()) {
                image.dispose();
            }
            sharedImages.clear();
        }
        log.debug("Image manager disposed");
    }
}
