/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.FontSource;

/**
 * Implementation of {@link FontManager}.
 * 
 * @author Sascha Zur
 */
public class FontManagerImpl extends FontManager {

    private final Log log = LogFactory.getLog(getClass());

    private final Map<FontSource, Font> sharedFonts = new HashMap<>();

    @Override
    public Font getFont(FontSource fonts) {
        synchronized (sharedFonts) {
            Font font = sharedFonts.get(fonts);
            if (font == null) {
                font = new Font(Display.getCurrent(), fonts.getFontName(), fonts.getFontWidth(), fonts.getFontSwtFlag());
                sharedFonts.put(fonts, font);
            }
            return font;
        }
    }

    @Override
    protected void dispose() {
        synchronized (sharedFonts) {
            log.debug("Disposing " + sharedFonts.values().size() + " shared fonts");
            for (Font f : sharedFonts.values()) {
                f.dispose();
            }
            sharedFonts.clear();
        }
        log.debug("Font manager disposed");
    }

}
