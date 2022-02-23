/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

final class PaletteViewTestConstants {

    static final Image TEST_ICON =
        ImageDescriptor.createFromURL(PaletteViewContentProviderTest.class.getResource("/resources/testIcon.png")).createImage();

    static final String TOOL_TEST_VERSION = "0.0";

    private PaletteViewTestConstants() {}
}
