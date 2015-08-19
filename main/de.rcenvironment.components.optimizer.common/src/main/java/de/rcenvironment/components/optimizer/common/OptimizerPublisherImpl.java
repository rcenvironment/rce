/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import de.rcenvironment.core.notification.NotificationService;

/**
 * Implementation of {@link StudyPublisher}.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public final class OptimizerPublisherImpl implements OptimizerPublisher {

    private static final long serialVersionUID = 6027553291193203997L;

    private NotificationService notificationService;
    
    private final ResultSet resultSet;

    private final String notificationId;
    

    public OptimizerPublisherImpl(final ResultSet resultSet, NotificationService notificationService) {
        this.resultSet = resultSet;
        this.notificationService = notificationService;
        notificationId = OptimizerUtils.createDataIdentifier(resultSet);
        setBufferSize(BUFFER_SIZE);
    }
    
    @Override
    public ResultSet getStudy() {
        return resultSet;
    }
    
    @Override
    public void setBufferSize(final int bufferSize) {
        notificationService.setBufferSize(notificationId, bufferSize);
    }

    @Override
    public void add(final OptimizerResultSet dataset) {
        notificationService.send(notificationId, dataset);
    }

    @Override
    public void clearStudy() {
        notificationService.removePublisher(notificationId);
    }
    

}

