/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.swt.graphics.Font;

/**
 * Abstract singleton holder for font manager implementations. Added to separate instance handling
 * and bundle activation/disposal from actual font management.
 * 
 * @author Sascha Zur
 */
public abstract class FontManager {

    private static volatile FontManager instance;

    protected FontManager() {}

    public static final FontManager getInstance() {
        return instance;
    }

    public static void setInstance(FontManager instance) {
        FontManager.instance = instance;
    }

    /**
     * @param font to retreive
     * @return SWT font
     */
    public abstract Font getFont(FontSource font);

    protected abstract void dispose();

}
