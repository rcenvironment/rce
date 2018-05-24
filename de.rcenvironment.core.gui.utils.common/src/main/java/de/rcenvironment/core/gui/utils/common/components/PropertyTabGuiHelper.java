/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.components;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Helper class to create unified-looking GUIs for everything connected with loading/saving of user data.
 * 
 * @author Arne Bachmann
 * @author Jan Flink
 */
public final class PropertyTabGuiHelper {

    /**
     * Offset for FormLayout.
     */
    public static final int OFFSET = 5;

    /**
     * Constant for the layout manager.
     */
    public static final int PERC_100 = 100;
    

    /**
     * The log instance.
     */    
    private static final Log LOGGER = LogFactory.getLog(PropertyTabGuiHelper.class);

    /**
     * Validates if current selection is instance of {@link IFile}.
     */
    private static FileValidator fileValidator = new FileValidator();

    /**
     * Selection type for files and/or directories.
     */
    private enum SelectionType {
        FILE,
        DIRECTORY,
        FILE_AND_DIRECTORY
    }
    

    /**
     * Hiding constructor. There are only static methods in this class.
     */
    private PropertyTabGuiHelper() {}

    /**
     * Create handsome buttons. Provide {@link LinkedHashMap}s for correct button and action order (left-right and top-down)
     * 
     * @param parent The composite to insert the buttons into. They will be layed out in a sub-container
     * @param factory Factory for the buttons
     * @param buttonActionMap A map of button name -> map of action name -> action
     * @return The created composite (sometimes needed in outer class), using formlayout
     */
    public static Composite createActionButtons(final Composite parent, final TabbedPropertySheetWidgetFactory factory,
        final Map<String, Map<String, Runnable>> buttonActionMap) {
        final Composite composite = factory.createFlatFormComposite(parent);
        composite.setLayout(new FormLayout());

        // first button
        FormData data = new FormData();
        data.top = new FormAttachment(0, OFFSET);
        data.left = new FormAttachment(0, OFFSET);
        Button lastButton = factory.createButton(composite, buttonActionMap.keySet().iterator().next(), SWT.NONE | SWT.FLAT);
        lastButton.addMouseListener(createActions(lastButton, buttonActionMap.values().iterator().next()));
        lastButton.setLayoutData(data);
        buttonActionMap.remove(buttonActionMap.keySet().iterator().next()); // remove first entry

        // remaining buttons
        for (final Entry<String, Map<String, Runnable>> entry : buttonActionMap.entrySet()) {
            data = new FormData();
            data.top = new FormAttachment(0, OFFSET);
            data.left = new FormAttachment(lastButton, OFFSET);
            lastButton = factory.createButton(composite, /* button label */entry.getKey(), SWT.NONE | SWT.FLAT);
            lastButton.addMouseListener(createActions(lastButton, entry.getValue()));
            lastButton.setLayoutData(data);
        }
        // data.right = new FormAttachment(PERC_100); // HINT: otherwise stretching the last button
        // to the right border, looks wrong
        return composite;
    }

    /**
     * Create a popup menu action with the specified actions on it.
     * 
     * @param actions The actions to show in the popup. Actions are called on GUI thread (!)
     * @return The selection listener
     */
    private static MouseListener createActions(final Button button, final Map<String, Runnable> actions) {
        return new MouseListener() {

            public void action(final MouseEvent event) {
                if (actions.size() > 1) {
                    final Menu menu = new Menu(button.getShell(), SWT.POP_UP);
                    menu.setLocation(button.toDisplay(event.x, event.y));
                    menu.setVisible(true);
                    for (final Entry<String, Runnable> entry : actions.entrySet()) {
                        final MenuItem item = new MenuItem(menu, SWT.NONE);
                        item.setText(entry.getKey());
                        item.addSelectionListener(new SelectionListener() {

                            @Override
                            public void widgetSelected(final SelectionEvent event) {
                                menu.setVisible(false);
                                menu.dispose();
                                entry.getValue().run();
                            }

                            @Override
                            public void widgetDefaultSelected(final SelectionEvent event) {
                                widgetSelected(event);
                            }
                        });
                    }
                } else { // one or zero actions for button
                    if (actions.size() == 1) {
                        actions.values().iterator().next().run();
                    }
                }
            }

            @Override
            public void mouseDown(final MouseEvent event) {
                action(event);
            }

            @Override
            public void mouseDoubleClick(final MouseEvent event) {}

            @Override
            public void mouseUp(final MouseEvent event) {}
        };
    }

