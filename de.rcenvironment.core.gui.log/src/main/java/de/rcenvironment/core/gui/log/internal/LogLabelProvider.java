/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.log.SerializableLogEntry;

/**
 * Provides the concrete label texts to display and images if required.
 * 
 * @author Arne Bachmann
 */
public class LogLabelProvider extends LabelProvider implements ITableLabelProvider {

    private Image infoImage;

    private Image warningImage;

    private Image errorImage;

    public LogLabelProvider() {
        super();

        infoImage = ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16);

        warningImage = ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16);

        errorImage = ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16);

    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        Image image = null;

        if (element instanceof SerializableLogEntry) {
            SerializableLogEntry logEntry = (SerializableLogEntry) element;

            if (0 == columnIndex) {
                // level column

                if (LogService.LOG_INFO == logEntry.getLevel()) {
                    // error level
                    image = infoImage;

                } else if (LogService.LOG_WARNING == logEntry.getLevel()) {
                    // warn level
                    image = warningImage;

                } else if (LogService.LOG_ERROR == logEntry.getLevel()) {
                    // error level
                    image = errorImage;
                }
            }
        }

        return image;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        String returnValue = ""; //$NON-NLS-1$
        if (element instanceof SerializableLogEntry) {
            SerializableLogEntry logEntry = (SerializableLogEntry) element;

            switch (columnIndex) {
            // level
            case 0:
                int level = logEntry.getLevel();
                switch (level) {
                case LogService.LOG_ERROR:
                    returnValue = "ERROR"; //$NON-NLS-1$
                    break;
                case LogService.LOG_INFO:
                    returnValue = "INFO"; //$NON-NLS-1$
                    break;
                case LogService.LOG_WARNING:
                    returnValue = "WARN"; //$NON-NLS-1$
                    break;
                default:
                    returnValue = "UNKNOWN"; //$NON-NLS-1$
                    break;
                }
                break;
            // message
            case 1:
                returnValue = logEntry.getMessage().replaceAll(SerializableLogEntry.RCE_SEPARATOR, " ");
                break;
            // bundle name
            case 2:
                returnValue = logEntry.getBundleName();
                break;
            // platform name
            case 3:
                returnValue = logEntry.getPlatformIdentifer().getAssociatedDisplayName();
                break;
            // date and time
            case 4:
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS"); //$NON-NLS-1$
                returnValue = df.format(logEntry.getTime());
                break;
            default:
                // shouldn't happen
                break;
            }
        }

        return returnValue;
    }

}
