/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.history;

/**
 * Special {@link MetaData} keys for history data management.
 * 
 * @author Robert Mischke
 */
public abstract class HistoryMetaDataKeys {

    /**
     * The (mandatory) fully qualified class name of the {@link Serializable} history object. Used
     * to determine the appropriate handler without deserializing the object first.
     * 
     * Implementation notice: This key is also used as the main marker to detect history objects in
     * the first place.
     */
    public static final String HISTORY_HISTORY_DATA_ITEM_IDENTIFIER = "rce.history.classname";

    /**
     * Optional (but recommended) end-user information text for a history object; used as title in
     * the default browser tree view.
     */
    public static final String HISTORY_USER_INFO_TEXT = "rce.history.infotext";

    /**
     * A {@link Long} value (converted to its String representation) that defines the time
     * associated with this history entry. For interpretation of its value, see
     * {@link System#currentTimeMillis()}.
     */
    // TODO adapt internal key id? will break existing test data
    public static final String HISTORY_TIMESTAMP = "rce.history.orderingindex";
}
