/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Provides static access to various {@link Image}s and {@link ImageDescriptor}s used in the {@link DataManagementBrowser}.
 * 
 * @author Markus Litz
 * @author Robert Mischke
 * @author Jan Flink
 */

public abstract class DMBrowserImages {

    /**
     * The plugin ID used to acquire {@link ImageDescriptor}s.
     */
    // Note: made public as checkstyle complains about order, but these are needed by other fields
    public static final String PLUGIN_ID = "de.rcenvironment.core.gui.datamanagement";

    /**
     * The path to custom image files.
     */
    // Note: made public as checkstyle complains about order, but these are needed by other fields
    public static final String ICONS_PATH_PREFIX = "resources/icons/";

    /**
     * "Timeline" icon.
     */
    public static final Image IMG_TIMELINE =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "timeline.gif").createImage();

    /**
     * "Timeline By Components" icon.
     */
    public static final Image IMG_COMPONENTS =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "components.gif").createImage();

    /**
     * "Sort" icon.
     */
    public static final ImageDescriptor IMG_SORT_ALPHABETICAL_ASC =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "sortAlphaAsc.gif");

    /**
     * "Sort" icon.
     */
    public static final ImageDescriptor IMG_SORT_ALPHABETICAL_DESC =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "sortAlphaDesc.gif");

    /**
     * "Sort" icon.
     */
    public static final ImageDescriptor IMG_SORT_TIMESTAMP =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "waiting.gif");

    /**
     * "Sort" icon.
     */
    public static final ImageDescriptor IMG_SORT_TIMESTAMP_ASC =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "sortTimeAsc.gif");

    /**
     * "Sort" icon.
     */
    public static final ImageDescriptor IMG_SORT_TIMESTAMP_DESC =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "sortTimeDesc.gif");

    /**
     * Generic default node/object icon.
     */
    public static final Image IMG_DEFAULT = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "default.gif")
        .createImage();
    
    /**
     * Generic default node/object icon.
     */
    public static final Image IMG_WF_RUN_INFO = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "runinfo.gif")
        .createImage();

    /**
     * "Collapse all" icon.
     */
    public static final ImageDescriptor IMG_DESC_COLLAPSE_ALL =
        PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL);

    /**
     * "Collapse all" icon.
     */
    public static final Image IMG_COLLAPSE_ALL = IMG_DESC_COLLAPSE_ALL.createImage();

    /**
     * "Auto refresh" icon.
     */
    public static final ImageDescriptor IMG_DESC_AUTOREFRESH =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "autoRefresh.gif");

    /**
     * "Refresh Node" icon.
     */
    public static final ImageDescriptor IMG_DESC_REFRESH_NODE =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "refreshNode.gif");

    /**
     * "Delete files" icon.
     */
    public static final ImageDescriptor IMG_DESC_DELETE_FILES =
        AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH_PREFIX + "deleteFiles.gif");

    private DMBrowserImages() {}

}
