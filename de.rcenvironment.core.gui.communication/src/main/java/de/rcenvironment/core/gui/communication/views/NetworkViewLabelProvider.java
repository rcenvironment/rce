/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.gui.communication.views.internal.AnchorPoints;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.SelfRenderingNetworkViewNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * The {@link LabelProvider} for the network view.
 * 
 * @author Robert Mischke
 * @author David Scholz (added resource monitoring folder)
 */
public class NetworkViewLabelProvider extends LabelProvider {

    private Image nodeImage;

    private Image folderImage;

    private Image infoImage;

    private Image networkImage;

    private Image connectionImage;

    private Image disconnectImage;

    private boolean optionNodeIdsVisible;

    public NetworkViewLabelProvider() {
        createImages();
    }

    public void setNodeIdsVisible(boolean value) {
        optionNodeIdsVisible = value;
    }

    @Override
    public String getText(Object element) {
        String result;
        if (element instanceof SelfRenderingNetworkViewNode) {
            return ((SelfRenderingNetworkViewNode) element).getText();
        } else if (element instanceof ContributedNetworkViewNode && ((ContributedNetworkViewNode) element).getContributor() != null) {
            // TODO second "if" clause can be removed after full transition
            return ((ContributedNetworkViewNode) element).getContributor().getText(element);
        } else if (element == AnchorPoints.INSTANCES_PARENT_NODE) {
            return "Instances";
        } else if (element == AnchorPoints.MAIN_NETWORK_SECTION_PARENT_NODE) {
            return "RCE Network";
        } else if (element == AnchorPoints.SSH_REMOTE_ACCESS_SECTION_PARENT_NODE) {
            return "SSH Remote Access";
        } else if (element == AnchorPoints.SSH_UPLINK_SECTION_PARENT_NODE) {
            return "Uplink (Experimental)";
        } else if (element instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) element;
            // switch by node context
            switch (typedNode.getContext()) {
            case ROOT:
                NetworkGraphNode node = typedNode.getNode();
                result = typedNode.getDisplayNameOfNode();
                if (optionNodeIdsVisible) {
                    result += "  [" + node.getNodeId().getInstanceNodeSessionIdString() + "] ";
                }
                if (typedNode.isWorkflowHost()) {
                    result += " <Workflow Host>";
                }
                if (typedNode.isLocalNode()) {
                    result += " <Self>";
                }
                return result;
            case RAW_NODE_PROPERTIES_FOLDER:
                return "Raw Node Properties";
            case RAW_NODE_PROPERTY:
                return typedNode.getDisplayText();
            default:
                return "<error>";
            }
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
        if (element instanceof SelfRenderingNetworkViewNode) {
            return ((SelfRenderingNetworkViewNode) element).getImage();
        } else if (element instanceof ContributedNetworkViewNode && ((ContributedNetworkViewNode) element).getContributor() != null) {
            // TODO second "if" clause can be removed after full transition
            return ((ContributedNetworkViewNode) element).getContributor().getImage(element);
        } else if (element instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) element;
            // switch by node context
            switch (typedNode.getContext()) {
            case ROOT:
                result = nodeImage;
                break;
            case RESOURCE_MONITORING_FOLDER:
            case RAW_NODE_PROPERTIES_FOLDER:
                result = folderImage;
                break;
            case RAW_NODE_PROPERTY:
                result = infoImage;
                break;
            default:
                result = null;
            }
        } else if (element instanceof NetworkGraphLink) {
            result = connectionImage;
        } else if (element == AnchorPoints.INSTANCES_PARENT_NODE) {
            result = nodeImage;
        } else if (element == AnchorPoints.MAIN_NETWORK_SECTION_PARENT_NODE) {
            result = networkImage;
        } else if (element == AnchorPoints.SSH_REMOTE_ACCESS_SECTION_PARENT_NODE) {
            result = networkImage;
        } else if (element == AnchorPoints.SSH_UPLINK_SECTION_PARENT_NODE) {
            result = networkImage;
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
    }

    private void createImages() {
        ImageManager imageManager = ImageManager.getInstance();
        folderImage = imageManager.getSharedImage(StandardImages.FOLDER_16);
        infoImage = imageManager.getSharedImage(StandardImages.INFORMATION_16);
        nodeImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/node.png")).createImage(); //$NON-NLS-1$
        networkImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/network.gif")).createImage(); //$NON-NLS-1$
        connectionImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/connect.png")).createImage(); //$NON-NLS-1$
        disconnectImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/disconnect.png")).createImage(); //$NON-NLS-1$
    }

    private void disposeImages() {
        nodeImage.dispose();
        networkImage.dispose();
        connectionImage.dispose();
        disconnectImage.dispose();
    }

}
