/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;

/**
 * A collection of standard images, some provided by this bundle, some reused from the underlying Eclipse platform.
 * 
 * @author Robert Mischke
 */

public enum StandardImages implements ImageSource {

    /**
     * 16x16 RCE logo.
     */
    RCE_LOGO_16(fromLocalBundle("logo16.png")),

    /**
     * 24x24 RCE logo.
     */
    RCE_LOGO_24(fromLocalBundle("logo24.png")),

    /**
     * 13x13 UNCHECK logo.
     */
    CHECK_UNCHECKED(fromLocalBundle("checkbox_u.png")),

    /**
     * 13x13 CHECK logo.
     */
    CHECK_CHECKED(fromLocalBundle("checkbox_c.png")),
    
    /**
     * 13x13 CHECK_DISABLED logo.
     */
    CHECK_DISABLED(fromLocalBundle("checkbox_d.png")),

    /**
     * 16x16 TABLE logo.
     */
    TABLE(fromLocalBundle("table.png")),

    /**
     * 16x16 TREE logo.
     */
    TREE(fromLocalBundle("treeview.png")),

    /**
     * 32x32 RCE logo.
     */
    RCE_LOGO_32(fromLocalBundle("logo32.png")),

    /**
     * 24x24 RCE logo only in grey.
     */
    RCE_LOGO_24_GREY(fromLocalBundle("logo24_grey.png")),

    /**
     * 32x32 RCE logo only in grey.
     */
    RCE_LOGO_32_GREY(fromLocalBundle("logo32_grey.png")),

    /**
     * Workflow icon.
     */
    WORKFLOW_16(fromLocalBundle("workflow16.gif")),

    /**
     * Workflow icon.
     */
    WORKFLOW_DISABLED_16(fromLocalBundle("workflow16_disabled.gif")),

    /**
     * 16x16 "debug" icon.
     */
    DEBUG_16(fromLocalBundle("debug.gif")),

    /**
     * 16x16 "information" icon.
     */
    INFORMATION_16(fromEclipseShared(ISharedImages.IMG_OBJS_INFO_TSK)),

    /**
     * 16x16 "warning" icon.
     */
    WARNING_16(fromEclipseShared(ISharedImages.IMG_OBJS_WARN_TSK)),

    /**
     * 16x16 "error" icon.
     */
    ERROR_16(fromEclipseShared(ISharedImages.IMG_OBJS_ERROR_TSK)),

    /**
     * 16x16 folder icon.
     */
    FOLDER_16(fromEclipseShared(ISharedImages.IMG_OBJ_FOLDER)),

    /**
     * 16x16 file icon.
     */
    FILE_16(fromEclipseShared(ISharedImages.IMG_OBJ_FILE)),

    /**
     * Files icon.
     */
    FILES_16(fromEclipseShared(ISharedImages.IMG_TOOL_COPY)),

    /**
     * Datatype Shorttext icon.
     */
    DATATYPE_SHORTTEXT_16(fromLocalBundle("datatype_shortText16.gif")),

    /**
     * Datatype Boolean icon.
     */
    DATATYPE_BOOLEAN_16(fromLocalBundle("datatype_boolean16.gif")),

    /**
     * Datatype Integer icon.
     */
    DATATYPE_INTEGER_16(fromLocalBundle("datatype_integer16.gif")),

    /**
     * Datatype Float icon.
     */
    DATATYPE_FLOAT_16(fromLocalBundle("datatype_float16.gif")),

    /**
     * Datatype Vector icon.
     */
    DATATYPE_VECTOR_16(fromLocalBundle("datatype_vector16.gif")),

    /**
     * Datatype Matrix icon.
     */
    DATATYPE_MATRIX_16(fromLocalBundle("datatype_matrix16.gif")),

    /**
     * Datatype Smalltable icon.
     */
    DATATYPE_SMALLTABLE_16(fromLocalBundle("datatype_smallTable16.gif")),

    /**
     * Datatype DateTime icon.
     */
    DATATYPE_DATETIME_16(fromLocalBundle("datatype_datetime16.gif")),

    /**
     * Datatype Indefinite icon.
     */
    DATATYPE_INDEFINITE_16(fromLocalBundle("datatype_indefinite16.gif")),

