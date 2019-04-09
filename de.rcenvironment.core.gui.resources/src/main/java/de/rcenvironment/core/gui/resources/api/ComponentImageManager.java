/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.api.ComponentInterface;

/**
 * The ComponentImageManager caches the SWT Image resource for the component icons. Currently the image is created and cached if it is
 * requested for the first time.
 * 
 * TODO Insert the icon as soon as it becomes known by the RCE instance. To implement this behavior, this class should implement the
 * DistributedComponentKnowledgeListener. However as other objects are also DistributedComponentKnowledgeListeners, they could get notified
 * before the ComponentImageManger and try to access an Image before it was created by this Manager.
 *
 * TODO Remove/update the image as soon as the component becomes unknown to the RCE instance. There might be synchronization issues here.
 * Furthermore issue #0014327 needs to be considered.
 *
 * TODO Automatically dispose all cached images if the RCE instance is shut down.
 *
 * @author Tobias Rodehutskors
 */
public final class ComponentImageManager {

    private static final String SETTING_COMPONENT_ICON_FAILED = "Setting component icon failed.";

    private static ComponentImageManager instance;

    /**
     * Simple container utility class to store the references to all three image sizes.
     */
    private class ImageContainer {

        private Image icon16 = null;

        private Image icon24 = null;

        private Image icon32 = null;
    }

    private Map<String, ImageContainer> iconImageMap = new HashMap<String, ImageContainer>();

    private ComponentImageManager() {

    }

    /**
     * @return The static instance of the {@link ComponentImageManager}.
     */
    public static ComponentImageManager getInstance() {
        if (instance == null) {
            instance = new ComponentImageManager();
        }
        return instance;
    }

    /**
     * Returns the icon of the given component as an image. The image is cached by the ComponentImageManger and should not be disposed!
     * 
     * @param ci The ComponentInterface of the Component for which the icon image should be retrieved.
     * @return The icon image of the ComponentInterface or null if the icon is not specified.
     */
    public Image getIcon16Image(ComponentInterface ci) {
        return returnImageContainer(ci).icon16;
    }

    /**
     * Returns the icon of the given component as an image. The image is cached by the ComponentImageManger and should not be disposed!
     * 
     * @param ci The ComponentInterface of the Component for which the icon image should be retrieved.
     * @return The icon image of the ComponentInterface or null if the icon is not specified.
     */
    public Image getIcon24Image(ComponentInterface ci) {
        return returnImageContainer(ci).icon24;
    }

    /**
     * Returns the icon of the given component as an image. The image is cached by the ComponentImageManger and should not be disposed!
     * 
     * @param ci The ComponentInterface of the Component for which the icon image should be retrieved.
     * @return The icon image of the ComponentInterface or null if the icon is not specified.
     */
    public Image getIcon32Image(ComponentInterface ci) {
        return returnImageContainer(ci).icon32;
    }

    private ImageContainer returnImageContainer(ComponentInterface ci) {

        // check if the cache already contains the icon hash
        if (!iconImageMap.containsKey(ci.getIconHash())) {

            // if not, instantiate the images and store them in the cache
            ImageContainer tmpContainer = new ImageContainer();

            if (ci.getIcon16() != null) {
                try {
                    tmpContainer.icon16 = new Image(Display.getCurrent(), new ByteArrayInputStream(ci.getIcon16()));
                } catch (SWTException e) {

                    // TODO should we set a fallback icon as done in https://mantis.sc.dlr.de/view.php?id=11453 ?
                    // image = Activator.getInstance().getImageRegistry().getDescriptor(Activator.IMAGE_RCE_ICON_16);
                    LogFactory.getLog(getClass()).debug(SETTING_COMPONENT_ICON_FAILED, e);
                }
            }

            if (ci.getIcon24() != null) {
                try {
                    tmpContainer.icon24 = new Image(Display.getCurrent(), new ByteArrayInputStream(ci.getIcon24()));
                } catch (SWTException e) {
                    LogFactory.getLog(getClass()).debug(SETTING_COMPONENT_ICON_FAILED, e);
                }
            }

            if (ci.getIcon32() != null) {
                try {
                    tmpContainer.icon32 = new Image(Display.getCurrent(), new ByteArrayInputStream(ci.getIcon32()));
                } catch (SWTException e) {
                    LogFactory.getLog(getClass()).debug(SETTING_COMPONENT_ICON_FAILED, e);
                }
            }

            iconImageMap.put(ci.getIconHash(), tmpContainer);
        }

        return iconImageMap.get(ci.getIconHash());
    }
}
