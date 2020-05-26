/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.swt.graphics.Color;

/**
 * Abstract singleton holder for color manager implementations. Added to separate instance handling and bundle activation/disposal from
 * actual color management.
 * 
 * @author Tobias Rodehutskors
 */
public abstract class ColorManager {

    private static volatile ColorManager instance;

    protected ColorManager() {
        // TODO Auto-generated constructor stub
    }

    public static final ColorManager getInstance() {
        return instance;
    }

    public static void setInstance(ColorManager instance) {
        ColorManager.instance = instance;
    }

    /**
     * Retrieves a shared SWT {@link Color} for the given {@link ColorSource}; if it does not exist yet, it is created. All shared colors
     * are automatically disposed on shutdown.
     * <p>
     * Callers of this method MUST NOT dispose the returned {@link Color}!
     * 
     * @param source a {@link ColorSource}
     * @return a shared SWT {@link Color}
     */
    public abstract Color getSharedColor(ColorSource source);

    protected abstract void dispose();
}
