/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.common.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;

/**
 * Abstract workflow integration editor page.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public abstract class IntegrationEditorPage extends CTabItem implements IIntegrationEditorPage {

    private final String title;

    private CLabel message;

    private IntegrationEditor integrationEditor;

    private boolean isPageValid = true;

    private IntegrationEditorButtonBar buttonBar;

    private CTabFolder container;

    protected IntegrationEditorPage(IntegrationEditor integrationEditor, CTabFolder container, String title) {
        super(container, SWT.None);
        this.container = container;
        this.integrationEditor = integrationEditor;
        this.title = title;
    }

    public abstract void createContent(Composite composite);

    public Composite generatePage() {

        Composite page = new Composite(container, SWT.FILL);

        page.setLayout(new GridLayout(1, false));

        final CLabel pageTitle = new CLabel(page, SWT.NONE);
        pageTitle.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        pageTitle.setText(title);
        pageTitle.setMargins(4, 3, 4, 3);
        pageTitle.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_LIGHT_GREY));
        message = new CLabel(page, SWT.NONE);
        GridData subtitleGridData = new GridData(GridData.FILL_HORIZONTAL);
        message.setLayoutData(subtitleGridData);
        message.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_WHITE));

        ScrolledComposite scrolledComposite = new ScrolledComposite(page, SWT.V_SCROLL | SWT.H_SCROLL);
        scrolledComposite.setLayout(new GridLayout(1, false));
        scrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        Composite workflowIntegrationEditorPage = new Composite(scrolledComposite, SWT.FILL);
        workflowIntegrationEditorPage.setLayout(new GridLayout(1, false));
        workflowIntegrationEditorPage.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        workflowIntegrationEditorPage.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        buttonBar = new IntegrationEditorButtonBar(page, integrationEditor);
        this.createContent(workflowIntegrationEditorPage);

        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(workflowIntegrationEditorPage.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrolledComposite.setContent(workflowIntegrationEditorPage);

        return page;
    }

    public void setMessage(String text, Image image) {
        message.setText(text);
        message.setImage(image);
        setImage(image);
        message.getParent().layout();
    }

    public void setMessage(String text) {
        setMessage(text, null);
    }

    public abstract void update();

    @Override
    public boolean isPageValid() {
        return isPageValid;
    }

    public void setPageValid(boolean isValid) {
        this.isPageValid = isValid;
    }

    public void updateSaveButtonActivation() {
        integrationEditor.updateValid();
    }

    @Override
    public void setBackButtonEnabled(boolean enable) {
        buttonBar.setBackButtonEnabled(enable);
    }

    @Override
    public void setNextButtonEnabled(boolean enable) {
        buttonBar.setNextButtonEnabled(enable);
    }

    @Override
    public void setSaveButtonEnabled(boolean enable) {
        buttonBar.setSaveButtonEnabled(enable);
    }

    @Override
    public Object getData() {
        return this;
    }

}


