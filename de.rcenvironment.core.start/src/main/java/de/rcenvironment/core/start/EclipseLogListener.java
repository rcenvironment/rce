/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Listens for Eclipse log messages and re-logs them with the logging infrastructure used in RCE. It ensures, that all messages even from
 * the underlying Eclipse are written to rce.log and warnings.log. (Especially, exceptions from the SWT event thread are missed without this
 * kind of forwarding.)
 * 
 * @author Doreen Seider
 */
public class EclipseLogListener implements ILogListener {

    private static final Log LOGGER = LogFactory.getLog(EclipseLogListener.class.getName());

    private static final String MESSAGE_SUFFIX = " [forwarded via ILogListener]";

    @Override
    public void logging(IStatus status, String arg1) {
        String message = status.getMessage();
        if (StringUtils.isNullorEmpty(message)) {
            message = "(empty message)";
        }
        String forwardedMessage = message + MESSAGE_SUFFIX;

        if (status.getSeverity() == IStatus.ERROR) {
            if (status.getException() == null) {
                LOGGER.error(forwardedMessage);
            } else {
                LOGGER.error(forwardedMessage, status.getException());
            }
        } else if (status.getSeverity() == IStatus.WARNING) {
            if (status.getException() == null) {
                LOGGER.warn(forwardedMessage);
            } else {
                LOGGER.warn(forwardedMessage, status.getException());
            }
        } else if (status.getSeverity() == IStatus.INFO) {
            if (status.getException() == null) {
                LOGGER.info(forwardedMessage);
            } else {
                LOGGER.info(forwardedMessage, status.getException());
            }
        }
    }

}