    /**
     * Datatype File icon.
     */
    DATATYPE_FILE_16(fromEclipseShared(ISharedImages.IMG_OBJ_FILE)),

    /**
     * Datatype Directory icon.
     */
    DATATYPE_DIRECTORY_16(fromEclipseShared(ISharedImages.IMG_OBJ_FOLDER)),

    /**
     * Input icon.
     */
    INPUT_16(fromLocalBundle("input16.gif")),

    /**
     * Output icon.
     */
    OUTPUT_16(fromLocalBundle("output16.gif")),

    /**
     * Delete icon (trash).
     */
    DELETE_16(fromLocalBundle("delete16.gif")),

    /**
     * Export icon.
     */
    EXPORT_16(fromLocalBundle("export16.gif")),

    /**
     * Open in editor icon.
     */
    OPEN_READ_ONLY_16(fromLocalBundle("open16.gif")),

    /**
     * Refresh icon.
     */
    REFRESH_16(fromLocalBundle("refresh16.gif")),

    /**
     * Refresh icon.
     */
    HELP_16(fromLocalBundle("help16.gif")),

    /**
     * Refresh icon.
     */
    PROPERTIES_16(fromLocalBundle("properties16.gif")),

    /**
     * Clear Console.
     */
    CLEARCONSOLE_16(fromLocalBundle("clear_co.gif")),

    /**
     * Copy.
     */
    COPY_16(fromLocalBundle("copy.gif")),

    /**
     * Paste.
     */
    PASTE_16(fromLocalBundle("paste.gif")),

    /**
     * New tool integration.
     */
    INTEGRATION_NEW(fromLocalBundle("integration_new.png")),

    /**
     * Edit integrated tool.
     */
    INTEGRATION_EDIT(fromLocalBundle("integration_edit.png")),

    /**
     * Remove integrated tool.
     */
    INTEGRATION_REMOVE(fromLocalBundle("integration_remove.png")),

    /**
     * Intermediate input folder icon in workflow data browser.
     */
    INTERMEDIATE_INPUT_16(fromLocalBundle("intermediate_input.gif")),

    /**
     * Tool input/output folder icon in workflow data browser.
     */
    TOOL_INPUT_OUTPUT_16(fromLocalBundle("tool_input_output.gif")),

    /**
     * Common text node icon in workflow data browser.
     */
    COMMON_TEXT_16(fromLocalBundle("common_text_node.gif")),

    /**
     * Common text nodes icon in workflow data browser.
     */
    COMMON_TEXT_NODES_16(fromLocalBundle("XPathChooser/common_text_nodes.gif")),

    /**
     * Icon for TIGLViewer.
     */
    TIGL_ICON(fromLocalBundle("TIGLViewer.png")),

    /**
     * Question mark node icon in workflow data browser.
     */
    QUESTION_MARK_16(fromLocalBundle("question_mark.gif")),

    /**
     * Snap to geometry icon in toolbar.
     */
    SNAP_TO_GEOMETRY(fromLocalBundle("snapToGeometry.png")),

    /**
     * Snap to grid icon in toolbar.
     */
    SNAP_TO_GRID(fromLocalBundle("snapToGrid.png")),

    /**
     * Show number of channels per connection icon in toolbar.
     */
    SHOW_CONNECTION_NUMBERS(fromLocalBundle("connectNumbers.gif")),

    /**
     * Workflow state: finished.
     */
    FINISHED(fromLocalBundle("finished.gif")),

    /**
     * Workflow state: cancelled.
     */
    CANCELLED(fromLocalBundle("cancel_enabled.gif")),

    /**
     * Workflow state: failed.
     */
    FAILED(fromLocalBundle("failed.gif")),
    
    /**
     * Workflow state: verification failed.
     */
    RESULTS_REJECTED(fromLocalBundle("results_rejected.png")),

    /**
     * Workflow state: corrupted.
     */
    CORRUPTED(fromLocalBundle("corrupted.gif")),

    /**
     * Undo icon.
     */
    UNDO(fromEclipseShared(ISharedImages.IMG_TOOL_UNDO)),

