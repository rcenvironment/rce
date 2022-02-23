/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;

/**
 * Resize an image.
 * 
 * @author Markus Kunde
 * @author Sascha Zur
 */
public abstract class ImageResize {

    /**
     * Resize image to given size. Tries to keep proportion.
     * 
     * @param image image
     * @param size maximum size (length) of one side.
     * @return BufferedImage
     */
    public static BufferedImage resize(BufferedImage image, int size) {
        return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, size, size, Scalr.OP_ANTIALIAS);
    }
}
