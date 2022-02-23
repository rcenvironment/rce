/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationServiceImpl.IconSize;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Since the handling of the tool icon has become quite involved over time, due to precomputing scaled versions of the icon and several
 * fallbacks when accessing the icon, we move the code responsible for handling this icon from {@link ToolIntegrationServiceImpl} into this
 * class.
 * 
 * @author Alexander Weinert
 */
@Component(service = IconHelper.class)
public final class IconHelper {

    private static final Log LOGGER = LogFactory.getLog(IconHelper.class);

    private FileAccessService fileAccess;

    private HashingService hashingService;

    private ImageService imageService;

    /**
     * This method performs two tasks: First, it copies the icon defined by the user to the tool integration folder, if the user has chosen
     * this option during tool integration. Moreover, if the user has defined a custom icon at all, then this method constructs scaled
     * versions of this icon for the standard icon sizes as defined by {@link IconSize} and stores these pre-scaled versions int he tool
     * integration folder.
     * 
     * @param configurationMap The map describing the configuration of the tool to be integrated.
     * @param toolConfigFile The configuration.json-file.
     */
    public void prescaleAndCopyIcon(ConfigurationMap configurationMap, File toolConfigFile) {
        final String toolIconPath = configurationMap.getIconPath();
        final boolean toolIconPathSpecified = (toolIconPath != null) && (!toolIconPath.isEmpty());
        if (!toolIconPathSpecified) {
            return;
        }

        final File icon = getIconFile(toolIconPath, toolConfigFile);

        // We already compute the MD5-hash of the icon at this point in order to have a single point at which we may capture IOExceptions
        // occurring during reading the icon file.
        final BufferedImage iconImage;
        final String md5Hash;
        try {
            iconImage = imageService.readImage(icon);
            final byte[] iconContent = fileAccess.readToByteArray(icon);
            md5Hash = hashingService.md5Hex(iconContent);
        } catch (IOException e) {
            LOGGER.debug("IOException during reading tool icon", e);
            return;
        }

        // We cannot guarantee that there is a value stored for whether or not the tool icon shall be copied to the tool integration folder,
        // as the configurationMap may result either from the tool integration wizard (which should contain a value for this option), or
        // from reading a configuration.json from the disk (which should not contain a value for this option).
        final Object uploadIconSelection = configurationMap.shouldUploadIcon();
        final boolean sourceIconShallBeCopied = (uploadIconSelection != null) && ((Boolean) uploadIconSelection);

        // In order to determine whether the icon file can be copied, we also have to check if the path to the file is absolute. A relative
        // path, in contrast, indicates that the icon is already stored in the tool configuration folder and hence, does not need to be
        // copied anymore.
        final boolean sourceIconCanBeCopied = icon.exists() && icon.isFile() && icon.isAbsolute();
        if (sourceIconShallBeCopied && sourceIconCanBeCopied) {
            final File destination = fileAccess.createFile(toolConfigFile, icon.getName());
            try {
                fileAccess.copyFile(icon, destination);
                // By only writing icon.getName() into the configuration map instead, we implicitly state that the path to the tool icon is
                // relative to the tool configuration folder.
                configurationMap.setIconPath(icon.getName());
                // Since we have copied the icon to the tool integration folder, we can remove the obligation to do so from the
                // configuration map
                configurationMap.doNotUploadIcon();
            } catch (IOException e) {
                LOGGER.warn("Could not copy icon to tool directory: ", e);
            }
        }

        for (IconSize iconSize : IconSize.values()) {
            tryPrescaleAndStoreIcon(iconImage, iconSize, toolConfigFile);
        }

        configurationMap.setIconHash(md5Hash);
        configurationMap.setIconModificationDate(icon.lastModified());
    }

    /**
     * If the icon cannot be successfully scaled and saved, an error is logged.
     * 
     * @param iconImage The original icon provided by the user.
     * @param iconSize The size the icon is to be scaled to.
     * @param toolConfigFile A file describing the tool configuration folder.
     * @return True if the scaled version of the icon could be successfully saved. False otherwise.
     */
    private boolean tryPrescaleAndStoreIcon(BufferedImage iconImage, IconSize iconSize, File toolConfigFile) {
        final BufferedImage resizedIcon = imageService.resize(iconImage, iconSize.getSize());
        final String format = iconSize.getPath().substring(iconSize.getPath().lastIndexOf('.') + 1);
        final File destination = fileAccess.createFile(toolConfigFile, iconSize.getPath());
        try {
            imageService.write(resizedIcon, format, destination);
            return true;
        } catch (IOException e) {
            final String errorMessage = String.format("Unable to write resized tool icon to file %s.", destination.toString());
            LOGGER.debug(errorMessage);
            return false;
        }
    }

