/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.script.gui;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.PartInitException;

import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.legacy.FileEncodingUtils;

/**
 * Opens file for scripting and load content of configuration if there is already some script stuff.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public abstract class AbstractEditScriptRunnable implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(AbstractEditScriptRunnable.class);
    
    @Override
    public void run() {
        try {
            final File tempFile = TempFileServiceAccess.getInstance()
                .createTempFileWithFixedFilename("script.py");
            // if script content already exist, load it to temp file
            if (getScript() != null) {
                FileEncodingUtils.saveUnicodeStringToFile(getScript(), tempFile);
            }

            EditorsHelper.openExternalFileInEditor(tempFile, new Runnable[] {
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // save new tempFile in component's configuration
                            setScript(FileEncodingUtils.loadUnicodeStringFromFile(tempFile));
                        } catch (final IOException e) {
                            LOGGER.error(e);
                        }
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.error(e);
        } catch (final PartInitException e) {
            LOGGER.error(e);
        }
    }
    
    protected abstract void setScript(String script);
    
    protected abstract String getScript();

}
