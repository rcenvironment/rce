/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The {@link BundleActivator} of this bundle. Responsibilities:
 * <ul>
 * <li>govern the resources used by this bundle</li>
 * </ul>
 * 
 * @author Christian Weiss
 */
public class Activator implements BundleActivator {

    /** The location of the images properties file relative to this class. */
    private static final String IMAGES_PROPERTIES_FILE = "images.properties";

    /** The {@link BundleContext}. */
    private static BundleContext bundleContext;

    /** The mapping of image names to their according locations. */
    private static Properties imageMapping;

    /** The {@link ImageRegistry}. */
    private static ImageRegistry imageRegistry;

    /**
     * {@inheritDoc}
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        Activator.bundleContext = context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        //
        if (imageRegistry != null) {
            imageRegistry.dispose();
            imageRegistry = null;
        }
    }

    /**
     * Returns the {@link BundleContext} of this bundle.
     * 
     * @return the {@link BundleContext}
     */
    public static BundleContext getContext() {
        return bundleContext;
    }

    /**
     * Returns the {@link ImageRegistry} of this bundle.
     * 
     * @return the {@link ImageRegistry}
     */
    private static ImageRegistry getImageRegistry() {
        if (Display.getDefault() == null) {
            return null;
        }
        imageRegistry = new ImageRegistry();
        return imageRegistry;
    }

    /**
     * Puts an image into the local {@link ImageRegistry} using the given key. This ensures the
     * images' disposal upon bundle shutdown.
     * 
     * @param key the key
     * @param image the image
     */
    public static void putImage(final String key, final Image image) {
        if (getImageRegistry() == null) {
            return;
        }
        imageRegistry.put(key, image);
    }

    /**
     * Returns the image stored unter the given key in the local {@link ImageRegistry}.
     * 
     * @param key the key
     * @return the image
     */
    public static Image getImage(final String key) {
        if (getImageRegistry() == null) {
            return null;
        }
        Image result = imageRegistry.get(key);
        // if the image is not registered yet, try to find it in the images.properties file
        // TODO needs exception handling
        if (result == null) {
            // load the image mapping
            if (imageMapping == null) {
                imageMapping = new Properties();
                try {
                    imageMapping.load(Activator.class
                        .getResourceAsStream(IMAGES_PROPERTIES_FILE));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // check if the given key exists in the image mapping
            if (imageMapping.getProperty(key) != null) {
                // try to load the configured file
                final InputStream imageInputStream = Activator.class
                    .getResourceAsStream(imageMapping.getProperty(key));
                // if the file exists try to load the image from it
                if (imageInputStream != null) {
                    result = new Image(Display.getDefault(), imageInputStream);
                    imageRegistry.put(key, result);
                }
            }
        }
        return result;
    }

}
