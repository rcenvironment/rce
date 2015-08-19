/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.osgi.framework.Bundle;

/**
 * Methods copied from {@link org.eclipse.ui.internal.ide.IDEWorkbenchAdvisor}.
 * 
 * @author Juergen Klein
 * @author Christian Weiss
 */
@SuppressWarnings("restriction")
public abstract class WorkbenchAdvisorDelegate {

    private WorkbenchAdvisorDelegate() {
        super();
    }

    /**
     * Declares all IDE-specific workbench images. This includes both "shared" images (named in
     * {@link IDE.SharedImages}) and internal images (named in
     * {@link org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages}).
     * 
     * @see org.eclipse.ui.internal.ide.IDEWorkbenchAdvisor#declareImage
     */
    protected static void declareWorkbenchImages(IWorkbenchConfigurer configurer) {
        final String iconsPath = "$nl$/icons/full/";
        final String pathELocalTool = iconsPath + "elcl16/";
        final String pathDLocalTool = iconsPath + "dlcl16/";
        final String pathETool = iconsPath + "etool16/";
        final String pathDTool = iconsPath + "dtool16/";
        final String pathObject = iconsPath + "obj16/";
        final String pathWizban = iconsPath + "wizban/";

        Bundle ideBundle = Platform.getBundle(IDEWorkbenchPlugin.IDE_WORKBENCH);

        String buildExecGIF = "build_exec.gif";
        String searchSrcGIF = "search_src.gif";

        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_BUILD_EXEC, pathETool + buildExecGIF,
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_BUILD_EXEC_HOVER, pathETool + buildExecGIF,
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_BUILD_EXEC_DISABLED, pathDTool + buildExecGIF,
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_SEARCH_SRC, pathETool + searchSrcGIF, false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_SEARCH_SRC_HOVER, pathETool + searchSrcGIF,
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_SEARCH_SRC_DISABLED, pathDTool + searchSrcGIF,
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_NEXT_NAV, pathETool + "next_nav.gif", false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PREVIOUS_NAV, pathETool + "prev_nav.gif", false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWPRJ_WIZ, pathWizban + "newprj_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFOLDER_WIZ, pathWizban + "newfolder_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFILE_WIZ, pathWizban + "newfile_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_IMPORTDIR_WIZ, pathWizban + "importdir_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_IMPORTZIP_WIZ, pathWizban + "importzip_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_EXPORTDIR_WIZ, pathWizban + "exportdir_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_EXPORTZIP_WIZ, pathWizban + "exportzip_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_RESOURCEWORKINGSET_WIZ,
                              pathWizban + "workset_wiz.png", false); //$NON-NLS-1$
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_DLGBAN_SAVEAS_DLG, pathWizban + "saveas_wiz.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_DLGBAN_QUICKFIX_DLG, pathWizban + "quick_fix.png",
            false);
        declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT, pathObject + "prj_obj.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, pathObject + "cprj_obj.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OPEN_MARKER, pathELocalTool + "gotoobj_tsk.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ELCL_QUICK_FIX_ENABLED, pathELocalTool
            + "smartmode_co.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_DLCL_QUICK_FIX_DISABLED, pathDLocalTool
            + "smartmode_co.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJS_TASK_TSK, pathObject + "taskmrk_tsk.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDE.SharedImages.IMG_OBJS_BKMRK_TSK, pathObject + "bkmrk_tsk.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_COMPLETE_TSK, pathObject + "complete_tsk.gif",
            true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INCOMPLETE_TSK, pathObject + "incomplete_tsk.gif",
            true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_ITEM, pathObject + "welcome_item.gif",
            true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_BANNER, pathObject + "welcome_banner.gif",
            true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH, pathObject + "error_tsk.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WARNING_PATH, pathObject + "warn_tsk.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INFO_PATH, pathObject + "info_tsk.gif", true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_LCL_FLAT_LAYOUT, pathELocalTool + "flatLayout.gif",
            true);
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_LCL_HIERARCHICAL_LAYOUT,
                              pathELocalTool + "hierarchicalLayout.gif", true); //$NON-NLS-1$
        declareWorkbenchImage(configurer, ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEM_CATEGORY, pathETool
            + "problem_category.gif", true);
    }

    /**
     * Declares an IDE-specific workbench image.
     * 
     * @param ideBundle bundle
     * 
     * @param symbolicName the symbolic name of the image
     * @param path the path of the image file; this path is relative to the base of the IDE plug-in
     * @param shared <code>true</code> if this is a shared image, and <code>false</code> if this is
     *        not a shared image
     * @see org.eclipse.ui.internal.ide.IDEWorkbenchAdvisor#declareImage
     */
    private static void declareWorkbenchImage(IWorkbenchConfigurer configurer, Bundle ideBundle, String symbolicName, String path,
        boolean shared) {
        URL url = FileLocator.find(ideBundle, new Path(path), null);
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        configurer.declareImage(symbolicName, desc, shared);
    }

}
