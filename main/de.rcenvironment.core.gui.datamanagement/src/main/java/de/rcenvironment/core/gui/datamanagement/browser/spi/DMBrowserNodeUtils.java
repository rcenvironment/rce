/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.util.Comparator;
import java.util.Map;

import de.rcenvironment.core.component.datamanagement.history.HistoryMetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.utils.common.ComparatorUtils;

/**
 * Provides common methods for manipulating {@link DMBrowserNode}s.
 * 
 * @author Robert Mischke
 * 
 */
public abstract class DMBrowserNodeUtils {
    
    /**
     * A {@link Comparator} that sorts by ascending history timestamp.
     */
    public static final Comparator<DMBrowserNode> COMPARATOR_BY_HISTORY_TIMESTAMP = new Comparator<DMBrowserNode>() {

        @Override
        public int compare(DMBrowserNode o1, DMBrowserNode o2) {
            if (o1.getMetaData() != null && o2.getMetaData() != null) {
                String val1 = o1.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                String val2 = o2.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                long time1 = nullSafeLongValue(val1);
                long time2 = nullSafeLongValue(val2);
                return ComparatorUtils.compareLong(time1, time2);
            }
            return 0;
        }

        private long nullSafeLongValue(String val1) {
            if (val1 == null) {
                return 0L;
            }
            return Long.parseLong(val1);
        }
    };

    /**
     * A {@link Comparator} that sorts by descending history timestamp.
     */
    public static final Comparator<DMBrowserNode> COMPARATOR_BY_HISTORY_TIMESTAMP_DESC = new Comparator<DMBrowserNode>() {

        @Override
        public int compare(DMBrowserNode o1, DMBrowserNode o2) {
            if (o1.getMetaData() != null && o2.getMetaData() != null) {
                String val1 = o1.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                String val2 = o2.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
                long time1 = nullSafeLongValue(val1);
                long time2 = nullSafeLongValue(val2);
                return ComparatorUtils.compareLong(time1, time2) * DESC;
            }
            return 0;
        }

        private long nullSafeLongValue(String val1) {
            if (val1 == null) {
                return 0L;
            }
            return Long.parseLong(val1);
        }
    };

    /**
     * A {@link Comparator} that sorts by associated filename.
     */
    public static final Comparator<DMBrowserNode> COMPARATOR_BY_FILENAME = new Comparator<DMBrowserNode>() {

        @Override
        public int compare(DMBrowserNode o1, DMBrowserNode o2) {
            String val1 = o1.getAssociatedFilename().toLowerCase();
            String val2 = o2.getAssociatedFilename().toLowerCase();
            return val1.compareTo(val2);
        }
    };

    /**
     * A {@link Comparator} that sorts by ascending node title.
     */
    public static final Comparator<DMBrowserNode> COMPARATOR_BY_NODE_TITLE = new Comparator<DMBrowserNode>() {

        @Override
        public int compare(DMBrowserNode o1, DMBrowserNode o2) {
            String val1 = o1.getTitle().toLowerCase();
            String val2 = o2.getTitle().toLowerCase();
            return val1.compareTo(val2);
        }
    };

    /**
     * A {@link Comparator} that sorts by descending node title.
     */
    public static final Comparator<DMBrowserNode> COMPARATOR_BY_NODE_TITLE_DESC = new Comparator<DMBrowserNode>() {

        @Override
        public int compare(DMBrowserNode o1, DMBrowserNode o2) {
            String val1 = o1.getTitle().toLowerCase();
            String val2 = o2.getTitle().toLowerCase();
            return val1.compareTo(val2) * DESC;
        }
    };

    private static final int DESC = -1;

    private static final MetaData METADATA_HISTORY_TIMESTAMP = new MetaData(
        HistoryMetaDataKeys.HISTORY_TIMESTAMP, true, true);
    
    private DMBrowserNodeUtils() {}
    
    /**
     * Creates a folder node containing file reference nodes for the files referenced by the
     * provided map. The file nodes are sorted by their associated filename.
     * 
     * @param fileMap a map containing entries with the associated filename as the key and the data
     *        reference id as value
     * @param folder the parent node to add the created nodes to
     */
    public static void createDMFileResourceNodesFromMap(Map<String, String> fileMap, DMBrowserNode folder) {
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            DMBrowserNode fileNode = DMBrowserNode.addNewLeafNode(entry.getKey(),
                DMBrowserNodeType.DMFileResource, folder);
            fileNode.setAssociatedFilename(entry.getKey());
            fileNode.setDataReferenceId(entry.getValue());
        }
        folder.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_FILENAME);
    }
    
}