    /**
     * Returns an icon of the desired size for the tool described by the configurationMap. If a custom icon is defined and if a pre-scaled
     * version has been saved, that pre-scaled version is returned. Otherwise, if a (legacy) icon is defined, it is loaded and scaled to the
     * requested size. Otherwise, the default icon is loaded in the given size. If that fails as well, then null is returned.
     * 
     * @param iconSize The size of the icon that shall be loaded.
     * @param configurationMap The configuration map describing the integrated component.
     * @param toolDirFile A {@link File} describing the tool integration folder.
     * @return A byte array containing an icon of the given size or null, if no such icon could be found
     */
    public byte[] getIcon(IconSize iconSize, ConfigurationMap configurationMap, File toolDirFile) {
        final byte[] prescaledIcon = tryGetPrescaledIcon(iconSize, configurationMap, toolDirFile);
        if (prescaledIcon != null) {
            return prescaledIcon;
        }

        final byte[] livescaledIcon = getLivescaledIcon(iconSize.getSize(), configurationMap, toolDirFile);
        if (livescaledIcon != null) {
            return livescaledIcon;
        }

        final byte[] defaultIcon = getDefaultIcon(iconSize.getSize());
        if (defaultIcon != null) {
            return defaultIcon;
        }

        return null;
    }
    
    /**
     * If an error occurs during reading the prescaled icon, an error is logged.
     * 
     * @param iconSize The size for which we are trying to find a prescaled version.
     * @param configurationMap The configuration map describing the component.
     * @param toolDirFile A file describing the tool integration folder.
     * @return A byte array containing the icon for the component in the given file, or null, if no prescaled version of this icon exists or
     *         if there was an error during reading that version.
     */
    private byte[] tryGetPrescaledIcon(IconSize iconSize, ConfigurationMap configurationMap, File toolDirFile) {
        // First, if the content of the actual icon has not changed since the last write of the configuration file and the associated
        // pre-scaling, we try to load the pre-scaled version of the icon.
        final String iconPath = (String) configurationMap.getIconPath();
        if (iconPath == null || iconPath.isEmpty()) {
            return null;
        }

        // We first quickly check whether the file has been modified since the prescaled versions would have been created, if they exist.
        // This is faster than hashing the file later down the line to check for actual (likely) equality.
        final File icon = getIconFile(iconPath, toolDirFile);
        final Long storedModifiedDate = configurationMap.getIconModificationDate();
        if (storedModifiedDate == null || !storedModifiedDate.equals(icon.lastModified())) {
            return null;
        }

        final String actualMd5Hash = tryComputeMd5Hash(icon);
        if (actualMd5Hash == null) {
            return null;
        }

        final String storedMd5Hash = configurationMap.getIconHash();
        // Since the configurationMap may have been produced by reading a configuration file from the file system, we are not guaranteed to
        // obtain an icon hash from the configurationMap, i.e., storedMd5Hash may be null. We are, however, guaranteed that actualMd5Hash is
        // not null, as even an empty byte array has a non-null md5 hash. Thus, the expression actualMd5Hash.equals(storedMd5Hash) is
        // guaranteed to not throw a NullPointerException.
        if (actualMd5Hash.equals(storedMd5Hash)) {
            return readPrescaledIconFile(toolDirFile, iconSize.getPath());
        } else {
            // The actual icon has changed since we produced the pre-scaled version. Hence, we cannot use the pre-scaled icons. If we later
            // decide to re-scale the new icon upon such a failed attempt to read a pre-scaled version, this would be the place to do so.
            return null;
        }
    }

    /**
     * If hashing fails, e.g., due to being unable to read the given file, a warning is logged and null is returned.
     * 
     * @param file The file to be hashed. Must not be null.
     * @return A string containing the MD5 hashsum of the contents of the given file. May be null if reading the file failed.
     */
    private String tryComputeMd5Hash(final File file) {
        try {
            final byte[] iconContent = fileAccess.readToByteArray(file);
            return hashingService.md5Hex(iconContent);
        } catch (IOException e) {
            LOGGER.warn("Could not read tool icon, using default icon instead");
            return null;
        }
    }

    /**
     * @param toolDirFile A file describing the location of the tool configuration folder.
     * @param path The path to the prescaled icon, relative to the tool integration folder.
     * @return A byte array containing the icon in the requested size if such an icon already exists. Null otherwise, i.e., if no pre-scaled
     *         icon of the desired size exists or if there is an error during reading that file.
     */
    private byte[] readPrescaledIconFile(final File toolDirFile, final String path) {
        final File iconFile = fileAccess.createFile(toolDirFile, path);
        if (iconFile.exists()) {
            return tryReadToByteArray(iconFile);
        } else {
            return null;
        }
    }

