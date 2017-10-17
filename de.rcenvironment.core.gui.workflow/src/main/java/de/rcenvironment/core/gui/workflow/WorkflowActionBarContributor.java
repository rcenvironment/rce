/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInRetargetAction;
import org.eclipse.gef.ui.actions.ZoomOutRetargetAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Adds zoom capabilities to the menu and tool bar.
 * 
 * @author Jan Flink
 */
public class WorkflowActionBarContributor extends ActionBarContributor {

    @Override
    public void contributeToToolBar(IToolBarManager toolBarManager) {
        toolBarManager.add(getAction(GEFActionConstants.ZOOM_IN));
        toolBarManager.add(getAction(GEFActionConstants.ZOOM_OUT));
        toolBarManager.add(new ZoomComboContributionItem(getPage()));
    }

    @Override
    protected void buildActions() {
        final ZoomInRetargetAction zoomInAction = new ZoomInRetargetAction();
        zoomInAction.addPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent arg0) {
                updateToolbar();
            }
        });
        addRetargetAction(zoomInAction);
        ZoomOutRetargetAction zoomOutAction = new ZoomOutRetargetAction();
        zoomOutAction.addPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent arg0) {
                updateToolbar();
            }
        });
        addRetargetAction(zoomOutAction);
    }

    @Override
    public void contributeToMenu(IMenuManager menuManager) {
        super.contributeToMenu(menuManager);
        MenuManager viewMenu = new MenuManager(Messages.viewMenu);

        viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
        viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));

        menuManager.insertAfter(IWorkbenchActionConstants.M_EDIT,
            viewMenu);
    }

    @Override
    protected void declareGlobalActionKeys() {
        // currently none
    }

    private void updateToolbar() {
        if (getActionBars() instanceof IActionBars2) {
            IActionBars2 actionBars = (IActionBars2) getActionBars();
            actionBars.getCoolBarManager().update(true);
        }
    }

}
