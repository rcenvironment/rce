/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.PartInitException;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.legacy.FileEncodingUtils;

/**
 * Opens mapping file in editor and allows to edit it.
 *
 * @author Brigitte Boden
 */
public class EditMappingFileRunnable implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(EditMappingFileRunnable.class);

    private final WorkflowNode workflowNode;

    public EditMappingFileRunnable(WorkflowNode node) {
        this.workflowNode = node;
    }

    @Override
    public void run() {
        try {
            final File tempFile =
                TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("mapping.xsl");
            String content =
                workflowNode.getConfigurationDescription().getConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);

            FileEncodingUtils.saveUnicodeStringToFile(content, tempFile);

            EditorsHelper.openExternalFileInEditor(tempFile, new Runnable[] {
                new Runnable() {

                    @Override
                    public void run() {
                        try {
                            final String newValue = FileEncodingUtils.loadUnicodeStringFromFile(tempFile);
                            workflowNode.getConfigurationDescription().setConfigurationValue(
                                XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, newValue);

                            if (workflowNode.getConfigurationDescription().getConfigurationValue(
                                XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME) == null
                                || (workflowNode.getConfigurationDescription().getConfigurationValue(
                                    XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME)
                                instanceof String
                                && workflowNode.getConfigurationDescription()
                                    .getConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME)
                                    .isEmpty())) {
                                // Just guessing it is XSLT
                                workflowNode.getConfigurationDescription().setConfigurationValue(
                                    XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME,
                                    XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
                            }
                        } catch (final IOException e) {
                            LOGGER.error("Could not read temporary edited file", e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.error("Could not create temporary file", e);
        } catch (PartInitException e) {
            LOGGER.error(e);
        }
    }
}
