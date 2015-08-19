/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.api.DisconnectReason;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * The {@link LabelProvider} for the network view.
 * 
 * @author Robert Mischke
 */
public class NetworkViewLabelProvider extends LabelProvider {

    private Image rceImage;
    
    private Image nodeImage;

    private Image folderImage;

    private Image infoImage;

    private Image networkImage;

    private Image connectionImage;

    private Image disconnectImage;

    private Image componentFallbackImage;

    private final Map<String, Image> componentIconCache;

    private boolean optionNodeIdsVisible;

    public NetworkViewLabelProvider() {
        componentIconCache = new HashMap<String, Image>();
        createImages();
    }

    public void setNodeIdsVisible(boolean value) {
        optionNodeIdsVisible = value;
    }

    @Override
    public String getText(Object element) {
        String result;
        if (element == NetworkViewContentProvider.NETWORK_ROOT_NODE) {
            return "Instances";
        } else if (element == NetworkViewContentProvider.CONNECTIONS_ROOT_NODE) {
            return "Connections";
        } else if (element instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) element;
            // switch by node context
            switch (typedNode.getContext()) {
            case ROOT:
                NetworkGraphNode node = typedNode.getNode();
                result = typedNode.getDisplayNameOfNode();
                if (optionNodeIdsVisible) {
                    result += "  [" + node.getNodeId().getIdString() + "] ";
                }
                if (typedNode.isWorkflowHost()) {
                    result += " <Workflow Host>";
                }
                if (typedNode.isLocalNode()) {
                    result += " <Self>";
                }
                return result;
            case PUBLISHED_COMPONENTS_FOLDER:
                return "Published Components";
            case LOCAL_COMPONENTS_FOLDER:
                return "Local Components";
            case COMPONENT_INSTALLATION:
                ComponentInterface componentInterface = typedNode.getComponentInstallation().getComponentRevision().getComponentInterface();
                // Should be improved because using plain ids here is weird.
                // Plain ids are used to not introduce a new dependency to core.component.integration as this is not good from a (kind of)
                // communication bundle. But as the network view is not showing only communication stuff anymore, this dependency thing is
                // probably obsolete -- seid_do, Aug 2014
                if (componentInterface.getVersion() != null
                    && componentInterface.getIdentifier().startsWith("de.rcenvironment.integration.common.")
                    || componentInterface.getIdentifier().startsWith("de.rcenvironment.integration.cpacs.")) {
                    return String.format("%s (%s)", componentInterface.getDisplayName(), componentInterface.getVersion());
                } else {
                    return String.format("%s", componentInterface.getDisplayName());
                }
            case RAW_NODE_PROPERTIES_FOLDER:
                return "Raw Node Properties";
            case RAW_NODE_PROPERTY:
                return typedNode.getDisplayText();
            default:
                return "<error>";
            }
        } else if (element instanceof ConnectionSetup) {
            final ConnectionSetup typedElement = (ConnectionSetup) element;
            String subState = "";
            ConnectionSetupState connectionState = typedElement.getState();
            DisconnectReason disconnectReason = typedElement.getDisconnectReason();
            if ((connectionState == ConnectionSetupState.DISCONNECTED || connectionState == ConnectionSetupState.DISCONNECTING)
                && (disconnectReason != null)) {
                subState = ": " + disconnectReason.getDisplayText();
            }
            result = String.format("%s (%s%s)", typedElement.getDisplayName(), connectionState.getDisplayText(), subState);
        } else if (element instanceof NetworkGraphLink) {
            result = ((NetworkGraphLink) element).getLinkId(); // TODO
        } else {
            result = element.toString();
        }
        return result;
    }

    @Override
    public Image getImage(Object element) {
        Image result = null;
        if (element == NetworkViewContentProvider.NETWORK_ROOT_NODE) {
            result = networkImage;
        } else if (element == NetworkViewContentProvider.CONNECTIONS_ROOT_NODE) {
            result = disconnectImage;
        } else if (element instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) element;
            // switch by node context
            switch (typedNode.getContext()) {
            case ROOT:
                result = nodeImage;
                break;
            case PUBLISHED_COMPONENTS_FOLDER:
            case LOCAL_COMPONENTS_FOLDER:
            case RAW_NODE_PROPERTIES_FOLDER:
                result = folderImage;
                break;
            case COMPONENT_INSTALLATION:
                ComponentInstallation installation = typedNode.getComponentInstallation();
                if (installation != null) {
                    // FIXME improve caching key; temporary
                    String cacheKey = installation.getInstallationId();
                    Image image = componentIconCache.get(cacheKey);
                    if (image == null) {
                        try {
                            byte[] iconData = installation.getComponentRevision().getComponentInterface().getIcon16();
                            // TODO review: dispose Image instance? or cache Image instance instead?
                            ImageDescriptor iDescr =
                                ImageDescriptor.createFromImage(new Image(Display.getCurrent(), new ByteArrayInputStream(iconData)));
                            image = iDescr.createImage();
                        } catch (RuntimeException e) {
                            image = rceImage; // set fallback in case of errors
                        }
                        componentIconCache.put(cacheKey, image);
                    }
                    result = image;
                }
                break;
            case RAW_NODE_PROPERTY:
                result = infoImage;
                break;
            default:
                result = null;
            }
        } else if (element instanceof ConnectionSetup) {
            ConnectionSetup typedNode = (ConnectionSetup) element;
            if (typedNode.getState() == ConnectionSetupState.CONNECTED) {
                result = connectionImage;
            } else {
                result = disconnectImage;
            }
        } else if (element instanceof NetworkGraphLink) {
            result = connectionImage;
        }
        if (result == null) {
            // FIXME proper error/placeholder image?
            result = super.getImage(element);
        }
        return result;
    }

    @Override
    public void dispose() {
        disposeImages();
        // dispose cached component resources/icons
        for (Image image : componentIconCache.values()) {
            if (image != componentFallbackImage) {
                image.dispose();
            }
        }
    }

    private void createImages() {
        ImageManager imageManager = ImageManager.getInstance();
        rceImage = imageManager.getSharedImage(StandardImages.RCE_LOGO_16);
        folderImage = imageManager.getSharedImage(StandardImages.FOLDER_16);
        infoImage = imageManager.getSharedImage(StandardImages.INFORMATION_16);
        nodeImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/node.png")).createImage(); //$NON-NLS-1$
        networkImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/instances.gif")).createImage(); //$NON-NLS-1$
        connectionImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/connect.png")).createImage(); //$NON-NLS-1$
        disconnectImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/disconnect.png")).createImage(); //$NON-NLS-1$

        // reused resources/icons; do not dispose again
        componentFallbackImage = infoImage;
    }

    private void disposeImages() {
        nodeImage.dispose();
        networkImage.dispose();
        connectionImage.dispose();
        disconnectImage.dispose();
    }

}
