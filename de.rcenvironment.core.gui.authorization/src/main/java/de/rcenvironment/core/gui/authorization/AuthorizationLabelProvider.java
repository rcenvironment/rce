/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.authorization;

import java.util.Optional;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ComponentImageManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Label Provider for the trees.
 *
 * @author Oliver Seebach
 * @author Robert Mischke
 * @author Jan Flink
 */
public class AuthorizationLabelProvider extends LabelProvider implements IColorProvider {

    private static final String ERROR_FALLBACK_LABEL_TEXT = "<Error>";

    private final AuthorizationService authorizationService;

    private final UserComponentIdMappingService userComponentIdMappingService;

    private final DistributedComponentKnowledgeService componentRegistryService;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private boolean showID = false;

    public AuthorizationLabelProvider() {
        super();
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        userComponentIdMappingService = serviceRegistryAccess.getService(UserComponentIdMappingService.class);
        authorizationService = serviceRegistryAccess.getService(AuthorizationService.class);

        componentRegistryService = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof NamedComponentAuthorizationSelector) {
            return getNameForComponentAuthorizationSelector((NamedComponentAuthorizationSelector) element);
        } else if (element instanceof AuthorizationAccessGroup) {
            return getNameForAuthorizationAccessGroup((AuthorizationAccessGroup) element);
        } else {
            return ERROR_FALLBACK_LABEL_TEXT; // should never happen
        }
    }
    
    private String getNameForComponentAuthorizationSelector(final NamedComponentAuthorizationSelector selector) {
        String internalId;
        try {
            internalId = userComponentIdMappingService.fromExternalToInternalId(selector.getId());
        } catch (OperationFailureException e) {
            logIdMappingFailure(selector.getId());
            return ERROR_FALLBACK_LABEL_TEXT;
        }
        final boolean isAvailable = componentRegistryService.getCurrentSnapshot().getAllLocalInstallations().stream()
            .anyMatch(entry -> entry.getComponentInterface().getIdentifier().equals(internalId));
        
        final StringBuilder displayNameBuilder = new StringBuilder(selector.getDisplayName());
        if (!isAvailable) {
            displayNameBuilder.append(" <not available>");
        }
        
        return displayNameBuilder.toString();
    }

    private void logIdMappingFailure(final String externalId) {
        final String logMessage = StringUtils.format("Could not map external component id %s to internal id", externalId);
        LogFactory.getLog(this.getClass()).warn(logMessage);
    }

    private String getNameForAuthorizationAccessGroup(AuthorizationAccessGroup group) {
        if (showID || authorizationService.isPublicAccessGroup(group)) {
            return group.getDisplayName();
        }
        return group.getName();
    }

    @Override
    public Image getImage(Object object) {
        if (object instanceof AuthorizationAccessGroup) {
            return getImageForAuthorizationAccessGroup((AuthorizationAccessGroup) object);
        } else if (object instanceof NamedComponentAuthorizationSelector) {
            return getImageForComponentAuthorizationSelector((NamedComponentAuthorizationSelector) object);
        } else {
            return getDefaultImage();
        }
    }

    private Image getDefaultImage() {
        return ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_16);
    }
    
    private Image getImageForAuthorizationAccessGroup(AuthorizationAccessGroup group) {
        if (authorizationService.isPublicAccessGroup(group)) {
            // public access group
            return AuthorizationConstants.PUBLIC_ICON.createImage();
        } else {
            // non-public group
            return AuthorizationConstants.GROUP_ICON.createImage();
        }
    }

    private Image getImageForComponentAuthorizationSelector(final NamedComponentAuthorizationSelector selector) {
        final Optional<DistributedComponentEntry> firstMatchingEntry =
            componentRegistryService.getCurrentSnapshot().getAllInstallations()
            .stream()
            // We only retain those installations whose id matches the external id for the given selector
            .filter(entry -> {
                try {
                    final String internalId = entry.getComponentInterface().getIdentifier();
                    final String externalId = userComponentIdMappingService.fromInternalToExternalId(internalId);
                    return externalId.equals(selector.getId());
                } catch (OperationFailureException e) {
                    logIdMappingFailure(selector.getId());
                    return false;
                }
            })
            .findFirst();

        if (firstMatchingEntry.isPresent()) {
            Image image = ComponentImageManager.getInstance().getIcon16Image(firstMatchingEntry.get().getComponentInterface());
            if (image != null) {
                return image;
            } else {
                // default icon for integrated tools
                return getDefaultIconForComponentAuthorizationSelector();
            }
        } else {
            return getDefaultIconForComponentAuthorizationSelector();
        }
    }

    private Image getDefaultIconForComponentAuthorizationSelector() {
        return ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATED_TOOL_DEFAULT_16);
    }

    public void setShowID(boolean show) {
        this.showID = show;
    }

    @Override
    public Color getBackground(Object arg0) {
        return null;
    }

    @Override
    public Color getForeground(Object element) {
        if (!(element instanceof NamedComponentAuthorizationSelector)) {
            return null;
        }
        final NamedComponentAuthorizationSelector selector = (NamedComponentAuthorizationSelector) element;
        final String internalId;
        try {
            internalId = userComponentIdMappingService.fromExternalToInternalId(selector.getId());
        } catch (OperationFailureException e) {
            logIdMappingFailure(selector.getId());
            return null;
        }
        final boolean isAvailable = componentRegistryService.getCurrentSnapshot().getAllLocalInstallations().stream()
            .anyMatch(entry -> entry.getComponentInterface().getIdentifier().equals(internalId));

        if (isAvailable) {
            return null;
        } else {
            return ColorManager.getInstance().getSharedColor(StandardColors.RCE_DOVE_GRAY);
        }
    }
}
