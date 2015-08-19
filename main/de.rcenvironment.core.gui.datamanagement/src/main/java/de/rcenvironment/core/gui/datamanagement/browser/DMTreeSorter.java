/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.rcenvironment.core.component.datamanagement.history.HistoryMetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.utils.common.ComparatorUtils;

/**
 * Class for sorting the DMItems. 
 * 
 * @author Sascha Zur
 */
public class DMTreeSorter extends ViewerSorter{

    /** Constant. */
    public static final int SORT_BY_TIMESTAMP = 0;
    /** Constant. */
    public static final int SORT_BY_NAME_ASC = 1;
    /** Constant. */
    public static final int SORT_BY_NAME_DESC = 2;
    /** Constant. */
    public static final int SORT_BY_TIMESTAMP_DESC = 3;
    
    private static final MetaData METADATA_HISTORY_TIMESTAMP = new MetaData(
        HistoryMetaDataKeys.HISTORY_TIMESTAMP, true, true);

    private static boolean stopSorting;
    
    private int sortType;

    public DMTreeSorter(int sortType) {
        this.sortType = sortType;
        // sorting enable
        stopSorting = false;
    }
    @Override
    public int category(Object element) {
        return 0;
    }
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        int result = 0;
        DMBrowserNode o1 = (DMBrowserNode) e1;
        DMBrowserNode o2 = (DMBrowserNode) e2;
        boolean sortable = isSortable(o1) && isSortable(o2);
        sortable &= !stopSorting;
        if (sortType == SORT_BY_NAME_ASC && sortable) {
            result = o1.getTitle().compareToIgnoreCase(o2.getTitle());
        } else if (sortType == SORT_BY_NAME_DESC && sortable) {
            result = -o1.getTitle().compareToIgnoreCase(o2.getTitle());
        } else {
            if (o1.getMetaData() != null && o2.getMetaData() != null && sortable) {
                String val1 = o1.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                String val2 = o2.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                long time1 = nullSafeLongValue(val1);
                long time2 = nullSafeLongValue(val2);
                result = ComparatorUtils.compareLong(time1, time2);
                if (sortType == SORT_BY_TIMESTAMP_DESC) {
                    result *= (0 - 1);
                }
            } else {
                result = 0;
            }
        }
        return result;
    }

    private long nullSafeLongValue(String val1) {
        if (val1 == null) {
            return 0L;
        }
        return Long.parseLong(val1);
    }

    /**
     * 
     * Sorting depends on the Browser Node Type.
     * 
     * @param node
     * @return sortable state of the node
     */
    private boolean isSortable(DMBrowserNode node) {
        boolean sortable = false;
        final DMBrowserNodeType nodeType = node.getType();
        if (nodeType.equals(DMBrowserNodeType.Workflow)) {
            sortable = true;
        } else if (nodeType.equals(DMBrowserNodeType.Component)) {
            if (sortType == SORT_BY_NAME_ASC || sortType == SORT_BY_NAME_DESC) {
                sortable = true;
            }
        } else if (nodeType.equals(DMBrowserNodeType.HistoryObject)) {
            if (sortType == SORT_BY_TIMESTAMP || sortType == SORT_BY_TIMESTAMP_DESC) {
                sortable = true;
            }
        }
        return sortable;
    }

    /**
     * Stops the sorting of the Tree Sorter.
     */
    public static void stopSorting() {
        stopSorting = true;
    }

}
