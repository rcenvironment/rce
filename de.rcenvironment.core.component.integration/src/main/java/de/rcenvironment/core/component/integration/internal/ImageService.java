/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.utils.incubator.ImageResize;

/**
 * Wrapper for the image-related functions required by {@link IconHelper}. Moved to a wrapper class in order to make {@link IconHelper}
 * testable.
 * 
 * @author Alexander Weinert
 */
@Component(service = ImageService.class)
public class ImageService {

    /**
     * Wraps ImageIO.read.
     * 
     * @param imageFile The image to be read
     * @return An object describing the image stored in imageFile
     * @throws IOException If an error occurs during reading the imageFile
     */
    public BufferedImage readImage(File imageFile) throws IOException {
        return ImageIO.read(imageFile);
    }

    /**
     * Wraps ImageResize.resize.
     * 
     * @param image The image to be rescaled
     * @param size The desired edge length of the resulting image
     * @return A square image with the given edge length
     */
    public BufferedImage resize(BufferedImage image, int size) {
        return ImageResize.resize(image, size);
    }

    /**
     * Wraps ImageIO.write.
     * 
     * @param image The image to be written
     * @param format A string describing the format to be used when writing the given image, e.g., "PNG"
     * @param destination The destination to which the encoded file shall be written
     * @throws IOException If an error occurs during writing the image.
     */
    public void write(BufferedImage image, String format, File destination) throws IOException {
        ImageIO.write(image, format, destination);
    }

}
