/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.impl;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentImageManagerService;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Implementation of {@link ComponentImageManagerService}.
 * 
 * @author Dominik Schneider
 */

@Component(scope = ServiceScope.SINGLETON)
public final class ComponentImageManagerImpl implements ComponentImageManagerService {

    private static final String REQUESTED_ICON_SIZE_NOT_DEFINED = "Requested icon size is not defined inside this switch case";

    // This is the md5 hash of three null images. All component interfaces without image data have this hash.
    private static final String NULL_HASH = "d41d8cd98f00b204e9800998ecf8427e";

    private static final String INTEGRATION = ToolIntegrationConstants.STANDARD_COMPONENT_ID_PREFIX.replace(".common.", "");

    /**
     * Constants for different component icons.
     * 
     * @author Dominik Schneider
     */
    public enum IconSize {

        /**
         * 16x16.
         */
        ICON16,

        /**
         * 24x24.
         */
        ICON24,

        /**
         * 32x32.
         */
        ICON32;
    }

    /**
     * Simple container utility class to store the references to all three image sizes.
     */
    public class ImagePackage {

        private String iconHash;

        /**
         * The image in size 16x16.
         */
        private Image icon16 = null;

        /**
         * The image in size 16x16.
         */
        private Image icon24 = null;

        /**
         * The image in size 32x32.
         */
        private Image icon32 = null;

        public ImagePackage(String iconHash) {
            this.iconHash = iconHash;
        }

        // if this method is refactored to public, synchronize it!
        protected void setImage(Image image, IconSize size) {
            switch (size) {
            case ICON16:
                icon16 = image;
                break;
            case ICON24:
                icon24 = image;
                break;
            case ICON32:
                icon32 = image;
                break;
            default:
                throw new IllegalArgumentException(REQUESTED_ICON_SIZE_NOT_DEFINED);
            }
        }

        // Note: all methods to get the icons require the component id. The id is used to get the correct default icon in case of missing
        // image data. The case that a RCE and an integrated component share an image package is possible, but there is no danger that a
        // wrong default icon is shown because the image data of a RCE component is always available

        /**
         * Returns the 16x16 image of the linked ComponentInterface. Creates the SWT image if it is not already existing or disposed.
         * 
         * @param componentId Id of the component which has a reference on this image package. The id is used to do deliver the correct
         *        default image.
         * 
         * @return the icon in size 16x16
         */
        public synchronized Image getIcon16(String componentId) {
            if (icon16 != null && !icon16.isDisposed()) {
                return icon16;
            } else {
                Image tmpImage = getIconImage(getIconHash(), componentId, IconSize.ICON16);
                setImage(tmpImage, IconSize.ICON16);
                return tmpImage;
            }
        }

        /**
         * Returns the 24x24 image of the linked ComponentInterface. Creates the SWT image if it is not already existing or disposed.
         * 
         * @param componentId Id of the component which has a reference on this image package. The id is used to do deliver the correct
         *        default image.
         * @return the icon in size 24x24
         */
        public synchronized Image getIcon24(String componentId) {
            if (icon24 != null && !icon24.isDisposed()) {
                return icon24;
            } else {
                Image tmpImage = getIconImage(getIconHash(), componentId, IconSize.ICON24);
                setImage(tmpImage, IconSize.ICON24);
                return tmpImage;
            }
        }

        /**
         * Returns the 32x32 image of the linked ComponentInterface. Creates the SWT image if it is not already existing or disposed.
         * 
         * @param componentId Id of the component which has a reference on this image package. The id is used to do deliver the correct
         *        default image.
         * @return the icon in size 32x32
         */
        public synchronized Image getIcon32(String componentId) {
            if (icon32 != null && !icon32.isDisposed()) {
                return icon32;
            } else {
                Image tmpImage = getIconImage(getIconHash(), componentId, IconSize.ICON32);
                setImage(tmpImage, IconSize.ICON32);
                return tmpImage;
            }
        }

        /**
         * @return the iconHash
         */
        public String getIconHash() {
            return this.iconHash;
        }

    }

    /**
     * This class is used to create default image packages with injected images.
     * 
     * @author Dominik Schneider
     *
     */
    private class DefaultImagePackage extends ImagePackage {

        DefaultImagePackage(String iconHash, Image icon16, Image icon24, Image icon32) {
            super(iconHash);
            setImage(icon16, IconSize.ICON16);
            setImage(icon24, IconSize.ICON24);
            setImage(icon32, IconSize.ICON32);
        }
    }

    // map of all image packages
    private Map<String, ImagePackage> iconImageMap;

    private ComponentImageCacheService cacheService;

    private DistributedComponentKnowledgeService knowledgeService;

    private ImagePackage rceDefaultImagePackage;

    private ImagePackage integratedDefaultImagePackage;

    public ComponentImageManagerImpl() {
        iconImageMap = new HashMap<>();
        createDefaultImagePackages();
    }

    // if this method is refactored to be public and available to be callable outside an image package, synchronize it!
    private Image getIconImage(String iconHash, String componentId, IconSize size) {
        // searching the component interface
        ComponentInterface ci = getComponentInterfaceByHash(iconHash);
        byte[] rawData = null;
        // trying to get image data from the ComponentInterface
        if (ci != null) {
            switch (size) {
            case ICON16:
                rawData = ci.getIcon16();
                break;
            case ICON24:
                rawData = ci.getIcon24();
                break;
            case ICON32:
                rawData = ci.getIcon32();
                break;
            default:
                throw new IllegalArgumentException(REQUESTED_ICON_SIZE_NOT_DEFINED);
            }

        }
        if (rawData == null) {
            // case: no component interface available with valid image data, searching in cache for data
            rawData = cacheService.getImageData(iconHash, size);
        }

        if (rawData == null) {
            // case: no image data could be found, neither in a component interface or the cache
            LogFactory.getLog(getClass()).debug("Finding component interface with image data failed, using fallback icon.");
            return getDefaultIcon(componentId, size);
        } else {
            try {
                // case: image data has been found and the swt image will be created
                return new Image(Display.getCurrent(), new ByteArrayInputStream(rawData));
            } catch (SWTException e) {
                LogFactory.getLog(getClass()).debug("Creating component icon failed, using fallback icon.", e);
                // this should never happen
                return null;
            }
        }
    }

