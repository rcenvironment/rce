/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.gui;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DefaultHistoryDataItemSubtreeBuilder;

/**
 * @author Alexander Weinert
 */
public class EndpointAdapterDataItemSubtreeBuilder extends DefaultHistoryDataItemSubtreeBuilder {

    private static final Image INPUTADAPTER_ICON;

    private static final Image OUTPUTADAPTER_ICON;

    static {
        final String bundleName = "de.rcenvironment.core.component.integration.workflow";
        final String inputIconName = "inputadapter_16.png";
        URL inputIconUrl = ComponentUtils.readIconURL(bundleName, inputIconName);
        if (inputIconUrl != null) {
            INPUTADAPTER_ICON = ImageDescriptor.createFromURL(inputIconUrl).createImage();
        } else {
            INPUTADAPTER_ICON = null;
        }

        final String outputIconName = "outputadapter_16.png";
        URL outputIconUrl = ComponentUtils.readIconURL(bundleName, outputIconName);
        if (outputIconUrl != null) {
            OUTPUTADAPTER_ICON = ImageDescriptor.createFromURL(outputIconUrl).createImage();
        }  else {
            OUTPUTADAPTER_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { "InputAdapter", "OutputAdapter" };
    }

    @Override
    public Image getComponentIcon(String identifier) {
        if (identifier.startsWith("InputAdapter")) {
            return INPUTADAPTER_ICON;
        } else {
            return OUTPUTADAPTER_ICON;
        }
    }

}