    /**
     * Create main layout for inner components.
     * 
     * @param parent The tabbedpropertysheets parent
     * @param factory The widgetfactory
     * @param title The title of the section
     * @return The section to put a composite in, don't forget to set a section client = inner composite
     */
    public static Section createSingleColumnSectionComposite(final Composite parent,
        final TabbedPropertySheetWidgetFactory factory, final String title) {
        final Composite composite = factory.createFlatFormComposite(parent);
        final ColumnLayout layout = new ColumnLayout();
        layout.maxNumColumns = 1;
        composite.setLayout(layout);
        final Section section = factory.createSection(composite, Section.TITLE_BAR);
        section.setText(title);
        return section;
    }

    /**
     * Dialog for project files.
     * 
     * @param shell The shell to block modally
     * @param title The title to show
     * @param message The message to show
     * @return The ifile selected or null
     */
    public static IFile selectFileFromActiveProject(final Shell shell, final String title, final String message) {
        String project = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
            .getActiveEditor().getEditorInput().toString().split("/")[1];
        Object resource = selectFileOrDirectoryFromWorkspace(shell, title, message,
            ResourcesPlugin.getWorkspace().getRoot().getProject(project), SelectionType.FILE);
        // this should be replaced by an validator inselectFileOrDirectoryFromWorkspace()
        if (resource instanceof IFile) {
            return (IFile) resource;
        }
        return null;
    }

    /**
     * Dialog for project directories.
     * 
     * @param shell The shell to block modally
     * @param title The title to show
     * @param message The message to show
     * @return The ifolder selected or null
     */
    public static IFolder selectDirectoryFromActiveProject(final Shell shell, final String title, final String message) {
        String project = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
            .getActiveEditor().getEditorInput().toString().split("/")[1];

        return (IFolder) selectFileOrDirectoryFromWorkspace(shell, title, message,
            ResourcesPlugin.getWorkspace().getRoot().getProject(project), SelectionType.DIRECTORY);
    }

    /**
     * Dialog for project directories or the project root itself.
     * 
     * @param shell The shell to block modally
     * @param title The title to show
     * @param message The message to show
     * @return The ifolder/iproject selected or null
     */
    public static IResource selectDirectoryFromActiveProjectIncludingItsRoot(final Shell shell, final String title, final String message) {
        final IProject project = getProjectOfCurrentlyActiveEditor();

        final ElementTreeSelectionDialog selectionDialog = new ElementTreeSelectionDialog(shell,
            new WorkbenchLabelProvider(),
            new BaseWorkbenchContentProvider()) {

            @Override
            protected Control createContents(Composite parent) {
                final Control result = super.createContents(parent);
                
                if (project != null) {
                    getTreeViewer().setExpandedElements(new Object[] { project });
                }
                return result;
            }

            @Override
            protected void updateButtonsEnableState(IStatus status) {
                getOkButton().setEnabled(status.isOK());
            }

        };
        
        selectionDialog.addFilter(new ViewerFilter() {
            
            @Override
            public boolean select(Viewer viewer, Object parent, Object element) {
                return element == project || element instanceof IFolder;
            }
        });
        
        selectionDialog.setStatusLineAboveButtons(false);
        selectionDialog.setTitle(title);
        selectionDialog.setMessage(message);
        selectionDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        selectionDialog.setInitialSelection(project);
        
        if (selectionDialog.open() == ElementTreeSelectionDialog.OK && selectionDialog.getResult().length > 0) {
            return (IResource) selectionDialog.getResult()[0];
        }
        return null;

    }

    /**
     * Dialog for project files.
     * 
     * @param shell The shell to block modally
     * @param title The title to show
     * @param message The message to show
     * @return The ifile selected or null
     */
    public static IFile selectFileFromProjects(final Shell shell, final String title, final String message) {
        fileValidator.setFileSuffix("");
        return (IFile) selectFileOrDirectoryFromWorkspace(shell, title, message, ResourcesPlugin.getWorkspace().getRoot(),
            SelectionType.FILE);
    }
    

    /**
     * Dialog for project files with a given suffix.
     * 
     * @param shell shell The shell to block modally
     * @param title The title to show
     * @param message The message to show
     * @param fileSuffix The file suffix to validate
     * @return The ifile selected or null
     */
    public static IFile selectFileFromProjects(Shell shell, String title, String message, String fileSuffix) {
        fileValidator.setFileSuffix(fileSuffix);
        return (IFile) selectFileOrDirectoryFromWorkspace(shell, title, message, ResourcesPlugin.getWorkspace().getRoot(),
            SelectionType.FILE);
    }
       