    @Override
    public synchronized ImagePackage getImagePackage(String componentId) {
        // searching for a component interface with the given id
        ComponentInterface ci = getComponentInterfaceById(componentId);
        String iconHash = null;
        if (ci != null) {
            // getting the icon hash which is associated with the component id
            iconHash = ci.getIconHash();
        }
        if (NULL_HASH.equals(iconHash) || iconHash == null) {
            // searching for the icon hash inside the cache because there could not be found a valid hash inside a component interface
            iconHash = cacheService.getIconHash(componentId);
        }
        if (iconHash != null) {
            // found a valid icon hash
            if (iconImageMap.containsKey(iconHash)) {
                // there is already an existing image package with the found icon hash
                return iconImageMap.get(iconHash);
            } else {
                // there is no existing image package with the found icon hash, a new image package will be created
                ImagePackage tmpImagePackage = new ImagePackage(iconHash);
                iconImageMap.put(iconHash, tmpImagePackage);
                return tmpImagePackage;
            }
            // neither the cache or a component interface has a valid icon hash, so a default image package will be returned
        } else if (componentId != null && componentId.contains(INTEGRATION)) {
            // default image package for integrated tools
            return integratedDefaultImagePackage;
        } else {
            // default image package for rce tools
            return rceDefaultImagePackage;
        }
    }

    private void createDefaultImagePackages() {
        rceDefaultImagePackage = new DefaultImagePackage("", getDefaultIcon(null, IconSize.ICON16), getDefaultIcon(null, IconSize.ICON24),
            getDefaultIcon(null, IconSize.ICON32));
        String integration = ToolIntegrationConstants.STANDARD_COMPONENT_ID_PREFIX;
        integratedDefaultImagePackage = new DefaultImagePackage("", getDefaultIcon(integration, IconSize.ICON16),
            getDefaultIcon(integration, IconSize.ICON24), getDefaultIcon(integration, IconSize.ICON32));
    }

    /**
     * Searches for a component interface by a given id. Local installations are preferred to shared ones.
     * 
     * @param componentId of a component
     * @return a found component interface or null
     */
    private ComponentInterface getComponentInterfaceById(String componentId) {
        Collection<DistributedComponentEntry> installations =
            ComponentImageUtility.getDistinctInstallations(knowledgeService.getCurrentSnapshot().getAllInstallations());
        ComponentInterface remoteComponentInterface = null;
        for (DistributedComponentEntry entry : installations) {
            // the ids have to match 100% to prevent returning of a wrong icon
            if (entry.getComponentInterface().getIdentifierAndVersion().equals(componentId)) {
                remoteComponentInterface = entry.getComponentInterface();
            }
        }
        return remoteComponentInterface;
    }

    /**
     * Searches for a component interface by a given icon hash. Local installations are preferred to shared ones.
     * 
     * @param iconHash of a group of icons which are saved inside a component interface
     * @return a found component interface or null
     */
    private ComponentInterface getComponentInterfaceByHash(String iconHash) {
        Collection<DistributedComponentEntry> installations =
            ComponentImageUtility.getDistinctInstallations(knowledgeService.getCurrentSnapshot().getAllInstallations());
        ComponentInterface remoteComponentInterface = null;
        for (DistributedComponentEntry entry : installations) {
            if (entry.getComponentInterface().getIconHash().equals(iconHash)) {
                remoteComponentInterface = entry.getComponentInterface();
                break;
            }
        }

        return remoteComponentInterface;
    }

    /**
     * Returns a fallback icon either for integrated and default tools. Default RCE components will receive a different icon than integrated
     * tools. If the given componentId is null, the default RCE logo will be returned.
     * 
     * @param componentId The id of the component for which the logo will be returned
     * @param iconSize The size in pixels of the returned image.
     * @return A square icon with border length size, or a new created icon if the default icon can not be acquired.
     */
    private Image getDefaultIcon(String componentId, IconSize iconSize) {
        StandardImages image;
        switch (iconSize) {
        case ICON16:

            if (componentId == null || !componentId.contains(INTEGRATION)) {
                image = StandardImages.RCE_LOGO_16;
            } else {
                image = StandardImages.INTEGRATED_TOOL_DEFAULT_16;
            }
            break;
        case ICON24:
            if (componentId == null || !componentId.contains(INTEGRATION)) {
                image = StandardImages.RCE_LOGO_24;
            } else {
                // since we have no 24x24 tool image available, we return the 16x16 image
                image = StandardImages.INTEGRATED_TOOL_DEFAULT_16;
            }
            break;
        case ICON32:
            if (componentId == null || !componentId.contains(INTEGRATION)) {
                image = StandardImages.RCE_LOGO_32;
            } else {
                image = StandardImages.INTEGRATED_TOOL_DEFAULT_32;
            }
            break;
        default:
            throw new IllegalArgumentException(REQUESTED_ICON_SIZE_NOT_DEFINED);
        }
        return ImageManager.getInstance().getSharedImage(image);
    }

    @Reference
    private void bindComponentImageCacheService(ComponentImageCacheService imageCacheService) {
        this.cacheService = imageCacheService;
    }

    @Reference
    private void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService componentKnowledgeService) {
        this.knowledgeService = componentKnowledgeService;
    }

}
