/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

/**
 * Listens for Eclipse log messages and re-logs them with the logging infrastructure used in RCE. It ensures, that all messages even from
 * the underlying Eclipse are written to rce.log and warnings.log. (Especially, exceptions from the SWT event thread are missed without this
 * kind of forwarding.)
 * 
 * @author Doreen Seider
 */
public class EclipseLogListener implements ILogListener {

    private static final Log LOGGER = LogFactory.getLog(EclipseLogListener.class.getName());

    @Override
    public void logging(IStatus status, String arg1) {
        if (status.getSeverity() == IStatus.ERROR) {
            if (status.getException() == null) {
                LOGGER.error(status.getMessage());
            } else {
                LOGGER.error(status.getMessage(), status.getException());
            }
        } else if (status.getSeverity() == IStatus.WARNING) {
            if (status.getException() == null) {
                LOGGER.warn(status.getMessage());
            } else {
                LOGGER.warn(status.getMessage(), status.getException());
            }
        } else if (status.getSeverity() == IStatus.INFO) {
            if (status.getException() == null) {
                LOGGER.info(status.getMessage());
            } else {
                LOGGER.info(status.getMessage(), status.getException());
            }
        }
    }

}
