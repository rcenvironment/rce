/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.wizards.exampleproject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.osgi.framework.Bundle;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the provided container. If the container resource (a folder or
 * a project) is selected in the workspace when the wizard is opened, it will accept it as the target container. The wizard creates one file
 * with the extension "mpe". If a sample multi-page editor (also available as a template) is registered for the same extension, it will be
 * able to open it.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Dominik Schneider
 */

public abstract class NewExampleProjectWizard extends Wizard implements INewWizard {

    private static final String ASTERISK = "*";

    private static final String APOSTROPH = "'";

    private NewExampleProjectWizardPage page;

    private ISelection selection;

    private Optional<Consumer<String>> projectNameListener = Optional.empty();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Constructor for NewDemoProjectWizard.
     */
    public NewExampleProjectWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        page = new NewExampleProjectWizardPage(selection, this);
        Bundle bundle = Platform.getBundle(getPluginID());
        // enumerate template folder sub-directories
        @SuppressWarnings("rawtypes") final Enumeration templateDirs = bundle.findEntries("templates", ASTERISK, false);
        while (templateDirs.hasMoreElements()) {
            final URL elementURL = (URL) templateDirs.nextElement();
            final String rawPath = elementURL.getPath();
            if (!rawPath.endsWith("/.svn/")) {
                log.debug("Located template folder '" + elementURL + APOSTROPH);
            }
        }
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using wizard as execution
     * context.
     * 
     * @return boolean : done
     */
    @Override
    public boolean performFinish() {

        final String newProjectName = page.getNewProjectName();
        if (this.projectNameListener.isPresent()) {
            this.projectNameListener.get().accept(newProjectName);
        }

        final int copyToolAnswer = page.getCreateTIExample();
        final boolean copyTool;
        if (copyToolAnswer == NewExampleProjectWizardPage.COPY_EXAMPLE_TOOL) {
            copyTool = true;
        } else if (copyToolAnswer == NewExampleProjectWizardPage.RED_X_CANCELD) {
            return false;
        } else {
            copyTool = false;
        }

        IRunnableWithProgress op = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doFinish(getTemplateFoldername(), newProjectName, monitor, copyTool);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }
        return true;
    }

    private void copyExampleTool(Bundle bundle) {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        ConfigurationService configurationService = serviceRegistryAccess.getService(ConfigurationService.class);
        File integrationFolder = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTEGRATION_DATA);
        File commonsFolder = new File(new File(integrationFolder, "tools"), "common");
        if (!commonsFolder.exists()) {
            commonsFolder.mkdirs();
        }
        try {
            @SuppressWarnings("rawtypes") final Enumeration toolFiles =
                bundle.findEntries("templates/" + "integration_example", ASTERISK, true);
            while (toolFiles.hasMoreElements()) {
                final URL elementURL = (URL) toolFiles.nextElement();
                final String rawPath = elementURL.getPath();
                final String targetPath = rawPath.replaceFirst("^/templates/\\w+/", "");
                final File target = new File(commonsFolder, targetPath);

                if (target.isDirectory() || rawPath.endsWith("/")) {
                    target.mkdirs();
                } else {
                    InputStream fileStream = null;
                    try {
                        fileStream = elementURL.openStream();
                        FileUtils.copyInputStreamToFile(fileStream, target);
                    } catch (IOException e) {
                        log.error("Could not copy tool integration example", e);
                    } finally {
                        if (fileStream != null) {
                            fileStream.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not copy tool integration example", e);
        }

    }

    /**
     * The worker method. It will find the container, create the file if missing or just replace its contents, and open the editor on the
     * newly created file.
     * 
     * @param templateName
     */

    private void doFinish(String templateName, String newProjectName, IProgressMonitor monitor, boolean copyIntegrationExample)
        throws CoreException {

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(newProjectName);
        project.create(monitor);
        project.open(monitor);
        monitor.worked(1);
        Bundle bundle = Platform.getBundle(getPluginID());
        @SuppressWarnings("rawtypes") final Enumeration templateFiles = bundle.findEntries("templates/" + templateName, ASTERISK, true);
        log.debug("Copying project template '" + templateName + APOSTROPH);
        try {
            while (templateFiles.hasMoreElements()) {
                final URL elementURL = (URL) templateFiles.nextElement();
                final String rawPath = elementURL.getPath();
                if (rawPath.endsWith("/") || rawPath.contains("/.svn/")) {
                    log.debug("Ignoring template resource '" + elementURL);
                    continue;
                }
                final String targetPath = rawPath.replaceFirst("^/templates/\\w+/", "");
                IFile file = project.getFile(targetPath);
                if (!file.getParent().exists()) {
                    IFolder parent = (IFolder) file.getParent();
                    parent.create(true, true, null);
                }
                log.debug("Copying template file '" + elementURL + " to '" + targetPath + APOSTROPH);
                InputStream fileStream = null;
                try {
                    fileStream = elementURL.openStream();
                    file.create(fileStream, true, monitor);
                } finally {
                    if (fileStream != null) {
                        fileStream.close();
                    }
                }
            }

            if (copyIntegrationExample) {
                copyExampleTool(bundle);
            }
        } catch (IOException e) {
            log.error("Error while creating example project", e);
        }

    }

    /**
     * We will accept the selection in the workbench to see if we can initialize from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     * @param workbench : wb
     * @param newSelection : sel
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection newSelection) {
        this.selection = newSelection;
    }

    /**
     * Allows to register a consumer which retrieves the new project name on creating the workflow examples project.
     * 
     * @param consumer to hold the new project name.
     */
    public void registerProjectNameListener(Consumer<String> consumer) {
        this.projectNameListener = Optional.of(consumer);
    }

    /**
     * Used to set project default name in inhertited class.
     * 
     * @return String
     */
    public abstract String getProjectDefaultName();

    /**
     * Used to set plugin id in inhertited class.
     * 
     * @return String
     */
    public abstract String getPluginID();

    /**
     * Used to set template foldername in inhertited class.
     * 
     * @return String
     */
    public abstract String getTemplateFoldername();

}
