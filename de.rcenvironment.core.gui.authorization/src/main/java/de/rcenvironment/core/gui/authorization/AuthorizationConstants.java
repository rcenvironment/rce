/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Constants for the authorization views.
 *
 * @author Jan Flink
 */
final class AuthorizationConstants {

    protected static final ImageDescriptor GROUP_ICON =
        ImageDescriptor.createFromURL(AuthorizationGroupDialog.class.getResource("/resources/icons/group.png"));

    protected static final ImageDescriptor PUBLIC_ICON =
        ImageDescriptor.createFromURL(AuthorizationGroupDialog.class.getResource("/resources/icons/Public_group.png"));

    protected static final ImageDescriptor SORT_ASC =
        ImageDescriptor.createFromURL(AuthorizationGroupDialog.class.getResource("/resources/icons/sortAlphaAsc.gif"));

    protected static final ImageDescriptor SORT_DESC =
        ImageDescriptor.createFromURL(AuthorizationGroupDialog.class.getResource("/resources/icons/sortAlphaDesc.gif"));

    //TODO find and insert fitting icons
    protected static final ImageDescriptor LABEL_TYPE_INTERNAL =
        ImageDescriptor.createFromURL(AuthorizationGroupDialog.class.getResource("/resources/icons/lock.png"));

    protected static final ImageDescriptor LABEL_TYPE_DISPLAY =
        ImageDescriptor.createFromURL(AuthorizationGroupDialog.class.getResource("/resources/icons/lock.png"));
    
    protected static final ImageDescriptor COLLAPSE_ALL_ICON = PlatformUI.getWorkbench().getSharedImages()
        .getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL);

    protected static final ImageDescriptor EXPAND_ALL_ICON = ImageDescriptor
        .createFromURL(ComponentPublishingView.class.getResource("/resources/icons/expandall.gif"));

    protected static final int ASCENDING = 1;

    protected static final int DESCENDING = -1;

    protected static final int SCROLL_COMPOSITE_MINIMUM_HEIGHT = 200;

    protected static final int SCROLL_COMPOSITE_MINIMUM_WIDTH = 250;

    protected static final int DIALOG_HEIGHT_HINT = 400;

    private AuthorizationConstants() {}
}
