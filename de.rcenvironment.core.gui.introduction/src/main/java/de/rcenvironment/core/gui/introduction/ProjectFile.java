/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.introduction;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Represents the file .project that defines an Eclipse project and is found at the root of a directory containing an Eclipse project.
 * 
 * @author Alexander Weinert
 */
final class ProjectFile {
    
    private static final String PROJECT_FILE_NAME = ".project";
    private final IPath absolutePathToProjectFolder;
    
    private ProjectFile(final IPath absolutePathToProjectFolder) {
        this.absolutePathToProjectFolder = absolutePathToProjectFolder;
    }

    public static ProjectFile createForProjectFolder(IPath absolutePathToProjectFolder) {
        return new ProjectFile(absolutePathToProjectFolder);
    }

    public IProjectDescription getProjectDescription() throws CoreException {
        final IPath pathToProjectFile = getPathToProjectFile();
        return ResourcesPlugin.getWorkspace().loadProjectDescription(pathToProjectFile);
    }

    private IPath getPathToProjectFile() {
        return absolutePathToProjectFolder.append(PROJECT_FILE_NAME);
    }

    public IProject getProjectFromWorkspaceByName() throws CoreException {
        final IProjectDescription projectDescription = getProjectDescription();
        return ResourcesPlugin.getWorkspace().getRoot().getProject(projectDescription.getName());
    }

    public boolean exists() {
        return getPathToProjectFile().toFile().exists();
    }

}
