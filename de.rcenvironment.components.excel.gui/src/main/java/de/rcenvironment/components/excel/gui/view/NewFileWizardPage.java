/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;



import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;


/**
 * Wizard page to create a new script.
 *
 * @author Markus Kunde
 */
public class NewFileWizardPage extends WizardNewFileCreationPage {
    
    /**
     * The logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(NewFileWizardPage.class);
    
    /**
     * What to put in the file when just created.
     */
    protected final byte[] initialContent; 
    
    /**
     * The constructor.
     * @param selection The ?
     * @param name The name of the file type (e.g. "Script" or "Python")
     * @param extension The extension to create
     * @param initialFileContent The initial file contents (containing a run method)
     */
    public NewFileWizardPage(final IStructuredSelection selection, final String name,
            final String extension, final byte[] initialFileContent) {
        super(Messages.newFileNewLabel
              + name 
              + Messages.newFilePageName,
              selection);
        setTitle(name + " " + Messages.newFileFileLabel);
        setDescription(Messages.newFileDescriptionPart1
                       + " " 
                       + name 
                       + " " 
                       + Messages.newFileDescriptionPart2);
        setFileExtension(extension);
        setAllowExistingResources(true); // allow overwrite of files
        this.initialContent = initialFileContent;
    }
    
    /**
     * Gets the initial file contents (null in superclass).
     * @return The input stream of the initial contents object
     */
    @Override
    protected InputStream getInitialContents() {
        return new ByteArrayInputStream(initialContent);
    }
    
}
