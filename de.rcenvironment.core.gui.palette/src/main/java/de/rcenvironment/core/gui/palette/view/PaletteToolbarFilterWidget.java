/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view;

import java.util.Arrays;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.viewers.StructuredViewer;
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

import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Filter widget for the Palette view's toolbar.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class PaletteToolbarFilterWidget extends ControlContribution implements ModifyListener, KeyListener {

    private static final int FILTER_TEXTFIELD_WIDTH = 100;

    private static final String FILTER_MESSAGE = "Filter components...";

    private PaletteView paletteView;

    private String filterText;

    private ViewerFilter filter;

    private Text filterTextfield;

    private boolean filterSet = false;

    private Object[] expandedElements;

    public PaletteToolbarFilterWidget(PaletteView paletteView) {
        super("de.rcenvironment.core.gui.palette.view.paletteToolbarFilterWidget");
        this.paletteView = paletteView;
    }

    private void createFilter() {
        filter = new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object elementObject) {
                if (!(viewer instanceof StructuredViewer)) {
                    throwIllegalTypeException("viewer", viewer.getClass().getCanonicalName(), StructuredViewer.class.getCanonicalName());
                }
                if (!(elementObject instanceof PaletteTreeNode)) {
                    throwIllegalTypeException("elementObject", elementObject.getClass().getCanonicalName(),
                        PaletteTreeNode.class.getCanonicalName());
                }
                PaletteTreeNode element = (PaletteTreeNode) elementObject;
                if (element.isCreationTool()) {
                    return true;
                }
                if (element.isGroup() && element.hasChildren()) {
                    return Arrays.stream(element.getChildren())
                        .anyMatch(child -> select(viewer, element, child));
                }
                if (element.isAccessibleComponent()) {
                    String compareString = element.getDisplayName();
                    return compareString.toLowerCase().contains(filterText.toLowerCase().trim());
                }
                return false;
            }

            private void throwIllegalTypeException(String variableName, String actualClassName, String expectedClassName) {
                throw new IllegalStateException(StringUtils.format(
                    "Unexpected type for parameter %s: %s. Expected type: %s", variableName, actualClassName,
                    expectedClassName));
            }
        };
    }

    @Override
    protected Control createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginHeight = 0;
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

    @Override
    public void modifyText(ModifyEvent e) {
        if (e.widget instanceof Text) {
            Text filterTextField = (Text) e.widget;
            filterText = filterTextField.getText();
            if (filterText != null && filterText.length() > 0) {
                if (!filterSet) {
                    filterSet = true;
                    expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
                }
                paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                paletteView.getPaletteTreeViewer().setFilters(filter);
                paletteView.getPaletteTreeViewer().expandAll(); // to show filtered nodes even if group was unexpanded before
                paletteView.getPaletteTreeViewer().refresh();
                paletteView.getPaletteTreeViewer().getTree().setVisible(true);
            } else {
                paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                paletteView.getPaletteTreeViewer().resetFilters();
                filterSet = false;
                if (expandedElements != null) {
                    paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
                    paletteView.getPaletteTreeViewer().refresh();
                }
                paletteView.getPaletteTreeViewer().getTree().setVisible(true);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Intentionally left empty.
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.character == SWT.ESC) {
            filterTextfield.setText("");
            paletteView.getPaletteTreeViewer().getTree().setFocus();
        }

    }
}
