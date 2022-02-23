/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DefaultHistoryDataItemSubtreeBuilder;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Parametric study component.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class ParametricStudyHistoryDataItemSubtreeBuilder extends DefaultHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.parametricstudy.common";
        String iconName = "parametricStudy16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { ParametricStudyComponentConstants.COMPONENT_ID };
    }

    @Override
    public Image getComponentIcon(String identifier) {
        return COMPONENT_ICON;
    }

}
