/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import org.eclipse.swt.graphics.Color;

import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ColorSource;

/**
 * Standard {@link ColorManager} implementation.
 * 
 * @author Tobias Rodehutskors
 */
public class ColorManagerImpl extends ColorManager {

    private final Map<ColorSource, Color> sharedColors = new HashMap<>();

    private final Log log = LogFactory.getLog(getClass());

    public ColorManagerImpl() {
        log.debug("Color manager initialized");
    }

    @Override
    public Color getSharedColor(ColorSource source) {
        synchronized (sharedColors) {
            Color color = sharedColors.get(source);
            if (color == null) {
                // TODO handle image failures explicitly?
                color = source.getColorDescriptor().createColor(null);
                sharedColors.put(source, color);
            }
            return color;
        }
    }

    @Override
    public void dispose() {
        synchronized (sharedColors) {
            log.debug("Disposing " + sharedColors.values().size() + " shared colors");
            for (Color color : sharedColors.values()) {
                color.dispose();
            }
            sharedColors.clear();
        }
        log.debug("Color manager disposed");
    }
}
