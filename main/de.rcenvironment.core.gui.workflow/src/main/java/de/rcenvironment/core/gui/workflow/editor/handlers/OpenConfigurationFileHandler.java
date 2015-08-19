/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;


/**
 * Handler that opens the profile configuration file in an editor.
 * 
 * @author Oliver Seebach
 *
 */
public class OpenConfigurationFileHandler extends AbstractHandler {

    private static final Log LOGGER = LogFactory.getLog(OpenConfigurationFileHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        File configurationFile;
        configurationFile = ServiceRegistry.createAccessFor(this).getService(ConfigurationService.class).getProfileConfigurationFile();
        
        try {
            EditorsHelper.openExternalFileInEditor(configurationFile);
        } catch (PartInitException e) {
            LOGGER.error("Failed to open profile configuration file in an editor.", e);
        }
        return null;
    }

}
