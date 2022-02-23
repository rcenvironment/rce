/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;

/**
 * Updater interface for viewer: {@link TableBehaviour}. {@link TreeBehaviour}
 * 
 * @author Goekhan Guerkan
 */
public interface Updatable {
    

    /**
     * Constant.
     */
    String Error = "Error";

    /**
     * Constant.
     */
    String KEY_CHECK = "CHECK_BOX";

    /**
     * Constant.
     */
    String LOST = "Instance not available anymore";

    /**
     * Constant.
     */
    String EDITOR = "EDITOR";

    /**
     * Updates the instance Column.
     * 
     * @param element Cell to update.
     */
    void updateInstanceColumn(ViewerCell element);

    /**
     * Sets the color for the current row.
     * 
     * @param color The color to set for the row
     */
    void setComboColor(Color color);

    /**
     * Updates the checkbox Column.
     * 
     * @param element Cell to update.
     */
    void updateCheckBoxColumn(ViewerCell element);

    /**
     * Refreshes the columns. Called after view changes between Tree and Table.
     */
    void refreshColumns();

    /**
     * Filters the Viewer. Checks if the element has to be shown in the viewer
     * 
     * @return true if should be visible in the view.
     * @param filterText The String to filter the view.
     * @param element The element that has to be checked.
     */
    boolean useFilter(String filterText, Object element);

    /**
     * Deletes disposable objects.
     */
    void disposeWidgets();

}
