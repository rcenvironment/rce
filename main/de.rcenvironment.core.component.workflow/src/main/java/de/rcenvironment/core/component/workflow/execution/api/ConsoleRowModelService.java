/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Provides central access to the information provided by received {@link ConsoleRow} instances. This includes the rows themselves, as well
 * as derived workflow and component information.
 * 
 * @author Robert Mischke
 */
public interface ConsoleRowModelService {

    /**
     * The sequence id to use for querying if no previous sequence id is available.
     */
    int INITIAL_SEQUENCE_ID = 0;

    /**
     * Returns a new {@link ConsoleModelSnapshot} of the current model state if the model was modified since the given sequence id. The
     * typical source of this sequence id is calling getSequenceId() on a previously returned snapshot. If no change has occured, this
     * method returns null.
     * 
     * @param sequenceId the last sequence id known to the caller
     * @return a new model snapshot, or null if no change has occured since the given sequence id
     */
    ConsoleModelSnapshot getSnapshotIfModifiedSince(int sequenceId);

    /**
     * Set the new {@link ConsoleRowFilter} for building future snapshots. Null is not permitted; set a permissive filter instead.
     * 
     * @param newFilter the new {@link ConsoleRowFilter}
     */
    void setRowFilter(ConsoleRowFilter newFilter);

    /**
     * FIXME temporary bridge method; move to inside.
     */
    void updateSubscriptions();

    /**
     * FIXME this should not be allowed for a central service; rework.
     */
    void clearAll();

    /**
     * Ensures that the console model is registered to listen for console output.
     * 
     * @throws InterruptedException on thread interruption
     */
    void ensureConsoleCaptureIsInitialized() throws InterruptedException;
}
