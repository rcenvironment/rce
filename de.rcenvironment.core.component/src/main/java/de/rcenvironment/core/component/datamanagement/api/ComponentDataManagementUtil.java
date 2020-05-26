/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.api;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.impl.ComponentContextImpl;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * Common utilities for component-level data management.
 * 
 * @author Robert Mischke
 */
public abstract class ComponentDataManagementUtil {

    private static Log log = LogFactory.getLog(ComponentDataManagementUtil.class);

    /**
     * Inserts the appropriate metadata values for the given {@link ComponentContext} into the given {@link MetaDataSet}.
     * <ul>
     * <li>COMPONENT_RUN_ID</li>
     * </ul>
     * 
     * @param mds the metadata set to modify
     * @param componentContext the source for the metadata values
     */
    public static void setComponentMetaData(MetaDataSet mds, ComponentContext componentContext) {

        MetaData mdComponentRunId = new MetaData(MetaDataKeys.COMPONENT_RUN_ID, true, true);
        if (((ComponentContextImpl) componentContext).getComponentExecutionDataManagementId() == null) {
            throw new IllegalArgumentException("Given data managegment identifier for the associated component run must not be null; "
                + "note: writing files to the data management is only allowed within 'start()' if 'treatStartAsComponentRun()' returns "
                + "true and within 'processInputs()' and not allowed at all if component was cancelled");
        }
        // transfer component run dm id
        mds.setValue(mdComponentRunId, String.valueOf(((ComponentContextImpl) componentContext).getComponentExecutionDataManagementId()));
    }

    /**
     * Sets the FILENAME metadata field.
     * 
     * @param mds the {@link MetaDataSet} to modify
     * @param filename the filename to set
     */
    public static void setAssociatedFilename(MetaDataSet mds, String filename) {
        MetaData mdFilename = new MetaData(MetaDataKeys.FILENAME, true, true);
        mds.setValue(mdFilename, filename);
    }

    /**
     * Logs debug information about a {@link MetaDataSet}.
     * 
     * TODO merge into {@link MetaDataSet}?
     * 
     * @param mds the object to log information about
     */
    public static void printDebugInfo(MetaDataSet mds) {
        Iterator<MetaData> iter = mds.iterator();
        while (iter.hasNext()) {
            MetaData md = iter.next();
            // TODO using sysout here due to current problems with log capture
            log.debug(md.getKey() + "/" + md.isReadOnly() + " -> " + mds.getValue(md));
        }
    }

}
