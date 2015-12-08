/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext.Context;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Contributes instance subtrees showing the local and published components of a node.
 * 
 * @author Robert Mischke
 * @author Sascha Zur (original component image handling)
 * @author Doreen Seider (original component image handling)
 */
public class InstanceComponentsInfoContributor extends NetworkViewContributorBase {

    private static final int INSTANCE_ELEMENTS_PRIORITY = 10;

    /**
     * A tree node containing node representing published or local component.
     * 
     * @author Robert Mischke
     */
    private class ComponentFolderNode implements ContributedNetworkViewNode {

        private final NetworkGraphNodeWithContext instanceNode;

        private final boolean typeIsPublic;

        public ComponentFolderNode(NetworkGraphNodeWithContext instanceNode, boolean typeIsPublic) {
            this.instanceNode = instanceNode;
            this.typeIsPublic = typeIsPublic;
        }

        @Override
        public NetworkViewContributor getContributor() {
            return InstanceComponentsInfoContributor.this;
        }

        public NetworkGraphNodeWithContext getInstanceNode() {
            return instanceNode;
        }

        public boolean getTypeIsPublic() {
            return typeIsPublic;
        }

    }

    private Image folderImage;

    private final Map<String, Image> componentIconCache;

    private Image componentFallbackImage;

    public InstanceComponentsInfoContributor() {
        ImageManager imageManager = ImageManager.getInstance();
        folderImage = imageManager.getSharedImage(StandardImages.FOLDER_16);
        componentFallbackImage = imageManager.getSharedImage(StandardImages.RCE_LOGO_16);
        componentIconCache = new HashMap<String, Image>();

    }

    @Override
    public int getRootElementsPriority() {
        return 0; // disabled
    }

    @Override
    public Object[] getTopLevelElements(Object parentNode) {
        return null; // disabled
    }

    @Override
    public int getInstanceDataElementsPriority() {
        return INSTANCE_ELEMENTS_PRIORITY;
    }

    @Override
    public Object[] getChildrenForNetworkInstanceNode(NetworkGraphNodeWithContext parentNode) {
        List<Object> result = new ArrayList<>(2); // max. expected number
        if (currentModel.componentKnowledge != null) {
            // only show the "published" folder when there are published components
            Collection<ComponentInstallation> publishedInstallations =
                currentModel.componentKnowledge.getPublishedInstallationsOnNode(parentNode.getNode().getNodeId());
            if (publishedInstallations != null && publishedInstallations.size() != 0) {
                result.add(new ComponentFolderNode(parentNode, true));
            }
            if (parentNode.isLocalNode()) {
                result.add(new ComponentFolderNode(parentNode, false));
            }
        }
        return result.toArray();
    }

    @Override
    public boolean hasChildren(Object parentNode) {
        if (parentNode instanceof ComponentFolderNode) {
            ComponentFolderNode typedNode = (ComponentFolderNode) parentNode;
            if (typedNode.getTypeIsPublic()) {
                return true; // otherwise, the folder wouldn't exist
            } else {
                Collection<ComponentInstallation> localInstallations = determineLocalComponents(typedNode.getInstanceNode());
                return !localInstallations.isEmpty();
            }
        }

        if (parentNode instanceof NetworkGraphNodeWithContext) {
            assertIsComponentNode((NetworkGraphNodeWithContext) parentNode); // consistency check
            return false;
        }
        throw newUnexpectedCallException();
    }

    @Override
    public Object[] getChildren(Object parentNode) {
        // only expecting ComponentFolderNode instances as parent
        final ComponentFolderNode typedParentNode = (ComponentFolderNode) parentNode;
        final NetworkGraphNodeWithContext instanceNode = typedParentNode.getInstanceNode();

        if (typedParentNode.getTypeIsPublic()) {
            Collection<ComponentInstallation> publishedInstallations =
                currentModel.componentKnowledge.getPublishedInstallationsOnNode(instanceNode.getNode().getNodeId());
            // note: the parent node is only created if the list is defined and not empty
            return createNodesForComponentInstallations(instanceNode, publishedInstallations);
        } else {
            Collection<ComponentInstallation> localInstallations = determineLocalComponents(instanceNode);
            return createNodesForComponentInstallations(instanceNode, localInstallations);
        }
    }