    /**
     * Save icon.
     */
    SAVE(fromEclipseShared(ISharedImages.IMG_ETOOL_SAVE_EDIT)),

    /**
     * OK icon.
     */
    TICK(fromLocalBundle("ok.png")),

    /**
     * VAMPZERO C icon.
     */
    VAMPZERO_C(fromLocalBundle("VampZero/c16.png")),

    /**
     * VAMPZERO D icon.
     */
    VAMPZERO_D(fromLocalBundle("VampZero/d16.png")),

    /**
     * VAMPZERO P icon.
     */
    VAMPZERO_P(fromLocalBundle("VampZero/p16.png")),

    /**
     * VAMPZERO ROOT icon.
     */
    VAMPZERO_ROOT(fromLocalBundle("VampZero/root16.png")),

    /**
     * Excel icon (small).
     */
    EXCEL_SMALL(fromLocalBundle("Excel/excel16.png")),

    /**
     * Excel icon (large).
     */
    EXCEL_LARGE(fromLocalBundle("Excel/excel64.png")),

    /**
     * Scroll lock disabled icon.
     */
    SCROLLOCK_DISABLED(fromLocalBundle("scrollLock_disabled.gif")),

    /**
     * Scroll lock enabled icon.
     */
    SCROLLOCK_ENABLED(fromLocalBundle("scrollLock_enabled.gif")),

    /**
     * Local component icon.
     */
    LOCAL(fromLocalBundle("local.gif")),

    /**
     * Local component icon.
     */
    IMITATION_MODE(fromLocalBundle("imitation_mode.gif")),
    
    /**
     * Deprecated component icon.
     */
    DEPRECATED(fromLocalBundle("deprecated.png")),

    /**
     * f(x) icon.
     */
    FUNCTION(fromLocalBundle("function12.png")),

    /**
     * Attribute icon.
     */
    ATTRIBUTE(fromLocalBundle("XPathChooser/attribute.gif")),

    /**
     * Attributes icon.
     */
    ATTRIBUTES(fromLocalBundle("XPathChooser/attributes.gif")),

    /**
     * Attribute icon.
     */
    ELEMENT(fromLocalBundle("XPathChooser/element.gif")),

    /**
     * Attribute icon.
     */
    ELEMENTS(fromLocalBundle("XPathChooser/elements.gif")),

    /**
     * Attribute icon.
     */
    TREE_SMALL(fromLocalBundle("XPathChooser/tree16.png")),

    /**
     * Attribute icon.
     */
    TREE_LARGE(fromLocalBundle("XPathChooser/tree64.png")),

    /**
     * Attribute icon.
     */
    FILE_SMALL(fromLocalBundle("XPathChooser/value12.png")),
    
    /**
     * Remove icon (red cross).
     */
    REMOVE_16(fromLocalBundle("remove.gif")),
    
    /**
     * Restore default icon (arrow and baseline).
     */
    RESTORE_DEFAULT(fromLocalBundle("restore_default.gif")),
    
    /**
     * Icon for SQL statements (e.g. in a folder).
     */
    SQL_STATEMENTS(fromLocalBundle("SQL/sqlStatements_16.gif")),
    
    /**
     * Default 16px icon for integrated tools.
     */
    INTEGRATED_TOOL_DEFAULT_16(fromLocalBundle("tool16.png")),

    /**
     * Default 32px icon for integrated tools.
     */
    INTEGRATED_TOOL_DEFAULT_32(fromLocalBundle("tool32.png"));

    private static final String IMAGE_PATH_PREFIX = "/resources/images/";

    private final ImageDescriptor imageDescriptor;

    StandardImages(ImageDescriptor imageDescriptor) {
        this.imageDescriptor = imageDescriptor;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return imageDescriptor;
    }

    private static ImageDescriptor fromLocalBundle(String filename) {
        // Note: the enum itself cannot be used for the class parameter, but any class in the bundle works - misc_ro
        return ImageUtils.createImageDescriptorFromBundleResource(ImageManager.class, IMAGE_PATH_PREFIX + filename);
    }

    private static ImageDescriptor fromEclipseShared(String eclipseId) {
        return ImageUtils.getEclipseImageDescriptor(eclipseId);
    }
}
