/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;

import de.rcenvironment.core.notification.NotificationService;

/**
 * Responsible for announcing study values.
 * 
 * Note: This interface has duplicate:
 * de.rcenvironment.rce.components.parametricstudy.commons.StudyPublisher
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public interface OptimizerPublisher extends Serializable {

    /** Default number of data sets which are buffered by the {@link NotificationService}. */
    int BUFFER_SIZE = 10000;

    /**
     * @return the adequate {@link ResultSet}.
     */
    ResultSet getStudy();

    /**
     * @param bufferSize the number of {@link StudyDataset}s to store.
     */
    void setBufferSize(int bufferSize);

    /**
     * @param dataset adds a new {@link StudyDataset}, i.e. announce and store.
     */
    void add(OptimizerResultSet dataset);

    /**
     * Clears the {@link ResultSet}. Must be called if {@link ResultSet} is not needed anymore to
     * free resources.
     */
    void clearStudy();
}
