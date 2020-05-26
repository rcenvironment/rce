/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

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
 */
public abstract class AbstractEditScriptRunnable implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(AbstractEditScriptRunnable.class);

    private File tempFile;

    @Override
    public void run() {
        try {
            tempFile = TempFileServiceAccess.getInstance()
                .createTempFileWithFixedFilename(getScriptName());
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

    /**
     * @param script to update in the editor view
     */
    public void update(String script) {
        try {
            String current = FileEncodingUtils.loadUnicodeStringFromFile(tempFile);
            if (!current.equals(script)) {
                FileEncodingUtils.saveUnicodeStringToFile(script, tempFile);
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    protected abstract String getScriptName();

    protected abstract void setScript(String script);

    protected abstract String getScript();

}
