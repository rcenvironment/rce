/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.common.editor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Abstract integration editor.
 *
 * @author Jan Flink
 */
public abstract class IntegrationEditor extends MultiPageEditorPart implements IWorkbenchListener, ISaveablePart2 {

    private static final String CLOSING_DIALOG_LOST_CHANGES =
        "The changes in the editor '%s' will be lost on close.\nDo you really want to close the editor?";

    private static final String CLOSING_DIALOG_INTEGRATE = "Integrate '%s'? ";

    private static final String BUTTON_CANCEL = "Cancel";

    private static final String BUTTON_DON_T_INTEGRATE = "Do not Integrate";

    private static final String BUTTON_DON_T_UPDATE = "Do not Update Integration";

    private boolean isValid;

    protected IntegrationEditor() {
        super();
        PlatformUI.getWorkbench().addWorkbenchListener(this);
    }

    @Override
    public int promptToSaveOnClose() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getActiveWorkbenchWindow().getActivePage().bringToTop(this);
        String buttonDonTIntegrate;
        if (isEditMode()) {
            buttonDonTIntegrate = BUTTON_DON_T_UPDATE;
        } else {
            buttonDonTIntegrate = BUTTON_DON_T_INTEGRATE;
        }

        if (this.isValid) {
            switch (MessageDialog.open(MessageDialog.QUESTION, workbench.getModalDialogShellProvider().getShell(),
                getDialogTitle(),
                StringUtils.format(CLOSING_DIALOG_INTEGRATE, getTitle()), 0,
                getButtonTextIntegrate(), buttonDonTIntegrate, BUTTON_CANCEL)) {
            case 0:
                integrate();
                return ISaveablePart2.NO;
            case 1:
                return ISaveablePart2.NO;
            default:
                return ISaveablePart2.CANCEL;
            }
        } else {
            if (MessageDialog.openQuestion(workbench.getModalDialogShellProvider().getShell(), getDialogTitle(),
                StringUtils.format(CLOSING_DIALOG_LOST_CHANGES, getTitle()))) {
                return ISaveablePart2.NO;
            }
            return ISaveablePart2.CANCEL;
        }
    }

    public abstract String getButtonTextIntegrate();

    public abstract String getDialogTitle();

    @Override
    public boolean isDirty() {
        return getPages().stream().anyMatch(IIntegrationEditorPage::hasChanges) || !isEditMode();
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return true;
    }

    @Override
    public boolean preShutdown(IWorkbench workbench, boolean arg1) {
        return workbench.getActiveWorkbenchWindow().getActivePage().closeEditor(this, true);
    }

    @Override
    public void postShutdown(IWorkbench workbench) {
    }

    private void setNavigationButtonActivation() {
        CTabFolder tabFolder = (CTabFolder) getContainer();
        Arrays.stream(tabFolder.getItems()).filter(item -> (item.getData() instanceof IIntegrationEditorPage)).forEach(item -> {
            int index = tabFolder.indexOf(item);
            IIntegrationEditorPage page = (IIntegrationEditorPage) item.getData();
            page.setBackButtonEnabled(index != 0);
            page.setNextButtonEnabled(index < tabFolder.getItemCount() - 1);
        });
    }


    @Override
    protected void initializePageSwitching() {
        super.initializePageSwitching();
        setNavigationButtonActivation();
    }

    @Override
    public abstract void doSave(IProgressMonitor arg0);

    @Override
    public void doSaveAs() {
        // Intentionally left empty
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }
    
    

    public void updateValid() {
        this.isValid = getPages().stream().allMatch(IIntegrationEditorPage::isPageValid);
        getPages().stream().forEach(page -> page.setSaveButtonEnabled(isValid));
    }

    public abstract boolean isEditMode();

    public void setNextPage() {
        if (getActivePage() < getPageCount() - 1) {
            setActivePage(getActivePage() + 1);
        }
    }

    public void setPreviousPage() {
        if (getActivePage() > 0) {
            setActivePage(getActivePage() - 1);
        }

    }

    @Override
    public void dispose() {
        PlatformUI.getWorkbench().removeWorkbenchListener(this);
        super.dispose();
    }

    public abstract void integrate();

    public void cancelPressed() {
        getSite().getPage().closeEditor(this, false);
    }

    public List<IIntegrationEditorPage> getPages() {
        CTabFolder tabFolder = (CTabFolder) getContainer();
        return Arrays.stream(tabFolder.getItems()).map(CTabItem::getData)
            .filter(IIntegrationEditorPage.class::isInstance).map(IIntegrationEditorPage.class::cast).collect(Collectors.toList());
    }

    public void updateDirty() {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        
    }
}
