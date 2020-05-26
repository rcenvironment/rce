/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.start.gui.internal;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.NewWizardMenu;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.ide.IIDEActionConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.actions.CommandAction;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.start.Application;

/**
 * This class advises the creation of the action bar of the {@link Application}.
 * 
 * @author Christian Weiss
 * @author Riccardo Dusi
 */
@SuppressWarnings("restriction")
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

    private static final Log LOGGER = LogFactory.getLog(ApplicationActionBarAdvisor.class);

    /** File -> New wizard menu. */
    private NewWizardMenu newWizardMenu;
    /** File -> Close action. */
    private IWorkbenchAction closeAction;
    /** File -> Close All action. */
    private IWorkbenchAction closeAllAction;
    /** File -> Save action. */
    private IWorkbenchAction saveAction;
    /** File -> Save As action. */
    private IWorkbenchAction saveAsAction;
    /** File -> Save All action. */
    private IWorkbenchAction saveAllAction;
    /** File -> Rename action. */
//    private IWorkbenchAction renameAction;
    /** File -> Refresh action. */
    private IWorkbenchAction refreshAction;
    /** File -> Print action. */
    private IWorkbenchAction printAction;
    /** File -> Switch Workspace. */
//    private IWorkbenchAction switchWorkspaceAction;
    /** File -> Restart. */
    private CommandAction restartAction;
    /** File -> Import action. */
    private IWorkbenchAction importAction;
    /** File -> Export action. */
    private IWorkbenchAction exportAction;
    /** File -> Properties action. */
    private IWorkbenchAction propertiesAction;
    /** File -> Exit action. */
    private IWorkbenchAction exitAction;

    /** Edit -> Undo action. */
    private IWorkbenchAction undoAction;
    /** Edit -> Redo action. */
    private IWorkbenchAction redoAction;
    /** Edit -> Cut action. */
    private IWorkbenchAction cutAction;
    /** Edit -> Copy action. */
    private IWorkbenchAction copyAction;
    /** Edit -> Paste action. */
    private IWorkbenchAction pasteAction;
    /** Edit -> Delete action. */
    private IWorkbenchAction deleteAction;
    /** Edit -> Select All action. */
    private IWorkbenchAction selectAllAction;
    /** Edit -> Find/Replace action. */
    private IWorkbenchAction findReplaceAction;

    /** Windows -> New Window action. */
    private IWorkbenchAction newWorkbenchWindowAction;
    /** Windows -> New Editor action. */
    private IWorkbenchAction newEditorWindowAction;
    /** Windows -> Customize Perspective... action. */
    private IWorkbenchAction editActionSetAction;
    /** Windows -> Save Perspective As... action. */
    private IWorkbenchAction savePerspectiveAction;
    /** Windows -> Reset Perspective... action. */
    private IWorkbenchAction resetPerspectiveAction;
    /** Windows -> Close Perspective action. */
    private IWorkbenchAction closePerspectiveAction;
    /** Windows -> Close All Perspectives action. */
    private IWorkbenchAction closeAllPerspectivesAction;
    /** Windows -> Preferences action. */
    private IWorkbenchAction preferenceAction;
    /** Action which opens the system menu. */
    private IWorkbenchAction showPartPaneMenuAction;
    /** Action which opens the view's menu. */
    private IWorkbenchAction showViewMenuAction;
    /** Action which maximizes the current view part. */
    private IWorkbenchAction maximizePartAction;
    /** Action which minimizes the current view part. */
    private IWorkbenchAction minimizePartAction;
    /** Action which activates the top-level editor. */
    private IWorkbenchAction activateEditorAction;
    /** Action which activates the next editor. */
    private IWorkbenchAction nextEditorAction;
    /** Action which activates the previous editor. */
    private IWorkbenchAction prevEditorAction;
    /** Action which switches to an open editor. */
    private IWorkbenchAction switchToEditorAction;
    /** Action which opens the quick switch menu. */
    private IWorkbenchAction openEditorDropDownAction;
    /** Action which activates the next part in a perspective. */
    private IWorkbenchAction nextPartAction;
    /** Action which activates the previous part in a perspective. */
    private IWorkbenchAction prevPartAction;
    /** Action which switches to the next perspective. */
    private IWorkbenchAction nextPerspectiveAction;
    /** Action which switches to the previous perspective. */
    private IWorkbenchAction prevPerspectiveAction;

    /** Help -> Help Contents action. */
    private IWorkbenchAction helpAction;
    /** Help -> Dynamic Help action. */
    private IWorkbenchAction dynamicHelpAction;
    /** Help -> About action. */
    private IWorkbenchAction aboutAction;

    /** New wizard drop down action. */
    private IWorkbenchAction newWizardDropDownAction;
    private IWorkbenchWindow workbenchWindow;

    private IWorkbenchAction introAction;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
    }

    @Override
    protected void makeActions(IWorkbenchWindow window) {
        this.workbenchWindow = window;
        
        newWizardMenu = new NewWizardMenu(window);
        closeAction = ActionFactory.CLOSE.create(window);
        register(closeAction);
        closeAllAction = ActionFactory.CLOSE_ALL.create(window);
        register(closeAllAction);
        saveAction = ActionFactory.SAVE.create(window);
        register(saveAction);
        saveAsAction = ActionFactory.SAVE_AS.create(window);
        register(saveAsAction);
        saveAllAction = ActionFactory.SAVE_ALL.create(window);
        register(saveAllAction);
//        renameAction = ActionFactory.RENAME.create(window);
//        register(renameAction);
        refreshAction = ActionFactory.REFRESH.create(window);
        register(refreshAction);
        printAction = ActionFactory.PRINT.create(window);
        register(printAction);
//        switchWorkspaceAction = IDEActionFactory.OPEN_WORKSPACE.create(window);
//        register(switchWorkspaceAction);
        restartAction = new CommandAction(window, IWorkbenchCommandConstants.FILE_RESTART);
        restartAction.setId("restart");
        restartAction.setText("Restart");
        register(restartAction);
        importAction = ActionFactory.IMPORT.create(window);
        register(importAction);
        exportAction = ActionFactory.EXPORT.create(window);
        register(exportAction);
        propertiesAction = ActionFactory.PROPERTIES.create(window);
        register(propertiesAction);
        exitAction = ActionFactory.QUIT.create(window);
        register(exitAction);
        
        // Edit
        undoAction = ActionFactory.UNDO.create(window);
        register(undoAction);
        redoAction = ActionFactory.REDO.create(window);
        register(redoAction);
        cutAction = ActionFactory.CUT.create(window);
        register(cutAction);
        copyAction = ActionFactory.COPY.create(window);
        register(copyAction);
        pasteAction = ActionFactory.PASTE.create(window);
        register(pasteAction);
        deleteAction = ActionFactory.DELETE.create(window);
        deleteAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.DELETE_16));
        register(deleteAction);
        selectAllAction = ActionFactory.SELECT_ALL.create(window);
        register(selectAllAction);
        findReplaceAction = ActionFactory.FIND.create(window);
        register(findReplaceAction);

        // Windows menu
        newWorkbenchWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        newWorkbenchWindowAction.setText("&New Window");
        register(newWorkbenchWindowAction);
        newEditorWindowAction = ActionFactory.NEW_EDITOR.create(window);
        register(newEditorWindowAction);
        editActionSetAction = ActionFactory.EDIT_ACTION_SETS.create(window);
        register(editActionSetAction);
        savePerspectiveAction = ActionFactory.SAVE_PERSPECTIVE.create(window);
        register(savePerspectiveAction);
        resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
        register(resetPerspectiveAction);
        closePerspectiveAction = ActionFactory.CLOSE_PERSPECTIVE.create(window);
        register(closePerspectiveAction);
        closeAllPerspectivesAction = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window);
        register(closeAllPerspectivesAction);
        showPartPaneMenuAction = ActionFactory.SHOW_PART_PANE_MENU.create(window);
        register(showPartPaneMenuAction);
        showViewMenuAction = ActionFactory.SHOW_VIEW_MENU.create(window);
        register(showViewMenuAction);
        maximizePartAction = ActionFactory.MAXIMIZE.create(window);
        register(maximizePartAction);
        minimizePartAction = ActionFactory.MINIMIZE.create(window);
        register(minimizePartAction);
        activateEditorAction = ActionFactory.ACTIVATE_EDITOR.create(window);
        register(activateEditorAction);
        nextEditorAction = ActionFactory.NEXT_EDITOR.create(window);
        register(nextEditorAction);
        prevEditorAction = ActionFactory.PREVIOUS_EDITOR.create(window);
        register(prevEditorAction);
        ActionFactory.linkCycleActionPair(nextEditorAction, prevEditorAction);
        switchToEditorAction = ActionFactory.SHOW_OPEN_EDITORS.create(window);
        register(switchToEditorAction);
        openEditorDropDownAction = ActionFactory.SHOW_WORKBOOK_EDITORS.create(window);
        register(openEditorDropDownAction);
        nextPartAction = ActionFactory.NEXT_PART.create(window);
        register(nextPartAction);
        prevPartAction = ActionFactory.PREVIOUS_PART.create(window);
        register(prevPartAction);
        ActionFactory.linkCycleActionPair(nextPartAction, prevPartAction);
        nextPerspectiveAction = ActionFactory.NEXT_PERSPECTIVE.create(window);
        register(nextPerspectiveAction);
        prevPerspectiveAction = ActionFactory.PREVIOUS_PERSPECTIVE.create(window);
        register(prevPerspectiveAction);
        ActionFactory.linkCycleActionPair(nextPerspectiveAction, prevPerspectiveAction);
        preferenceAction = ActionFactory.PREFERENCES.create(window);
        register(preferenceAction);

        // Help
        helpAction = ActionFactory.HELP_CONTENTS.create(window);
        register(helpAction);
        dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window);
        register(dynamicHelpAction);
        
        aboutAction = ActionFactory.ABOUT.create(window);
        aboutAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.RCE_LOGO_16));
        aboutAction.setDisabledImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.RCE_LOGO_16));
        aboutAction.setHoverImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.RCE_LOGO_16));
        register(aboutAction);

        // Welcome Screen
        introAction = ActionFactory.INTRO.create(window);
        register(introAction);
        
        // Cool bar actions
        newWizardDropDownAction = IDEActionFactory.NEW_WIZARD_DROP_DOWN.create(window);
        register(newWizardDropDownAction);

        // remove unwanted stuff (foreign contributions) from the GUI
        removeUnwanted();
    }

    private void removeUnwanted() {
        final Properties unwanted = new Properties();
        try {
            unwanted.load(ApplicationActionBarAdvisor.class.getResourceAsStream("unwanted.properties"));
        } catch (IOException e) {
            LOGGER.error("Failed to remove unwanted elements from UI:", e);
            return;
        }
        // remove unwanted action sets
        final String actionSetsValue = unwanted.getProperty("org.eclipse.ui.actionSets");
        if (actionSetsValue != null && !actionSetsValue.trim().isEmpty()) {
            final String[] actionSets = actionSetsValue.split(",");
            ActionSetRegistry actionSetRegistry = WorkbenchPlugin.getDefault().getActionSetRegistry();
            for (final String actionSetId : actionSets) {
                IActionSetDescriptor actionSetDescriptor = actionSetRegistry.findActionSet(actionSetId.trim());
                if (actionSetDescriptor != null) {
                    IExtension ext = actionSetDescriptor.getConfigurationElement().getDeclaringExtension();
                    actionSetRegistry.removeExtension(ext, new Object[] { actionSetDescriptor });
                }
            }
        }
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar) {

        // File -> New submenu
        final String newId = ActionFactory.NEW.getId();
        MenuManager newMenu = new MenuManager("New", newId);
        newMenu.add(new Separator(newId));
        newMenu.add(newWizardMenu);
        newMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        // File
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        fileMenu.add(newMenu);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
        fileMenu.add(new Separator());
        fileMenu.add(closeAction);
        fileMenu.add(closeAllAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
        fileMenu.add(new Separator());
        fileMenu.add(saveAction);
        fileMenu.add(saveAsAction);
        fileMenu.add(saveAllAction);
        fileMenu.add(new Separator());
        fileMenu.add(refreshAction);
        fileMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        fileMenu.add(exitAction);
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        fileMenu.add(ContributionItemFactory.REOPEN_EDITORS.create(workbenchWindow));
        // add to main menu
        menuBar.add(fileMenu);
        
        // Edit
        MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
        editMenu.add(undoAction);
        editMenu.add(redoAction);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
        editMenu.add(new Separator());
        editMenu.add(cutAction);
        editMenu.add(copyAction);
        editMenu.add(pasteAction);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.CUT_EXT));
        editMenu.add(new Separator());
        editMenu.add(deleteAction);
        editMenu.add(new Separator());
        editMenu.add(selectAllAction);
        editMenu.add(new Separator());
        editMenu.add(findReplaceAction);
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT));
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
        // add to main menu
        menuBar.add(editMenu);

        // Additions
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.M_NAVIGATE));
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        
        // Window menu
        MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
        windowMenu.add(newWorkbenchWindowAction);
        windowMenu.add(newEditorWindowAction);
        windowMenu.add(new Separator());
        addOpenPerspectiveAction(windowMenu);
        addShowViewAction(windowMenu);
        windowMenu.add(new Separator());
        windowMenu.add(editActionSetAction);
        windowMenu.add(savePerspectiveAction);
        windowMenu.add(resetPerspectiveAction);
        windowMenu.add(closePerspectiveAction);
        windowMenu.add(closeAllPerspectivesAction);
        windowMenu.add(new Separator());
        addKeyboardShortcuts(windowMenu);
        windowMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        windowMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "end")); //$NON-NLS-1$
        windowMenu.add(preferenceAction);
        windowMenu.add(ContributionItemFactory.OPEN_WINDOWS.create(workbenchWindow));
        // add to main menu
        menuBar.add(windowMenu);
        
        // Help
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
        helpMenu.add(introAction);
        helpMenu.add(new Separator());
        helpMenu.add(helpAction);
        helpMenu.add(dynamicHelpAction);
        helpMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        helpMenu.add(new Separator());
        helpMenu.add(aboutAction);
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
        
        // add to main menu
        menuBar.add(helpMenu);
    }
    
    @Override
    protected void fillCoolBar(ICoolBarManager coolBar) {
        // File Group
        IToolBarManager fileToolBar = new ToolBarManager();
        fileToolBar.add(newWizardDropDownAction);
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.SAVE_GROUP));
        fileToolBar.add(saveAction);
        fileToolBar.add(saveAllAction);
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.PRINT_EXT));

        // Add to the cool bar manager
        coolBar.setLockLayout(false);
        coolBar.add(new GroupMarker(IIDEActionConstants.GROUP_FILE));
        coolBar.add(new ToolBarContributionItem(fileToolBar, IWorkbenchActionConstants.TOOLBAR_FILE));
        coolBar.add(new GroupMarker("de.rcenvironment.configuration.toolbar"));
        coolBar.add(new GroupMarker("de.rcenvironment.toolintegration.toolbar"));
        coolBar.add(new GroupMarker("de.rcenvironment.workflow.toolbar"));
        coolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_EDITOR));
        coolBar.add(new GroupMarker(IWorkbenchActionConstants.M_NAVIGATE));
        coolBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    };
    
    /**
     * Adds the "Show View" action.
     * 
     * @param parentMenu
     *            the parent menu.
     */
    private void addShowViewAction(MenuManager parentMenu) {
        final MenuManager subMenu = new MenuManager("&Show View", "showView"); //$NON-NLS-1$
        subMenu.add(ContributionItemFactory.VIEWS_SHORTLIST.create(workbenchWindow));
        parentMenu.add(subMenu);
    }

    /**
    * Adds the "Open Perspective" action.
    * 
    * @param parentMenu
    *            the parent menu.
    */
    private void addOpenPerspectiveAction(MenuManager parentMenu) {
        final MenuManager subMenu = new MenuManager("&Open Perspective", "openPerspective"); //$NON-NLS-1$
        subMenu.add(ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(workbenchWindow));
        parentMenu.add(subMenu);
    }
    
    /**
     * Adds the keyboard navigation submenu to the specified menu.
     * 
     * @param parentMenu
     *            the parent menu.
     */
    private void addKeyboardShortcuts(MenuManager parentMenu) {
        final MenuManager subMenu = new MenuManager("&Shortcuts", "shortcuts"); //$NON-NLS-1$
        parentMenu.add(subMenu);
        subMenu.add(showPartPaneMenuAction);
        subMenu.add(showViewMenuAction);
        subMenu.add(new Separator());
        subMenu.add(maximizePartAction);
        subMenu.add(minimizePartAction);
        subMenu.add(new Separator());
        subMenu.add(activateEditorAction);
        subMenu.add(nextEditorAction);
        subMenu.add(prevEditorAction);
        subMenu.add(switchToEditorAction);
        subMenu.add(openEditorDropDownAction);
        subMenu.add(new Separator());
        subMenu.add(nextPartAction);
        subMenu.add(prevPartAction);
        subMenu.add(new Separator());
        subMenu.add(nextPerspectiveAction);
        subMenu.add(prevPerspectiveAction);
    }
}
