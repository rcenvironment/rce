/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette.view.dialogs;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;


/**
 * An Action to manage the custom groups.
 *
 * @author Kathrin Schaffert
 * 
 */
public class ManageCustomGroupsAction extends Action {

    private PaletteView paletteView;
    
    public ManageCustomGroupsAction(PaletteView paletteView) {
        super("Manage Custom Groups ...");
        setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_MANAGE));
        this.paletteView = paletteView;
    }
    
    @Override
    public void run() {
        Shell shell = Display.getDefault().getActiveShell();
        ManageCustomGroupsDialog organizeGroupsDialog =
            new ManageCustomGroupsDialog(shell, paletteView);
        paletteView.setOrganizeGroupsDialog(organizeGroupsDialog);
        organizeGroupsDialog.open();
        paletteView.selectSelectionToolNode();
    }
    
}
