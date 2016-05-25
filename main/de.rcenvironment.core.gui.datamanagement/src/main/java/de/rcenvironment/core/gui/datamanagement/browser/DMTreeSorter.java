/*
 * Copyright (C) 2006-2016 DLR, Germany
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

    private static boolean enableSorting;
    
    private int sortingType;

    public DMTreeSorter(int sortType) {
        this.sortingType = sortType;
        enableSorting = true;
    }
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        DMBrowserNode o1 = (DMBrowserNode) e1;
        DMBrowserNode o2 = (DMBrowserNode) e2;
        if (!enableSorting || !isSortable(o1, sortingType) || !isSortable(o2, sortingType)) {
            return 0;
        }
        switch (sortingType) {
        case SORT_BY_NAME_ASC:
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        case SORT_BY_NAME_DESC:
            return o2.getTitle().compareToIgnoreCase(o1.getTitle());
        case SORT_BY_TIMESTAMP:
        case SORT_BY_TIMESTAMP_DESC:
            if (o1.getMetaData() != null && o2.getMetaData() != null) {
                String val1 = o1.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                String val2 = o2.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                long time1 = nullSafeLongValue(val1);
                long time2 = nullSafeLongValue(val2);
                if (sortingType == SORT_BY_TIMESTAMP_DESC) {
                    return ComparatorUtils.compareLong(time2, time1);
                }
                return ComparatorUtils.compareLong(time1, time2);
            }
        default:
            return 0;
        }

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
     * @param node the node to check
     * @param type the sorting type
     * @return sortable state of the node
     */
    public boolean isSortable(DMBrowserNode node, int type) {
        boolean sortable = false;
        final DMBrowserNodeType nodeType = node.getType();
        if (nodeType.equals(DMBrowserNodeType.Workflow)) {
            sortable = true;
        } else if (nodeType.equals(DMBrowserNodeType.Timeline) || nodeType.equals(DMBrowserNodeType.Component)) {
            if (type == SORT_BY_TIMESTAMP || type == SORT_BY_TIMESTAMP_DESC) {
                sortable = true;
            }
        } else if (nodeType.equals(DMBrowserNodeType.Components)) {
            if (type == SORT_BY_NAME_ASC || type == SORT_BY_NAME_DESC) {
                sortable = true;
            }
        } else if (nodeType.equals(DMBrowserNodeType.HistoryObject)) {
            if (type == SORT_BY_TIMESTAMP || type == SORT_BY_TIMESTAMP_DESC) {
                sortable = true;
            }
        } else if (nodeType.equals(DMBrowserNodeType.ComponentHostInformation)) {
            if (type == SORT_BY_NAME_ASC || type == SORT_BY_NAME_DESC) {
                sortable = true;
            }
        }
        return sortable;
    }


    /**
     * Enables or disables sorting of the Tree Sorter.
     * 
     * @param enable true if sorting should be enabled.
     */
    public void enableSorting(boolean enable) {
        enableSorting = enable;
    }

    public void setSortingType(int sortingType) {
        this.sortingType = sortingType;
    }

    public int getSortingType() {
        return sortingType;
    }

}
