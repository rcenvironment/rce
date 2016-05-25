/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.connections.ConnectionDialogComposite.FilterMode;

/**
 * Section that shows the connection editor.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionsSection extends AbstractPropertySection {

    private static final int MINIMUM_HEIGHT_OF_CONNECTION_COMPOSITE = 300;

    private static final String IS_EXACTLY = "match exactly";

    private static final String STARTS_WITH = "start with";

    private static final String CONTAINS = "contain";

    private WorkflowDescription workflowDescription;

    private ConnectionDialogComposite connectionDialogComposite;

    private WorkflowDescriptionPropertyListener workflowDescriptionPropertyListener;

    private WorkflowEditor editor;

    private TabbedPropertySheetPage sheetPage;

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        // initialize
        if (part instanceof WorkflowEditor && connectionDialogComposite.getWorkflowDescription() == null) {
            editor = ((WorkflowEditor) part);
            synchronized (this) {
                workflowDescription = editor.getWorkflowDescription();
                editor.addPropertyListener(new IPropertyListener() {

                    @Override
                    public void propertyChanged(Object obj, int prop) {
                        if (prop == WorkflowEditor.PROP_FINAL_WORKFLOW_DESCRIPTION_SET) {
                            synchronized (ConnectionsSection.this) {
                                if (workflowDescription != editor.getWorkflowDescription()) { // if it is a new object
                                    workflowDescription = editor.getWorkflowDescription();
                                    connectionDialogComposite.updateConnectionViewer(workflowDescription);
                                }
                            }
                        }
                    }
                });
                connectionDialogComposite.initialize(workflowDescription, null, null);
                connectionDialogComposite.getCanvas().setEditorsCommandStack(editor.getEditorsCommandStack());
                connectionDialogComposite.setCommandStack(editor.getEditorsCommandStack());
            }
        }

        // initalize and register workflow description property change listener if not already done
        if (workflowDescriptionPropertyListener == null) {
            workflowDescriptionPropertyListener = new WorkflowDescriptionPropertyListener();
            if (workflowDescription != null) {
                workflowDescription.addPropertyChangeListener(workflowDescriptionPropertyListener);
            }
        }
    }

    private void setSizeOfConnectionComposite(Composite parent) {
        final int topMargin = 125;
        if (!parent.isDisposed() && !connectionDialogComposite.isDisposed()) {
            if (parent.getSize().y < MINIMUM_HEIGHT_OF_CONNECTION_COMPOSITE) {
                ((GridData) connectionDialogComposite.getLayoutData()).heightHint = MINIMUM_HEIGHT_OF_CONNECTION_COMPOSITE;
            } else {
                ((GridData) connectionDialogComposite.getLayoutData()).heightHint = parent.getSize().y - topMargin;
                connectionDialogComposite.update();
            }
        }
    }

    @Override
    public void aboutToBeShown() {
        connectionDialogComposite.markSectionAsInitialized();
    }

    @Override
    public void refresh() {
        setSizeOfConnectionComposite(connectionDialogComposite.getParent());
        super.refresh();
    }

    @Override
    public void dispose() {
        if (workflowDescription != null) {
            workflowDescription.removePropertyChangeListener(workflowDescriptionPropertyListener);
        }
        super.dispose();
    }

    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {

        connectionDialogComposite = new ConnectionDialogComposite(parent, SWT.NONE);

        sheetPage = aTabbedPropertySheetPage;

        GridLayout gridLayout = new GridLayout();
        parent.setLayout(gridLayout);

        GridData gridDataParentComposite = new GridData();
        gridDataParentComposite.horizontalAlignment = GridData.FILL;
        gridDataParentComposite.verticalAlignment = GridData.FILL;
        gridDataParentComposite.grabExcessHorizontalSpace = true;
        gridDataParentComposite.grabExcessVerticalSpace = true;
        parent.setLayoutData(gridDataParentComposite);

        GridLayout gridLayoutComposite = new GridLayout(3, false);
        gridLayoutComposite.horizontalSpacing = 0;
        gridLayoutComposite.marginWidth = 0;
        gridLayoutComposite.marginHeight = 0;
        connectionDialogComposite.setLayout(gridLayoutComposite);

        GridData gridDataConnectionComposite = new GridData();
        gridDataConnectionComposite.horizontalAlignment = GridData.FILL;
        gridDataConnectionComposite.verticalAlignment = GridData.FILL;
        gridDataConnectionComposite.grabExcessHorizontalSpace = true;
        gridDataConnectionComposite.grabExcessVerticalSpace = true;
        connectionDialogComposite.setLayoutData(gridDataConnectionComposite);

        // Target filter
        GridData gridDataTargetFilterText = new GridData();
        gridDataTargetFilterText.grabExcessHorizontalSpace = true;
        gridDataTargetFilterText.horizontalAlignment = GridData.FILL;
        gridDataTargetFilterText.horizontalSpan = 3;

        GridData gridDataTargetFilterModeGroup = new GridData();
        gridDataTargetFilterModeGroup.grabExcessHorizontalSpace = true;
        gridDataTargetFilterModeGroup.horizontalAlignment = GridData.FILL;

        // Create target mode group
        Group targetFilterModeGroup = new Group(connectionDialogComposite.getTargetGroup(), SWT.NONE);
        targetFilterModeGroup.setText("Keep components that ... ");
        targetFilterModeGroup.setLayout(new GridLayout(3, true));
        targetFilterModeGroup.setLayoutData(gridDataTargetFilterModeGroup);
        Button targetFilterModeButtonIsExactly = new Button(targetFilterModeGroup, SWT.RADIO);
        targetFilterModeButtonIsExactly.setText(IS_EXACTLY);
        Button targetFilterModeButtonStartsWith = new Button(targetFilterModeGroup, SWT.RADIO);
        targetFilterModeButtonStartsWith.setText(STARTS_WITH);
        Button targetFilterModeButtonContains = new Button(targetFilterModeGroup, SWT.RADIO);
        targetFilterModeButtonContains.setText(CONTAINS);
        targetFilterModeButtonContains.setSelection(true);

        final Text targetFilterText = new Text(targetFilterModeGroup, SWT.BORDER | SWT.FILL);
        targetFilterText.setMessage(Messages.filter);
        targetFilterText.setToolTipText(Messages.filterTooltip);
        targetFilterText.setLayoutData(gridDataTargetFilterText);

        // Register listener for mode
        TargetFilterModeSelectionListener targetListener = new TargetFilterModeSelectionListener();
        targetFilterModeButtonContains.addSelectionListener(targetListener);
        targetFilterModeButtonStartsWith.addSelectionListener(targetListener);
        targetFilterModeButtonIsExactly.addSelectionListener(targetListener);

        // Register listener for filter string
        targetFilterText.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                connectionDialogComposite.setTargetFilterString(targetFilterText.getText());
                connectionDialogComposite.applyTargetFilter();
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
                keyPressed(arg0);

            }
        });

        // Source filter
        GridData gridDataSourceFilterText = new GridData();
        gridDataSourceFilterText.grabExcessHorizontalSpace = true;
        gridDataSourceFilterText.horizontalAlignment = GridData.FILL;
        gridDataSourceFilterText.horizontalSpan = 3;

        GridData gridDataSourceFilterModeGroup = new GridData();
        gridDataSourceFilterModeGroup.grabExcessHorizontalSpace = true;
        gridDataSourceFilterModeGroup.horizontalAlignment = GridData.FILL;

        // Create source mode group
        Group sourceFilterModeGroup = new Group(connectionDialogComposite.getSourceGroup(), SWT.NONE);
        sourceFilterModeGroup.setText("Keep components that ... ");
        sourceFilterModeGroup.setLayout(new GridLayout(3, true));
        sourceFilterModeGroup.setLayoutData(gridDataSourceFilterModeGroup);
        Button sourceFilterModeButtonIsExactly = new Button(sourceFilterModeGroup, SWT.RADIO);
        sourceFilterModeButtonIsExactly.setText(IS_EXACTLY);
        Button sourceFilterModeButtonStartsWith = new Button(sourceFilterModeGroup, SWT.RADIO);
        sourceFilterModeButtonStartsWith.setText(STARTS_WITH);
        Button sourceFilterModeButtonContains = new Button(sourceFilterModeGroup, SWT.RADIO);
        sourceFilterModeButtonContains.setText(CONTAINS);
        sourceFilterModeButtonContains.setSelection(true);

        final Text sourceFilterText = new Text(sourceFilterModeGroup, SWT.BORDER | SWT.FILL);
        sourceFilterText.setMessage(Messages.filter);
        sourceFilterText.setToolTipText(Messages.filterTooltip);
        sourceFilterText.setLayoutData(gridDataSourceFilterText);

        // Register listener for mode
        SourceFilterModeSelectionListener sourceListener = new SourceFilterModeSelectionListener();
        sourceFilterModeButtonContains.addSelectionListener(sourceListener);
        sourceFilterModeButtonStartsWith.addSelectionListener(sourceListener);
        sourceFilterModeButtonIsExactly.addSelectionListener(sourceListener);

        // Register listener for filter string
        sourceFilterText.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                connectionDialogComposite.setSourceFilterString(sourceFilterText.getText());
                connectionDialogComposite.applySourceFilter();
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
                keyPressed(arg0);

            }
        });

        // Add resize listener to handle large endpoint trees
        aTabbedPropertySheetPage.getControl().addControlListener(new ConnectionSectionResizeListener());

        // When opening section, set default filter to "contains"
        connectionDialogComposite.setSourceFilterMode(FilterMode.CONTAINS);
        connectionDialogComposite.applySourceFilter();
        connectionDialogComposite.setTargetFilterMode(FilterMode.CONTAINS);
        connectionDialogComposite.applyTargetFilter();

        super.createControls(parent, aTabbedPropertySheetPage);
    }

    /**
     * Listener that reacts on resizing of the connection section.
     * 
     * @author Oliver Seebach
     *
     */
    private final class ConnectionSectionResizeListener implements ControlListener {

        @Override
        public void controlResized(ControlEvent arg0) {
            setSizeOfConnectionComposite((Composite) sheetPage.getControl());
        }

        @Override
        public void controlMoved(ControlEvent arg0) {}

    }

    /**
     * Listener that reacts on changes in the workflow description and triggers a connection composite update.
     * 
     * @author Oliver Seebach
     *
     */
    private final class WorkflowDescriptionPropertyListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(WorkflowDescription.PROPERTY_NODES_OR_CONNECTIONS)) {
                if (!connectionDialogComposite.isDisposed()) {
                    connectionDialogComposite.updateConnectionViewer(workflowDescription);
                }
            }
        }
    }

    /**
     * Listener for the filter mode of the source endpoint tree.
     * 
     * @author Oliver Seebach
     *
     */
    private class SourceFilterModeSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            Button button = ((Button) event.widget);
            if (button.getText().equals(CONTAINS)) {
                connectionDialogComposite.setSourceFilterMode(FilterMode.CONTAINS);
                connectionDialogComposite.applySourceFilter();
            } else if (button.getText().equals(STARTS_WITH)) {
                connectionDialogComposite.setSourceFilterMode(FilterMode.STARTSWITH);
                connectionDialogComposite.applySourceFilter();
            } else if (button.getText().equals(IS_EXACTLY)) {
                connectionDialogComposite.setSourceFilterMode(FilterMode.ISEXACTLY);
                connectionDialogComposite.applySourceFilter();
            }
        }

    }

    /**
     * Listener for the filter mode of the target endpoint tree.
     * 
     * @author Oliver Seebach
     *
     */
    private class TargetFilterModeSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            Button button = ((Button) event.widget);
            if (button.getText().equals(CONTAINS)) {
                connectionDialogComposite.setTargetFilterMode(FilterMode.CONTAINS);
                connectionDialogComposite.applyTargetFilter();
            } else if (button.getText().equals(STARTS_WITH)) {
                connectionDialogComposite.setTargetFilterMode(FilterMode.STARTSWITH);
                connectionDialogComposite.applyTargetFilter();
            } else if (button.getText().equals(IS_EXACTLY)) {
                connectionDialogComposite.setTargetFilterMode(FilterMode.ISEXACTLY);
                connectionDialogComposite.applyTargetFilter();
            }
        }

    }

}
