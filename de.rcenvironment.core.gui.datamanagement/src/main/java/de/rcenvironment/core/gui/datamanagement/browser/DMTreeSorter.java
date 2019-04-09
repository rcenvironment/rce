/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
        final DMBrowserNodeType nodeType = node.getType();
        if (node.isLeafNode()) {
            return false;
        }
        switch (type) {
        case SORT_BY_TIMESTAMP:
        case SORT_BY_TIMESTAMP_DESC:
            switch (nodeType) {
            case HistoryObject:
                return node.getParent().getType().equals(DMBrowserNodeType.Components);
            case Component:
            case Workflow:
            case Timeline:
                return true;
            default:
                break;
            }
            return false;
        case SORT_BY_NAME_ASC:
        case SORT_BY_NAME_DESC:
            switch (nodeType) {
            case Component:
            case Workflow:
            case Components:
            case Timeline:
            case ComponentHostInformation:
            case Input:
            case Output:
                return true;
            default:
                break;
            }
            return false;
        default:
            return false;
        }

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
