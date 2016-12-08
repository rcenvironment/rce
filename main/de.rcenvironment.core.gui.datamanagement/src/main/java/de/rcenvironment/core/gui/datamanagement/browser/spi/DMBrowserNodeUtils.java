/*
 * Copyright (C) 2006-2016 DLR, Germany
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

/**
 * Provides common methods for manipulating {@link DMBrowserNode}s.
 * 
 * @author Robert Mischke
 * @author Jan Flink Made compare methods null safe
 * 
 */
public abstract class DMBrowserNodeUtils {
    
    
    /**
     * A {@link Comparator} that sorts by ascending history timestamp.
     */
    public static final Comparator<DMBrowserNode> COMPARATOR_BY_HISTORY_TIMESTAMP = new Comparator<DMBrowserNode>() {

        @Override
        public int compare(DMBrowserNode o1, DMBrowserNode o2) {
            
            boolean o1NullCheck = o1 == null || o1.getMetaData() == null;
            boolean o2NullCheck = o2 == null || o2.getMetaData() == null;

            if (o1NullCheck && o2NullCheck) {
                return 0;
            }

            if (o1NullCheck) {
                return Integer.MAX_VALUE;
            }

            if (o2NullCheck) {
                return Integer.MIN_VALUE;
            }

            String val1 = o1.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
            String val2 = o2.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
            long time1 = nullSafeLongValue(val1);
            long time2 = nullSafeLongValue(val2);
            return Long.compare(time1, time2);
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
            
            boolean o1NullCheck = o1 == null || o1.getMetaData() == null;
            boolean o2NullCheck = o2 == null || o2.getMetaData() == null;

            if (o1NullCheck && o2NullCheck) {
                return 0;
            }

            if (o1NullCheck) {
                return Integer.MAX_VALUE;
            }

            if (o2NullCheck) {
                return Integer.MIN_VALUE;
            }

            String val1 = o1.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
            String val2 = o2.getMetaData().getValue(METADATA_HISTORY_TIMESTAMP);
            long time1 = nullSafeLongValue(val1);
            long time2 = nullSafeLongValue(val2);
            return Long.compare(time1, time2) * DESC;
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

            boolean o1NullCheck = o1 == null || o1.getAssociatedFilename() == null;
            boolean o2NullCheck = o2 == null || o2.getAssociatedFilename() == null;

            if (o1NullCheck && o2NullCheck) {
                return 0;
            }

            if (o1NullCheck) {
                return Integer.MIN_VALUE;
            }

            if (o2NullCheck) {
                return Integer.MAX_VALUE;
            }

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
            
            boolean o1NullCheck = o1 == null || o1.getTitle() == null;
            boolean o2NullCheck = o2 == null || o2.getTitle() == null;

            if (o1NullCheck && o2NullCheck) {
                return 0;
            }

            if (o1NullCheck) {
                return Integer.MIN_VALUE;
            }

            if (o2NullCheck) {
                return Integer.MAX_VALUE;
            }

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
            
            boolean o1NullCheck = o1 == null || o1.getTitle() == null;
            boolean o2NullCheck = o2 == null || o2.getTitle() == null;

            if (o1NullCheck && o2NullCheck) {
                return 0;
            }

            if (o1NullCheck) {
                return Integer.MIN_VALUE;
            }

            if (o2NullCheck) {
                return Integer.MAX_VALUE;
            }
            
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
