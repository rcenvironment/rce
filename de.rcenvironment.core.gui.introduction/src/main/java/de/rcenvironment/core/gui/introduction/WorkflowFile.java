/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.introduction;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

/**
 * Represents a .wf-file.
 *
 * @author Alexander Weinert
 */
final class WorkflowFile {
    
    private final IPath path;
    
    private WorkflowFile(IPath path) {
        this.path = path;
    }

    public static WorkflowFile fromPath(IPath path) {
        return new WorkflowFile(path);
    }

    public boolean canBeLoadedIntoWorkflowEditor() {
        final File file = this.path.toFile();
        return file.exists() && file.isFile();
    }
    
    private IFileStore getFileStore() {
        return EFS.getLocalFileSystem().getStore(this.path);
    }

    public void loadIntoWorkbenchPage(IWorkbenchPage page) throws PartInitException {
        final IFileStore fileStore = getFileStore();
        IDE.openEditorOnFileStore(page, fileStore);
    }

}