    private byte[] getLivescaledIcon(int size, ConfigurationMap configurationMap, File toolDirFile) {
        final String iconPath = configurationMap.getIconPath();
        if (iconPath == null || iconPath.isEmpty()) {
            return null;
        }

        final File sourceIcon = getIconFile(iconPath, toolDirFile);
        final boolean iconAvailable = sourceIcon.exists() && sourceIcon.isFile();
        if (!iconAvailable) {
            return null;
        }

        final File tempFile;
        try {
            tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("icon_" + size + "*.png");
        } catch (IOException e) {
            LOGGER.debug("Could not create temporary file for rescaling icon, using default icon");
            return null;
        }

        final BufferedImage image = tryReadToImage(sourceIcon);
        if (image == null) {
            LOGGER.debug("Could not read tool icon, using default icon");
            return null;
        }

        try {
            BufferedImage bi = imageService.resize(image, size);
            if (bi != null && tempFile != null) {
                // We write the resized image to a temporary file and read it back in oder to obtain PNG encoding
                imageService.write(bi, "PNG", tempFile);
                final byte[] iconArray = fileAccess.readToByteArray(tempFile);
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFile);
                return iconArray;
            } else {
                return null;
            }
            // We do not need to close the imageInputStream here, as ImageIO.read(...) already closed the input stream according to the
            // documentation.
        } catch (IOException e) {
            LOGGER.debug("Could not resize tool icon, using default icon");
            return null;
        }
    }

    private BufferedImage tryReadToImage(final File iconFile) {
        try {
            return imageService.readImage(iconFile);
        } catch (IOException e) {
            LOGGER.debug("IOException during reading tool icon, using default icon");
            return null;
        }
    }

    /**
     * If an error occurs during reading the iconFile, this error is logged and null is returned.
     * 
     * @param iconFile The file to be read to a byte array.
     * @return A byte array containing the icon or null if during reading the icon an error occurred.
     */
    private byte[] tryReadToByteArray(File iconFile) {
        try {
            return fileAccess.readToByteArray(iconFile);
        } catch (IOException e) {
            final String message =
                String.format("Error when reading icon at %s: ", iconFile.getAbsolutePath());
            LOGGER.warn(message, e);
            return null;
        }
    }

    /**
     * If the icon path stored in the configurationMap is relative, we assume it to be relative to the tool integration folder. It may,
     * however, also be an absolute path referring to any icon.
     * 
     * @param iconPath The path (relative or absolute) to the icon.
     * @param toolDirFile A file describing the tool integration folder.
     * @return A file describing the tool icon as specified by the icon path.
     */
    private File getIconFile(String iconPath, File toolDirFile) {
        final boolean iconPathIsAbsolute = fileAccess.createFile(iconPath).isAbsolute();
        if (iconPathIsAbsolute) {
            return fileAccess.createFile(iconPath);
        } else {
            return fileAccess.createFile(toolDirFile, iconPath);
        }
    }

    /**
     * Returns the contents of the default icon for the given size. By convention, this is the icon stored at /resources/icons/ with the
     * name, e.g., tool16.png.
     * 
     * @param iconSize The size in pixels of the requested icon.
     * @return A square icon with border length size, or null, if an error occurred during reading the default icon for the given size.
     */
    private byte[] getDefaultIcon(int iconSize) {
        final String iconPath = String.format("/resources/icons/tool%s.png", iconSize);
        try (InputStream inputStream = ToolIntegrationServiceImpl.class.getResourceAsStream(iconPath)) {
            return fileAccess.toByteArray(inputStream);
        } catch (IOException e) {
            final String warningMessage =
                String.format("Could not load default icon of size %sx%s, expected at %s: ", iconSize, iconSize, iconPath);
            LOGGER.warn(warningMessage, e);
            return null;
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    void bindFileAccessService(FileAccessService newInstance) {
        fileAccess = newInstance;
    }

    void unbindFileAccessService(FileAccessService oldInstance) {
        fileAccess = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    void bindHashingService(HashingService newInstance) {
        hashingService = newInstance;
    }

    void unbindHashingService(HashingService oldInstance) {
        hashingService = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    void bindImageService(ImageService newInstance) {
        imageService = newInstance;
    }

    void unbindImageService(ImageService oldInstance) {
        imageService = null;
    }

}
