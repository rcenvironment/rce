/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyComposite;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabSelectionListener;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.gui.workflow.GUIWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.gui.workflow.parts.ReadOnlyWorkflowEditorEditPartFactory;

/**
 * WorkflowEditor operating in read-only mode.
 * 
 * @author Martin Misiak
 */
public class ReadOnlyWorkflowEditor extends WorkflowEditor {

    private static final Log LOGGER = LogFactory.getLog(ReadOnlyWorkflowEditor.class);

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    public ReadOnlyWorkflowEditor() {
        super();
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySheetPage.class) {
            if (tabbedPropertySheetPage == null || tabbedPropertySheetPage.getControl() == null
                || tabbedPropertySheetPage.getControl().isDisposed()) {
                tabbedPropertySheetPage = new ReadOnlyTabbedPropertySheetPage(this);
            }
            return tabbedPropertySheetPage;
        } else {
            return super.getAdapter(type);
        }
    }

    /**
     * ReadOnlyTabbedPropertySheetPage hooks the "disable property editing" functionality into a TabbedPropertySheetPage.
     */
    private class ReadOnlyTabbedPropertySheetPage extends TabbedPropertySheetPage {

        ReadOnlyTabbedPropertySheetPage(ITabbedPropertySheetPageContributor tabbedPropertySheetPageContributor) {
            super(tabbedPropertySheetPageContributor);
            super.addTabSelectionListener(new ITabSelectionListener() {

                @Override
                public void tabSelected(ITabDescriptor arg0) {
                    disableCurrentPropertySection(tabbedPropertySheetPage);
                }
            });
        }

        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            super.selectionChanged(part, selection);
            disableCurrentPropertySection(tabbedPropertySheetPage);
        }
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = super.getViewer();
        viewer.setEditPartFactory(new ReadOnlyWorkflowEditorEditPartFactory());
        viewer.setContents(super.getWorkflowDescription());
        viewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
        viewer.setContextMenu(new MenuManager()); // removes context menu

        int[] eventTypes = { SWT.MouseDoubleClick, SWT.DragDetect };
        removeListeners(viewer.getControl(), eventTypes);

        tabbedPropertySheetPage = new ReadOnlyTabbedPropertySheetPage(this);
    }

    /**
     * Effectively disables hit-testing of the visual components of the tool palette.
     */
//    @Override
//    protected PaletteViewerProvider createPaletteViewerProvider() {
//        return new PaletteViewerProvider(getEditDomain()) {
//
//            @Override
//            public PaletteViewer createPaletteViewer(Composite parent) {
//                PaletteViewer pv = super.createPaletteViewer(parent);
//                pv.getVisualPartMap().clear();
//                return pv;
//            }
//
//        };
//    }

    /**
     * Default palette-state is set to hidden.
     */
