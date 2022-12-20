/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import java.util.Arrays;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.ComponentNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.MappingNode;

public class MappingTreeFilterWidget implements ModifyListener, KeyListener {

    private static final String FILTER_DELIMITER = ";";

    private static final int FILTER_TEXTFIELD_WIDTH = 150;

    private static final String FILTER_MESSAGE = "Filter tree...";

    private Text filterTextfield;

    private ViewerFilter filter;

    private String filterText;

    private boolean filterSet;

    private Object[] expandedElements;

    private CheckboxTreeViewer treeViewer;

    public MappingTreeFilterWidget(Composite parent, CheckboxTreeViewer treeViewer) {
        createControl(parent);
        this.treeViewer = treeViewer;
    }

    protected Control createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_VERTICAL | SWT.NO_BACKGROUND));
        GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginHeight = 0;
        compositeLayout.marginWidth = 0;
        composite.setLayout(compositeLayout);
        filterTextfield = new Text(composite, SWT.BORDER | SWT.NO_FOCUS);
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        layoutData.widthHint = FILTER_TEXTFIELD_WIDTH;
        filterTextfield.setLayoutData(layoutData);
        filterTextfield.setMessage(FILTER_MESSAGE);
        filterTextfield.addModifyListener(this);
        filterTextfield.addKeyListener(this);
        createFilter();
        return composite;
    }

    private void createFilter() {
        filter = new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object elementObject) {
                if (elementObject instanceof ComponentNode) {
                    ComponentNode node = (ComponentNode) elementObject;
                    boolean childMatch = Arrays.stream(node.getChildren())
                        .anyMatch(child -> select(viewer, node, child));
                    return childMatch || node.getComponentName().toLowerCase().contains(filterText.toLowerCase());
                }
                if (elementObject instanceof MappingNode) {
                    MappingNode node = (MappingNode) elementObject;
                    return Arrays.stream(node.getFilterString().split(FILTER_DELIMITER))
                        .anyMatch(n -> n.toLowerCase().contains(filterText.toLowerCase()));
                }
                return false;
            }

            @Override
            public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
                if (parent instanceof ComponentNode
                    && ((ComponentNode) parent).getComponentName().toLowerCase().contains(filterText.toLowerCase())) {
                    return elements;
                }
                return super.filter(viewer, parent, elements);
            }

        };
    }

    @Override
    public void modifyText(ModifyEvent e) {
        if (e.widget instanceof Text) {
            Text filterTextField = (Text) e.widget;
            filterText = filterTextField.getText();
            if (filterText != null && filterText.length() > 0) {
                if (!filterSet) {
                    filterSet = true;
                    expandedElements = treeViewer.getExpandedElements();
                }
                treeViewer.cancelEditing();
                treeViewer.getTree().deselectAll();
                treeViewer.getTree().setVisible(false);
                treeViewer.setFilters(filter);
                treeViewer.expandAll(); // to show filtered nodes even if Component Node was unexpanded before
                treeViewer.refresh();
                treeViewer.getTree().setVisible(true);
            } else {
                treeViewer.getTree().setVisible(false);
                treeViewer.resetFilters();
                filterSet = false;
                if (expandedElements != null) {
                    treeViewer.setExpandedElements(expandedElements);
                    treeViewer.refresh();
                }
                treeViewer.getTree().setVisible(true);
            }
        }

    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        // Intenionally left empty
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.character == SWT.ESC) {
            filterTextfield.setText("");
            treeViewer.getTree().setFocus();
        }
    }

}
