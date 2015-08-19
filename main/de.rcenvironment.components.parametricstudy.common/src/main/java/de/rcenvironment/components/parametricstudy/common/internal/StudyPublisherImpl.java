/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common.internal;

import de.rcenvironment.components.parametricstudy.common.Study;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.core.notification.NotificationService;


/**
 * Implementation of {@link StudyPublisher}.
 * 
 * @author Christian Weiss
 */
public final class StudyPublisherImpl implements StudyPublisher {

    private static final long serialVersionUID = 6027553291193203997L;

    private final Study study;

    private final String notificationId;
    
    private NotificationService notificationService;

    public StudyPublisherImpl(final Study study, NotificationService notificationService) {
        this.study = study;
        this.notificationService = notificationService;
        notificationId = ParametricStudyUtils.createDataIdentifier(study);
        setBufferSize(BUFFER_SIZE);
    }
    
    @Override
    public Study getStudy() {
        return study;
    }
    
    @Override
    public void setBufferSize(final int bufferSize) {
        notificationService.setBufferSize(notificationId, bufferSize);
    }

    @Override
    public void add(final StudyDataset dataset) {
        notificationService.send(notificationId, dataset);
    }

    @Override
    public void clearStudy() {
        notificationService.removePublisher(notificationId);
    }

}