    @Override
    public Object getParent(Object node) {
        if (node instanceof ComponentFolderNode) {
            return ((ComponentFolderNode) node).getInstanceNode();
        } else if (node instanceof NetworkGraphNodeWithContext) {
            return ((NetworkGraphNodeWithContext) node).getParent();
        } else {
            return null; // error - will trigger an upstream warning
        }
    }

    @Override
    public String getText(Object node) {
        if (node instanceof ComponentFolderNode) {
            ComponentFolderNode typedNode = (ComponentFolderNode) node;
            if (typedNode.getTypeIsPublic()) {
                return "Published Components";
            } else {
                return "Local Components";
            }
        }
        if (node instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) node;
            assertIsComponentNode(typedNode); // consistency check
            ComponentInterface componentInterface = typedNode.getComponentInstallation().getComponentRevision().getComponentInterface();
            // Should be improved because using plain ids here is weird.
            // Plain ids are used to not introduce a new dependency to core.component.integration as this is not good from a (kind of)
            // communication bundle. But as the network view is not showing only communication stuff anymore, this dependency thing is
            // probably obsolete -- seid_do, Aug 2014
            if (componentInterface.getVersion() != null
                && componentInterface.getIdentifier().startsWith("de.rcenvironment.integration.common.")
                || componentInterface.getIdentifier().startsWith("de.rcenvironment.integration.cpacs.")) {
                return StringUtils.format("%s (%s)", componentInterface.getDisplayName(), componentInterface.getVersion());
            } else {
                return StringUtils.format("%s", componentInterface.getDisplayName());
            }
        }
        throw newUnexpectedCallException();
    }

    @Override
    public Image getImage(Object node) {
        if (node instanceof ComponentFolderNode) {
            return folderImage;
        }
        if (node instanceof NetworkGraphNodeWithContext) {
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) node;
            assertIsComponentNode(typedNode); // consistency check
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
                        image = componentFallbackImage; // set fallback in case of errors
                    }
                    componentIconCache.put(cacheKey, image);
                }
                return image;
            } else {
                // fallback; should never happen
                LogFactory.getLog(getClass()).warn(
                    "Found a network view component node without a component installation: " + typedNode.getDisplayNameOfNode());
                return componentFallbackImage;
            }
        }
        throw newUnexpectedCallException();
    }

    @Override
    public void dispose() {
        // dispose cached component resources/icons
        for (Image image : componentIconCache.values()) {
            if (image != componentFallbackImage) {
                image.dispose();
            }
        }
        // note: not disposing folderImage and componentFallbackImage, as they are shared
    }

    private Object[] createNodesForComponentInstallations(NetworkGraphNodeWithContext node,
        Collection<ComponentInstallation> installations) {
        Object[] result = new Object[installations.size()];
        int i = 0;
        for (ComponentInstallation installation : installations) {
            if (installation == null) {
                LogFactory.getLog(getClass()).warn("Skipping 'null' component installation for node " + node.getNode().getNodeId());
                continue;
            }
            NetworkGraphNodeWithContext newChild = new NetworkGraphNodeWithContext(node, Context.COMPONENT_INSTALLATION, this);
            newChild.setComponentInstallation(installation);
            result[i++] = newChild;
        }
        Arrays.sort(result);
        return result;
    }

    private Collection<ComponentInstallation> determineLocalComponents(NetworkGraphNodeWithContext typedParentNode) {
        Collection<ComponentInstallation> localInstallations = currentModel.componentKnowledge.getLocalInstallations();
        // hide all published local components from "local components" list
        Collection<ComponentInstallation> publishedLocalInstallations =
            currentModel.componentKnowledge.getPublishedInstallationsOnNode(typedParentNode.getNode().getNodeId());
        if (publishedLocalInstallations != null) {
            localInstallations = new ArrayList<ComponentInstallation>(localInstallations);
            localInstallations.removeAll(publishedLocalInstallations);
        }
        return localInstallations;
    }

    private void assertIsComponentNode(NetworkGraphNodeWithContext typedNode) {
        if (typedNode.getContext() != Context.COMPONENT_INSTALLATION) {
            throw new IllegalStateException("Unexpected context: " + typedNode.getContext());
        }
    }

}