    private static Object selectFileOrDirectoryFromWorkspace(final Shell shell, final String title, final String message,
        Object input, final SelectionType filter) {
        final IProject project = getProjectOfCurrentlyActiveEditor();
        final ElementTreeSelectionDialog selectionDialog = new ElementTreeSelectionDialog(shell,
            new WorkbenchLabelProvider(),
            new BaseWorkbenchContentProvider()) {


            @Override
            protected void updateStatus(IStatus status) {
                updateButtonsEnableState(status);
            }

            @Override
            protected Control createContents(Composite parent) {
                final Control result = super.createContents(parent);
                if (project != null) {
                    getTreeViewer().setExpandedElements(new Object[] { project });
                }
                return result;
            }

            @Override
            protected void updateButtonsEnableState(IStatus status) {
                getOkButton().setEnabled(status.isOK());
            }

        };

        selectionDialog.setStatusLineAboveButtons(false);

        if (filter == SelectionType.FILE) {
            selectionDialog.setValidator(fileValidator);
        } else if (filter == SelectionType.DIRECTORY) {
            selectionDialog.addFilter(new ViewerFilter() {

                @Override
                public boolean select(Viewer viewer, Object parent, Object element) {
                    return element instanceof IFolder;
                }
            });
        }
        selectionDialog.setTitle(title);
        selectionDialog.setMessage(message);
        selectionDialog.setInput(input);
        selectionDialog.setAllowMultiple(false);
        if (selectionDialog.open() == ElementTreeSelectionDialog.OK && selectionDialog.getResult().length > 0) {
            return selectionDialog.getResult()[0];
        }
        return null;
    }

    /**
     * Dialog for file system files.
     * 
     * @param shell The shell to block modally
     * @param extensionFilters The filter expression, e.g. *.py
     * @param title The dialog title
     * @param filterPath The path the dialog should start, null if default
     * @return The filename or null
     */
    public static String selectFileFromFileSystem(final Shell shell, final String[] extensionFilters,
        final String title, final String filterPath) {
        final FileDialog fileDialog = new FileDialog(shell);
        fileDialog.setFilterExtensions(extensionFilters);
        fileDialog.setText(title);
        fileDialog.setFilterPath(filterPath);
        return fileDialog.open();
    }

    /**
     * Dialog for file system files.
     * 
     * @param shell The shell to block modally
     * @param extensionFilters The filter expression, e.g. *.py
     * @param title The dialog title
     * @return The filename or null
     */
    public static String selectFileFromFileSystem(final Shell shell, final String[] extensionFilters,
        final String title) {
        return selectFileFromFileSystem(shell, extensionFilters, title, null);
    }

    /**
     * Dialog for file system directories.
     * 
     * @param shell The shell to block modally
     * @param title The dialog title
     * @return The filename or null
     */
    public static String selectDirectoryFromFileSystem(final Shell shell, final String title) {
        final DirectoryDialog fileDialog = new DirectoryDialog(shell);
        fileDialog.setText(title);
        return fileDialog.open();
    }

    /**
     * Return the path expression to refer to the active editor's project, relative to the current workbench root.
     * 
     * @return The name of the project or null if not found
     */
    public static IProject getProjectOfCurrentlyActiveEditor() {
        String source = "Return the project path of the active editor: %s";
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IEditorInput input = null;
        if (window != null) {
            final IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                final IEditorPart editor = page.getActiveEditor();
                if (editor == null) {
                    LOGGER.debug(StringUtils.format(source, "No active editor in workbench page"));
                    return null;
                } else {
                    input = editor.getEditorInput();
                }
            } else {
                LOGGER.debug(StringUtils.format(source, "No active page in workbench"));
                return null;
            }
        } else {
            LOGGER.debug(StringUtils.format(source, "No active workbench window"));
            return null;
        }
        // second if-chain to avoid deep nesting
        if (input.exists()) {
            if (input instanceof FileEditorInput) {
                return ((FileEditorInput) input).getFile().getProject();
            } else {
                LOGGER.debug(StringUtils.format(source, "Wrong type of active editor input " + input.getClass()));
            }
        } else {
            LOGGER.debug(StringUtils.format(source, "Editor input does not exist"));
        }
        return null;
    }

}

/**
 * Custom file validator implementation that additionally validates a given file suffix.
 *
 * @author Jan Flink
 */
class FileValidator implements ISelectionStatusValidator {

    private String fileSuffix = "";

    /**
     * Sets the file suffix for validation.
     * 
     * @param suffix The file suffix without the leading dot.
     */
    public void setFileSuffix(String suffix) {
        this.fileSuffix = suffix;
    }

    @Override
    public IStatus validate(Object[] selection) {
        if (selection.length == 1 && selection[0] instanceof IFile) {
            if (!fileSuffix.equals("") && !selection[0].toString().endsWith("." + fileSuffix)) {
                return Status.CANCEL_STATUS;
            }
            return Status.OK_STATUS;
        }
        return Status.CANCEL_STATUS;
    }
}
