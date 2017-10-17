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

import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeUtils;

/**
 * Class for sorting the DMItems.
 * 
 * @author Sascha Zur
 * @author Jan Flink
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
            return DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE.compare(o1, o2);
        case SORT_BY_NAME_DESC:
            return DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE_DESC.compare(o1, o2);
        case SORT_BY_TIMESTAMP:
            return DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP.compare(o1, o2);
        case SORT_BY_TIMESTAMP_DESC:
            return DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP_DESC.compare(o1, o2);
        default:
            return 0;
        }

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
        } else if (nodeType.equals(DMBrowserNodeType.HistoryObject) && node.getParent().getType().equals(DMBrowserNodeType.Components)) {
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
