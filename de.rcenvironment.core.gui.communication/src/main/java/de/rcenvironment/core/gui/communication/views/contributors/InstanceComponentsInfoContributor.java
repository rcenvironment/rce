/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext.Context;
import de.rcenvironment.core.gui.communication.views.spi.ContributedNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Contributes instance subtrees showing the local and published components of a node.
 * 
 * @author Robert Mischke
 * @author Sascha Zur (original component image handling)
 * @author Doreen Seider (original component image handling)
 * @author Alexander Weinert (display of associated authorization groups)
 */
public class InstanceComponentsInfoContributor extends NetworkViewContributorBase {

    private static final int INSTANCE_ELEMENTS_PRIORITY = 10;

    private final Log log = LogFactory.getLog(getClass());

    private final AuthorizationService authorizationService;

    /**
     * A tree node containing node representing published or local component.
     * 
     * @author Robert Mischke
     */
    private class ComponentFolderNode implements ContributedNetworkViewNode {

        private final NetworkGraphNodeWithContext instanceNode;

        private final boolean typeIsPublic;

        ComponentFolderNode(NetworkGraphNodeWithContext instanceNode, boolean typeIsPublic) {
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getContributor().hashCode();
            if (instanceNode == null) {
                result = prime * result;
            } else {
                result = prime * result + instanceNode.hashCode() + Boolean.valueOf(typeIsPublic).hashCode();
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ComponentFolderNode other = (ComponentFolderNode) obj;
            if (typeIsPublic != other.typeIsPublic) {
                return false;
            }
            if (!getContributor().equals(other.getContributor())) {
                return false;
            }
            if (instanceNode == null) {
                if (other.instanceNode != null) {
                    return false;
                }
            } else if (!instanceNode.equals(other.instanceNode)) {
                return false;
            }
            return true;
        }

    }

    private Image folderImage;

    private final Map<String, Image> componentIconCache;

    private Image componentFallbackImage;

    public InstanceComponentsInfoContributor() {
        ImageManager imageManager = ImageManager.getInstance();
        folderImage = imageManager.getSharedImage(StandardImages.FOLDER_16);
        componentFallbackImage = imageManager.getSharedImage(StandardImages.RCE_LOGO_16);
        componentIconCache = new HashMap<>();

        authorizationService = ServiceRegistry.createAccessFor(this).getService(AuthorizationService.class);
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
            Collection<DistributedComponentEntry> publishedInstallations =
                currentModel.componentKnowledge.getKnownSharedInstallationsOnNode(parentNode.getNode().getNodeId(), true);
            if (publishedInstallations != null && !publishedInstallations.isEmpty()) {
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
                Collection<DistributedComponentEntry> localInstallations = currentModel.componentKnowledge.getLocalAccessInstallations();
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
            Collection<DistributedComponentEntry> publishedInstallations =
                currentModel.componentKnowledge.getKnownSharedInstallationsOnNode(instanceNode.getNode().getNodeId(), true);
            // note: the parent node is only created if the list is defined and not empty
            return createNodesForComponentInstallations(instanceNode, publishedInstallations);
        } else {
            Collection<DistributedComponentEntry> componentEntries = currentModel.componentKnowledge.getLocalAccessInstallations();
            // note: the parent node is only created if the list is defined and not empty
            return createNodesForComponentInstallations(instanceNode, componentEntries);
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
            final StringBuilder returnValueBuilder = new StringBuilder();

            if (typedNode.getComponentInstallation() != null) {
                final ComponentInterface componentInterface = typedNode.getComponentInstallation().getComponentInterface();

                returnValueBuilder.append(componentInterface.getDisplayName());
                // Should be improved because using plain ids here is weird.
                // Plain ids are used to not introduce a new dependency to core.component.integration as this is not good from a (kind of)
                // communication bundle. But as the network view is not showing only communication stuff anymore, this dependency thing is
                // probably obsolete -- seid_do, Aug 2014
                if (componentInterface.getVersion() != null
                    && componentInterface.getIdentifierAndVersion().startsWith("de.rcenvironment.integration.common.")
                    || componentInterface.getIdentifierAndVersion().startsWith("de.rcenvironment.integration.cpacs.")) {
                    returnValueBuilder.append(StringUtils.format(" (%s)", componentInterface.getVersion()));
                }
            } else {
                returnValueBuilder.append(typedNode.getDisplayText());
            }

            final Collection<AuthorizationAccessGroup> permissionSet =
                typedNode.getDistributedComponentEntry().getDeclaredPermissionSet().getAccessGroups();

            // If the permission set of a component that is shown in the network view is empty, it is a local unpublished component. In this
            // case it does not make sense to display information about the accessibility of the component, hence we omit the complete
            // suffix displaying this information.
            if (!permissionSet.isEmpty()) {
                returnValueBuilder.append(" <");

                final boolean componentIsPublic =
                    (permissionSet.size() == 1)
                        && authorizationService.isPublicAccessGroup(permissionSet.iterator().next());

                if (componentIsPublic) {
                    returnValueBuilder.append("public");
                } else {
                    returnValueBuilder.append("available via ");
                    returnValueBuilder.append(
                        // Map the access groups to their name and join the resulting strings with the separator ", "
                        permissionSet.stream().map(AuthorizationAccessGroup::getName).collect(Collectors.joining(", ")));
                }
                returnValueBuilder.append(">");
            }

            return returnValueBuilder.toString();
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
                        byte[] iconData = installation.getComponentInterface().getIcon16();
                        if (iconData != null) {
                            // TODO review: dispose Image instance? or cache Image instance instead?
                            ImageDescriptor iDescr =
                                ImageDescriptor.createFromImage(new Image(Display.getCurrent(), new ByteArrayInputStream(iconData)));
                            image = iDescr.createImage();
                        } else {
                            log.debug(
                                "Using fallback image for component \"" + installation.getInstallationId() + "\" as the icon data is null");
                            image = componentFallbackImage;
                        }
                    } catch (RuntimeException e) {
                        log.warn(
                            "Using fallback image for component \"" + installation.getInstallationId()
                                + "\" as an error occurred while parsing the image data: " + e.toString());
                        image = componentFallbackImage; // set fallback in case of errors
                    }
                    componentIconCache.put(cacheKey, image);
                }
                return image;
            } else {
                // This case occurs if we display a component installation that is inaccessible due to publication groups. Previously, the
                // following warning was logged:
                // 
                //      "Found a network view component node without a component installation: " + typedNode.getDisplayNameOfNode()
                // 
                // We retain this warning in this comment in order to allow future developers to find the point where this message was
                // issued in case they need to debug user-supplied logs that contain this warning message.
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
        Collection<DistributedComponentEntry> installations) {
        List<Object> result = new ArrayList<>();
        for (DistributedComponentEntry entry : installations) {
            ComponentInstallation installation = entry.getComponentInstallation();
            NetworkGraphNodeWithContext newChild = new NetworkGraphNodeWithContext(node, Context.COMPONENT_INSTALLATION, this);
            newChild.setComponentInstallation(installation);
            newChild.setDisplayText(entry.getDisplayName());
            newChild.setDistributedComponentEntry(entry);
            result.add(newChild);
        }
        final Object[] resultArray = result.toArray();
        Arrays.sort(resultArray);
        return resultArray;
    }

    private void assertIsComponentNode(NetworkGraphNodeWithContext typedNode) {
        if (typedNode.getContext() != Context.COMPONENT_INSTALLATION) {
            throw new IllegalStateException("Unexpected context: " + typedNode.getContext());
        }
    }

}
