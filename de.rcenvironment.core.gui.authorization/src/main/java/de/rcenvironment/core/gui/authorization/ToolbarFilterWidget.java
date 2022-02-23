/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;

/**
 * Filter widget for tables and trees in authorization view and dialogs.
 *
 * @author Jan Flink
 */
public class ToolbarFilterWidget extends ControlContribution implements ModifyListener {

    private static final int FILTER_TEXTFIELD_WIDTH = 120;

    private String defaultMessage;

    private String filterText;

    private StructuredViewer structuredViewer;

    private ViewerFilter filter;

    private Text filterTextfield;

    protected ToolbarFilterWidget(StructuredViewer structuredViewer) {
        this(structuredViewer, "Filter...");
    }

    protected ToolbarFilterWidget(StructuredViewer structuredViewer, String message) {
        super("viewerFilter");
        this.structuredViewer = structuredViewer;
        defaultMessage = message;
    }

    protected void setViewer(StructuredViewer viewer) {
        this.structuredViewer = viewer;
        filterText = "";
        filterTextfield.setText("");
    }

    protected void setMessage(String message) {
        if (filterTextfield != null && !filterTextfield.isDisposed()) {
            filterTextfield.setMessage(message);
        }
    }

    private void createFilter() {
        filter = new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                String compareString = "";
                if (element instanceof AuthorizationAccessGroup && parentElement instanceof NamedComponentAuthorizationSelector) {
                    return true;
                }
                if (element instanceof NamedComponentAuthorizationSelector && parentElement instanceof AuthorizationAccessGroup) {
                    return true;
                }
                if (viewer instanceof StructuredViewer) {
                    IBaseLabelProvider labelProvider = ((StructuredViewer) viewer).getLabelProvider();
                    if (labelProvider instanceof AuthorizationLabelProvider) {
                        compareString = ((AuthorizationLabelProvider) labelProvider).getText(element);
                        return compareString.toLowerCase().contains(filterText.toLowerCase().trim());
                    }
                }
                return true;
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
        GridData layoutData = new GridData(SWT.LEFT, SWT.TOP, true, false);
        layoutData.widthHint = FILTER_TEXTFIELD_WIDTH;
        layoutData.verticalAlignment = GridData.CENTER;
        filterTextfield.setLayoutData(layoutData);
        setMessage(defaultMessage);
        filterTextfield.addModifyListener(this);
        createFilter();
        return composite;
    }

    @Override
    public void modifyText(ModifyEvent e) {
        if (e.widget instanceof Text) {
            Text filterTextField = (Text) e.widget;
            filterText = filterTextField.getText();
            if (filterText != null && filterText.length() > 0) {
                structuredViewer.setFilters(filter);
            } else {
                structuredViewer.resetFilters();
            }
        }
    }
}
