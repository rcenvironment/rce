/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;


/**
 * A eclipse wizard to create a new script.

 * @author Markus Kunde
 */
public class NewFileWizard extends Wizard implements INewWizard {
    
    /**
     * File type name.
     */
    private final String name;
    
    /**
     * File extension to create with the wizard.
     */
    private final String extension;
    
    /**
     * Boiler-plate code for the file.
     */
    private final byte[] initialContent;

    /**
     * Initial selection (should be the current project).
     */
    private IStructuredSelection selection;
    
    /**
     * The only page available.
     */
    private NewFileWizardPage newFileWizardPage;
    
    /**
     * The selected file.
     */
    private IFile file = null; 

    
    /**
     * Constructor.
     * @param fileTypeName The file type name, e.g. "Excel"
     * @param fileExtension The file extension
     * @param initialFileContent The file's contents
     * @param imageData The image or null
     */
    public NewFileWizard(final String fileTypeName, final String fileExtension,
            final byte[] initialFileContent, final ImageData imageData) {
        setWindowTitle(Messages.newFileTitle);
        setHelpAvailable(false);
        setForcePreviousAndNextButtons(false);
        setNeedsProgressMonitor(false);
        name = fileTypeName;
        if (imageData != null) {
            setDefaultPageImageDescriptor(ImageDescriptor.createFromImageData(imageData));
        }
        this.extension = fileExtension;
        this.initialContent = initialFileContent;
    } 

    /**
     * Adds all wizard pages.
     */
    @Override
    public void addPages() {
        newFileWizardPage = new NewFileWizardPage(selection, name, extension, initialContent);
        addPage(newFileWizardPage);
    }
    
    /**
     * Called when the wizard's finish button is pressed.
     * @return True if OK
     */
    @Override
    public boolean performFinish() {
        file = newFileWizardPage.createNewFile();
        return file != null;
    }

    /**
     * Used to set some properties.
     * @param myWorkbench The workbench
     * @param theSelection selection
     */
    @Override
    public void init(final IWorkbench myWorkbench, final IStructuredSelection theSelection) {
        this.selection = theSelection;
    }
    
    /**
     * Getter for the created file.
     * @return The created file
     */
    public IFile getFile() {
        return file;
    }

}
