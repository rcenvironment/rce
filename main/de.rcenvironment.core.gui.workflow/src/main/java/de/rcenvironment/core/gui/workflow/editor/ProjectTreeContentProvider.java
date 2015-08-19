/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * ContentProvider for the ProjectTreeViewer of the WorkflowProjectWizard.
 * It provides projects and folders available in the current workspace.
 * 
 * @author Oliver Seebach
 * 
 */
public class ProjectTreeContentProvider implements ITreeContentProvider {


    @Override
    public void dispose() {
        
    }

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
        
    }

    @Override
    public Object[] getChildren(Object parentElement) {

        // Add projects as root nodes
        if (parentElement instanceof IProject){
            IPath path = ((IProject) parentElement).getLocation();
            File file = path.toFile();
            List<File> projects = new ArrayList<File>();
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    projects.add(f);
                    getChildren(f);
                }
            }
            return projects.toArray();
        }
        
        // Add folders as non-root nodes
        if (parentElement instanceof File){
            File file = (File) parentElement;
            List<File> folders = new ArrayList<File>();
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (f.isDirectory()) {
                        folders.add(f);
                    }
                }
            }
            return folders.toArray();
        }
        
        return null;
    }

    @Override
    public Object[] getElements(Object arg0) {
        
        // Order projects case insensitive alphabetically
        List<String> projectNames = new ArrayList<String>(); 
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()){
            projectNames.add(project.getName());
        }
        List<IProject> projects = new ArrayList<IProject>();
        Collections.sort(projectNames,  String.CASE_INSENSITIVE_ORDER);
        for (String projectName : projectNames){
            IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(projectName);
            projects.add(project);
        }
        
        return projects.toArray();
    }

    @Override
    public Object getParent(Object arg0) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (getChildren(element) != null && getChildren(element).length > 0
               && (element instanceof IProject || element instanceof IFile || element instanceof File)) {
            return true;
        }
        return false;
    }
}
