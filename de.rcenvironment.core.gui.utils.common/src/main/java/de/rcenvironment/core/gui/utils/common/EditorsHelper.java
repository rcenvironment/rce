/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.utils.common;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileInPlaceEditorInput;


/**
 * Helpers for handling of temporary files and workspace files, including dirty-state observing and editor detection.
 *
 * @author Arne Bachmann
 */
public final class EditorsHelper {

    /**
     * Default editor extension (fallback editor).
     */
    private static final String TXT = "txt";
    
    /**
     * "Index of" constant.
     */
    private static final int NOT_FOUND = -1;


    /**
     * This class has only static methods.
     */
    private EditorsHelper() {
        // only static methods
    }
    
    
    /**
     * Helper to determine the file extension.
     * @param filename The file name to check
     * @return the pure extension without a dot or txt as a fallback
     */
    public static String getExtension(final String filename) {
        if (filename == null) {
            return TXT; // default
        }
        final String f = filename.trim();
        final int lastSlash = f.lastIndexOf(File.separator); // works only on current node (same OS)
        final int lastDot = f.lastIndexOf(".");
        if (lastDot > lastSlash) { // found an extension
            return f.substring(lastDot + 1);
        }
        return TXT; // fallback
    }
    
    /**
     * Helper to get just any matching editor.
     * A message dialog pops up if nothing found (!).
     * @param filename The filename to find an editor for
     * @return The editor descriptor
     */
    private static IEditorDescriptor findEditorForFilename(final String filename) {
        
        IEditorDescriptor editor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(filename);
        
        if (editor == null) {
            editor = PlatformUI.getWorkbench().getEditorRegistry().getEditors("*." + TXT)[0];
        }
        
        return editor;
    }

    /**
     * Open an in-place editor within the editing view. Falls back to txt-editor.
     * @param ifile The file to open
     * @param callbacks The action to perform whenever save is activated, or null for none
     * @throws PartInitException on error
     */
    public static void openFileInEditor(final IFile ifile, final Runnable... callbacks) throws PartInitException {
        if (ifile == null) {
            return;
        }
        openAndObserveFileInEditor(new ObservedFile(ifile), findEditorForFilename(ifile.getName()), new FileInPlaceEditorInput(ifile),
            callbacks);
    }
    
    /**
     * Helper to open an external file.
     * Set it to read-only if necessary
     * 
     * @param file The file to open (mostly a temp file)
     * @param callbacks The action to perform whenever save is activated, or null for none
     * @throws PartInitException on error
     */
    public static void openExternalFileInEditor(final File file, final Runnable... callbacks) throws PartInitException {
        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getAbsolutePath()));
        
        openAndObserveFileInEditor(new ObservedFile(file), findEditorForFilename(file.getName()), new FileStoreEditorInput(fileStore),
            callbacks);
    }
    
    
    private static IEditorPart openAndObserveFileInEditor(final ObservedFile observedFile, final IEditorDescriptor editorDescriptor,
        final IEditorInput editorInput, final Runnable... callbacks) throws PartInitException {
        
        final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editorInput,
                editorDescriptor.getId());
        if (editor != null) { // if internal editor
            observeEditedFile(observedFile, editor, new Runnable() {
                @Override
                public void run() {
                    if (callbacks != null) {
                        for (final Runnable action: callbacks) {
                            action.run();
                        }
                    }
                }
            });
            // close editor on RCE shutdown as the file-property relation is gone after RCE is started again
            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
    
                @Override
                public boolean preShutdown(IWorkbench workbench, boolean arg1) {
                    return editor.getSite().getPage().closeEditor(editor, true);
                }
    
                @Override
                public void postShutdown(IWorkbench workbench) {}
            });
        }
        return editor;
    }
    
    
    
    /**
     * Observes a file and call the action upon each safe action.
     * @param observedFile The file in the local filesystem to observe
     * @param editor The editor we have opened
     * @param callback The action to perform on each safe (but not on undo that makes the editor clean again)
     */
    private static void observeEditedFile(final ObservedFile observedFile, final IEditorPart editor, final Runnable callback) {
        final AtomicLong timeStamp = new AtomicLong(observedFile.getLastModified()); // we simply need a final reference here
        editor.addPropertyListener(new IPropertyListener() {
            @Override
            public void propertyChanged(final Object source, final int id) {
                if (id == IEditorPart.PROP_DIRTY) {
                    if (!((IEditorPart) source).isDirty()) { // became clean, thus must have been saved
                        final long ts = observedFile.getLastModified();
                        if (ts > timeStamp.longValue()) {
                            timeStamp.set(ts);
                            callback.run();
                        }
                    }
                }
            }
        });
    }

    /**
     * Abstracts {@link File} and {@link IFile} objects.
     * 
     * @author Doreen Seider
     */
    static class ObservedFile {

        private File file;

        private IFile iFile;

        ObservedFile(IFile iFile) {
            this.iFile = iFile;
        }

        ObservedFile(File file) {
            this.file = file;
        }

        public long getLastModified() {
            if (file != null) {
                return file.lastModified();
            } else {
                return iFile.getModificationStamp();
            }
        }
    }

}
