/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.view.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;


/**
 * Provides concrete label texts to display and images if required.
 * 
 * @author Doreen Seider
 */
public class ClusterJobInformationLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    private Image runningImage;
    
    private Image queuedImage;
    
    private Image completedImage;
    
    private Image othersImage;
    
    public ClusterJobInformationLabelProvider() {
        super();
        runningImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/running.gif")).createImage(); //$NON-NLS-1$
        queuedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/queued.gif")).createImage(); //$NON-NLS-1$
        completedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/completed.gif")).createImage(); //$NON-NLS-1$
        othersImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/others.gif")).createImage(); //$NON-NLS-1$
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        Image image = null;

        if (element instanceof ClusterJobInformation) {
            ClusterJobInformation logEntry = (ClusterJobInformation) element;

            if (0 == columnIndex) {
                switch (logEntry.getJobState()) {
                case Running:
                    image = runningImage;
                    break;
                case Queued:
                case Waiting:
                case DepWait:
                case Unsched:
                    image = queuedImage;
                    break;
                case Completed:
                    image = completedImage;
                    break;
                default:
                    image = othersImage;
                    break;
                }
            }
        }

        return image;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        String returnValue = ""; //$NON-NLS-1$

        if (element instanceof ClusterJobInformation) {
            ClusterJobInformation logEntry = (ClusterJobInformation) element;

            switch (columnIndex) {
            case 0:
                returnValue = logEntry.getJobId();
                break;
            case 1:
                returnValue = logEntry.getJobName();
                break;
            case 2:
                returnValue = logEntry.getUser();
                break;
            case 3:
                returnValue = logEntry.getQueue();
                break;
            case 4:
                returnValue = logEntry.getRemainingTime();
                break;
            case 5:
                returnValue = logEntry.getStartTime();
                break;
            case 6:
                returnValue = logEntry.getQueueTime();
                break;
            case 7:
                returnValue = logEntry.getJobState().toString();
                break;
            default:
                break;
            }
        }

        return returnValue;
    }
    
    @Override
    public void dispose() {
        runningImage.dispose();
        queuedImage.dispose();
        completedImage.dispose();
        othersImage.dispose();
    }
}
