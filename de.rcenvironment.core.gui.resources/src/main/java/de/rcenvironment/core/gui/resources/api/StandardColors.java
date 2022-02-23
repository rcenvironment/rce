/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.swt.graphics.RGB;

/**
 * A collection of standard colors.
 * 
 * The names of the colors have been determined by 'Name that Color": http://chir.ag/projects/name-that-color/
 * 
 * @author Tobias Rodehutskors
 */

public enum StandardColors implements ColorSource {

    // Colors are sorted ascending by their red component first, then their green component and last by their blue component.

    /**
     * Black.
     */
    RCE_BLACK(fromRGB(0x00, 0x00, 0x00)),

    /**
     * Dove Gray.
     */
    RCE_DOVE_GRAY(fromRGB(0x64, 0x64, 0x64)),
    
    /**
     * Color for Imitation mode.
     */
    RCE_IMITATION(fromRGB(0xCE, 0xDE, 0xFF)),
    
    /**
     * Alto.
     */
    RCE_GREY(fromRGB(0xDD, 0xDD, 0xDD)),

    /**
     * Geraldine. 
     */
    RCE_GERALDINE(fromRGB(0xFF, 0x7D, 0x7D)),
    
    /**
     * Pastel Pink.
     */
    RCE_COLOR_2(fromRGB(0xFF, 0xCC, 0xD2)),

    /**
     * Early Dawn.
     */
    RCE_COLOR_1(fromRGB(0xFF, 0xF7, 0xE7)),
    
    /**
     * White.
     */
    RCE_WHITE(fromRGB(0xFF, 0xFF, 0xFF)),

    /**
     * Light grey.
     */
    RCE_LIGHT_GREY(fromRGB(0xEF, 0xEF, 0xEF)),

    /**
     * Light blue.
     */
    RCE_LIGHT_BLUE(fromRGB(0xed, 0xf4, 0xff));

    private final ColorDescriptor colorDescriptor;

    StandardColors(ColorDescriptor colorDescriptor) {
        this.colorDescriptor = colorDescriptor;
    }

    @Override
    public ColorDescriptor getColorDescriptor() {
        return colorDescriptor;
    }

    private static ColorDescriptor fromRGB(int r, int g, int b) {
        return ColorDescriptor.createFrom(new RGB(r, g, b));
    }
}
