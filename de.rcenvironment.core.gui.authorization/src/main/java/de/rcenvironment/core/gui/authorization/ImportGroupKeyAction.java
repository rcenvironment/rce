/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Action class to import an authorization group key.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 */
public final class ImportGroupKeyAction extends Action {

    private static final String APOSTROPHE = "'";

    private static final String DOT = ".";

    private AuthorizationService authorizationService;

    private Log log = LogFactory.getLog(getClass());

    public ImportGroupKeyAction(AuthorizationService authorizationService) {
        super("Import Group Key...");
        this.authorizationService = authorizationService;
    }

    @Override
    public void run() {
        final Display display = Display.getDefault();
        Shell shell = display.getActiveShell();
        ImportGroupKeyDialog importGroupKeyDialog = new ImportGroupKeyDialog(shell);
        int id = importGroupKeyDialog.open();
        if (id == 0) {
            String importKey = importGroupKeyDialog.getKeyToImport().trim();
            try {
                authorizationService.importFromString(importKey);
            } catch (OperationFailureException e) {
                log.error("Failed to import access group for key '" + importKey + APOSTROPHE + DOT);
            }
        }
    }
}
