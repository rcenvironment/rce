/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.remoteaccess;

import java.io.InputStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.apache.commons.io.IOUtils;

import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessConstants;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DefaultHistoryDataItemSubtreeBuilder;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Remote Access component.
 *
 * @author Brigitte Boden
 */
public class RemoteAccessHistoryDataItemSubtreeBuilder extends DefaultHistoryDataItemSubtreeBuilder {

    private static Image defaultIconImage;

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { SshRemoteAccessConstants.COMPONENT_ID.replace(".", "\\.") + ".*" };
    }

    @Override
    public Image getComponentIcon(String historyDataItemIdentifier) {
        if (defaultIconImage == null) {
            InputStream inputStream = getClass().getResourceAsStream("/icons/tool16.png");
            defaultIconImage = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), inputStream)).createImage();
            IOUtils.closeQuietly(inputStream);
        }
        return defaultIconImage;
    }

}