//    @Override
//    protected FlyoutPreferences getPalettePreferences() {
//        FlyoutPreferences prefs = super.getPalettePreferences();
//        prefs.setPaletteState(8);
//        return prefs;
//    }

    @Override
    protected void loadWorkflowFromFile(final File wfFile, final GUIWorkflowDescriptionLoaderCallback wfdc) {

        if (wfFile != null) {
            Job job = new Job(Messages.openWorkflow) {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask(Messages.loadingComponents, 2);
                        monitor.worked(1);
                        workflowDescription = workflowExecutionService
                            .loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, wfdc, true);
                        initializeWorkflowDescriptionListener();
                        monitor.worked(1);

                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                if (getViewer().getControl() != null) {
                                    getViewer().setContents(workflowDescription);
//                                    getGraphicalControl().setVisible(true);
                                    if (getEditorSite() != null) {
                                        setFocus();
                                    }
                                    firePropertyChange(PROP_FINAL_WORKFLOW_DESCRIPTION_SET);
                                }
                            }
                        });
                    } catch (final WorkflowFileException e) {
                        LogFactory.getLog(getClass()).error("Failed to open workflow: " + wfFile.getAbsolutePath(), e);
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                // do not use Display.getDefault().getActiveShell() as this might return
                                // the progress monitor dialog
                                closeEditorAndShowMessage("Failed to open workflow file in read-only: The workflow was probably executed"
                                    + " with a different version of RCE than the one in use.");
                            }

                        });
                    } finally {
                        monitor.done();
                    }
                    return Status.OK_STATUS;

                };
            };
            job.setUser(true);
            job.schedule();
        }

        String fileName = getPartName();
        if (fileName != null) {
            String fileNameRO = fileName + " - Read only";
            setPartName(fileNameRO);
        }

    }

    /**
     * Identify towards the TabbedPropertySheetPage as a "WorkflowEditor" to receive all component properties, which are registered for this
     * Editor. TODO : Find a better way, than a hard-coded string.
     * 
     * @return The contributor ID of the WorkflowEditor.
     */

    @Override
    public String getContributorId() {
        return "de.rcenvironment.rce.gui.workflow.editor.WorkflowEditor";
    }

    public boolean isSaveOnCloseNeeded() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void doSave(IProgressMonitor arg0) {}

    /**
     * Disables the controls in the currently selected tab of the TabbedPropertySheetPage, making the Properties of a selected Component
     * non-editable.
     * 
     * @param tpsp TabbedPropertySheetPage responsible for holding the properties
     */
    private void disableCurrentPropertySection(TabbedPropertySheetPage tpsp) {
        if (tpsp instanceof TabbedPropertySheetPage) {
            Control swtControls = tpsp.getControl();
            if (swtControls instanceof TabbedPropertyComposite) {
                TabbedPropertyComposite propComposite = (TabbedPropertyComposite) swtControls;
                Composite tabComposite = propComposite.getTabComposite();
                recursiveSetDisabled(tabComposite);
            }
        }

    }

    /**
     * Recursively iterates an Composite until it reaches its leaf Controls, or it does not contain any more children, and sets them to
     * disabled. Some Composites/Controls receive a special treatment( Labels, StyledText, Tree ) instead of being set to disabled. Special
     * treatment is also applied for Object of different types than Composite/Control.
     * 
     * @param ctrl Input Control/Composite
     */
    private void recursiveSetDisabled(Object obj) {

        if (obj instanceof Composite) {
            Composite comp = (Composite) obj;
            for (Control c : comp.getChildren()) {
                recursiveSetDisabled(c);
            }
            // For composites which do not have children( Like CCombo, StyledText etc...)
            if (comp.getChildren().length == 0) {
                comp.setEnabled(false);
            }

            // No composite, must be a Control leaf
        } else if (obj instanceof Control) {
            Control ctrl = (Control) obj;
            ctrl.setEnabled(false);
        }

        // Apply special treatment for specific Composites/Controls and different Objects
        specialTreatment(obj);
        return;
    }

    /**
     * Provides special treatment of specific Composites/Controls for the Method recursiveSetDisabled(Control).
     * 
     * @param ctrl Input Control/Composite
     */
    private void specialTreatment(Object obj) {

        // Special Treatment of Composites
        if (obj instanceof Composite) {

            Composite comp = (Composite) obj;

            // Treatment: Tree
            if (comp instanceof Tree) {
                Tree t = (Tree) comp;
                int[] eventTypes = { SWT.DragDetect, SWT.Selection };
                removeListeners(t, eventTypes);
                // t.getItems()[0].
                t.setEnabled(true);
            }

            // Treatment: StyledText
            if (comp instanceof StyledText) {
                StyledText text = (StyledText) comp;
                text.setEnabled(true);
                text.setEditable(false);
            }

            // Treatment: CTabFolder
            if (comp instanceof CTabFolder) {
                CTabFolder folder = (CTabFolder) comp;
                CTabItem[] folderItems = folder.getItems();
                for (CTabItem cItem : folderItems) {
                    // Identify the "add new folder" button on the absence of SWT.CLOSE style
                    if (cItem.getStyle() != SWT.CLOSE) {
                        cItem.dispose();
                    } else {
                        cItem.setShowClose(false);
                    }

                }

            }

            // Special Treatment of Controls
        } else if (obj instanceof Control) {
            Control ctrl = (Control) obj;
            // Treatment: Label
            if (ctrl instanceof Label) {
                ctrl.setEnabled(true);
                int[] eventTypes = { SWT.MouseDown, SWT.MouseUp, SWT.MouseDoubleClick };
                removeListeners(ctrl, eventTypes);
            }
            // Treatment Controls: ...add more here
        }

        return;
    }

    /**
     * Removes all listeners of a SWT Widget which listen to specific eventTypes.
     * 
     * @param ctrl Control who's listeners are to be removed
     * @param eventTypes Listeners who listen to the events specified in this array get removed
     */
    public static void removeListeners(Widget ctrl, int[] eventTypes) {

        for (int eventType : eventTypes) {
            Listener[] listeners = ctrl.getListeners(eventType);
            for (Listener listener : listeners) {
                ctrl.removeListener(eventType, listener);
            }
        }
    }

}
