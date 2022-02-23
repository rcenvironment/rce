/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DefaultHistoryDataItemSubtreeBuilder;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Parametric study component.
 * 
 * @author Doreen Seider
 */
public class ConvergerHistoryDataItemSubtreeBuilder extends DefaultHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.converger.common";
        String iconName = "converger16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { ConvergerComponentConstants.COMPONENT_ID };
    }

    @Override
    public Image getComponentIcon(String identifier) {
        return COMPONENT_ICON;
    }

}
