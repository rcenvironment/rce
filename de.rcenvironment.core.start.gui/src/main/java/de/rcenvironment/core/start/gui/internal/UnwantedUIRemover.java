/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.dialogs.WorkbenchWizardElement;
import org.eclipse.ui.internal.wizards.AbstractExtensionWizardRegistry;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.ui.wizards.IWizardCategory;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.ui.wizards.IWizardRegistry;

/**
 * Utililty class to remove UI Elements (Perspectives, Views, ...) from environment. This class is entirely based on reflection accesses
 * because there is no public API available for the wizard registry.
 * 
 * @author Hendrik Abbenhaus
 * 
 */
// TODO this class references several classes from org.eclipse.ui.internal; review this while/after switching to e4
@SuppressWarnings("restriction")
public final class UnwantedUIRemover {

    /** NOT SHOWN PERSPECTIVES. **/
    private static final String[] PERSPECTIVES = new String[] {
        /**
         * Debug
         */
        "org.eclipse.debug.ui.DebugPerspective",
        /**
         * Team Synchronizing
         */
        "org.eclipse.team.ui.TeamSynchronizingPerspective",
        /**
         * Resource
         */
        "org.eclipse.ui.resourcePerspective",
        /**
         * XML
         */
        "org.eclipse.wst.xml.ui.perspective"
    };

    /** NOT SHOWN NEW WIZARDS. **/
    private static final String[] NEW_WIZARDS = new String[] {
        // Untitled Text File: Creates an empty untitled text file
        // "org.eclipse.ui.editors.wizards.UntitledTextFileWizard",

        // Project: Create a new project resource
        // "org.eclipse.ui.wizards.new.project",

        // Folder: Create a new folder resource
        // "org.eclipse.ui.wizards.new.folder",

        // File: Create a new file resource
        // "org.eclipse.ui.wizards.new.file",

        /**
         * Editing and validating XML files: Create a project containing XML sample files
         */
        "org.eclipse.wst.xml.ui.XMLExampleProjectCreationWizard",

        /**
         * DTD File: Create a new DTD file
         */
        "org.eclipse.wst.dtd.ui.internal.wizard.NewDTDWizard",

        /**
         * XML File: Create a new XML file
         */
        "org.eclipse.wst.xml.ui.internal.wizards.NewXMLWizard",

        /**
         * XML Schema File: Create a new XML schema file
         */
        "org.eclipse.wst.xsd.ui.internal.wizards.NewXSDWizard"

    };

    /** NOT SHOWN VIEWS. **/
    private static final String[] VIEWS = new String[] {
        /**
         * Breakpoints
         */
        "org.eclipse.debug.ui.BreakpointView",

        /**
         * Debug
         */
        "org.eclipse.debug.ui.DebugView",

        /**
         * Expressions
         */
        "org.eclipse.debug.ui.ExpressionView",

        /**
         * Memory
         */
        "org.eclipse.debug.ui.MemoryView",

        /**
         * Modules
         */
        "org.eclipse.debug.ui.ModuleView",

        /**
         * Registers
         */
        "org.eclipse.debug.ui.RegisterView",

        /**
         * Variables
         */
        "org.eclipse.debug.ui.VariableView",

        /**
         * Palette
         */
        "org.eclipse.gef.ui.palette_view",

        // Help:
        // "org.eclipse.help.ui.HelpView",

        // Error Log:
        // "org.eclipse.pde.runtime.LogView",

        // Classic Search:
        // "org.eclipse.search.SearchResultView",

        // Search:
        // "org.eclipse.search.ui.views.SearchView",

        /**
         * Synchronize
         */
        "org.eclipse.team.sync.views.SynchronizeView",
        /**
         * History
         */
        "org.eclipse.team.ui.GenericHistoryView",

        // Internal Web Browser
        // "org.eclipse.ui.browser.view",

        // Cheat Sheets:
        // "org.eclipse.ui.cheatsheets.views.CheatSheetView",

        /**
         * Console
         */
        "org.eclipse.ui.console.ConsoleView",

        // Welcome:
        // "org.eclipse.ui.internal.introview",

        // Project Explorer:
        // "org.eclipse.ui.navigator.ProjectExplorer",

        // Markers:
        // "org.eclipse.ui.views.AllMarkersView",

        /**
         * Bookmarks
         */
        "org.eclipse.ui.views.BookmarkView",

        // Outline:
        // "org.eclipse.ui.views.ContentOutline",

        // Problems:
        // "org.eclipse.ui.views.ProblemView",

        // Progress:
        // "org.eclipse.ui.views.ProgressView",

        // Properties:
        // "org.eclipse.ui.views.PropertySheet",

        // Navigator:
        // "org.eclipse.ui.views.ResourceNavigator",

        // Tasks:
        // "org.eclipse.ui.views.TaskList",

        // Snippets:
        // "org.eclipse.wst.common.snippets.internal.ui.SnippetsView",

        // Content Model:
        // "org.eclipse.wst.xml.ui.contentmodel.view",

        // Documentation:
        // "org.eclipse.wst.xml.ui.views.annotations.XMLAnnotationsView"

    };

    /** NOT SHOWN EXPORT WIZARDS. **/
    private static final String[] EXPORT_WIZARDS = new String[] {
        /**
         * Breakpoints, Export breakpoints to the local file system.
         */
        "org.eclipse.debug.internal.ui.importexport.breakpoints.WizardExportBreakpoints",

        /**
         * Launch Configurations, Export launch configurations to the local file system.
         */
        "org.eclipse.debug.ui.export.launchconfigurations",

        /**
         * Installed Software Items to File, Export the list of your installed software to the local file system.
         */
        "org.eclipse.equinox.p2.replication.export",

        /**
         * Team Project Set, Export a file containing the names and locations of the shared projects in the workspace.
         */
        "org.eclipse.team.ui.ProjectSetExportWizard",

        // Preferences, Export preferences to the local file system.
        // "org.eclipse.ui.wizards.export.Preferences",

        // File System, Export resources to the local file system.
        // "org.eclipse.ui.wizards.export.FileSystem",

        // Archive File, Export resources to an archive file on the local file system.
        // "org.eclipse.ui.wizards.export.ZipFile",

        /**
         * XML Catalog, Export an XML Catalog.
         */
        "org.eclipse.wst.xml.ui.internal.wizards.ExportXMLCatalogWizard",
    };

    /** NOT SHOWN IMPORT WIZARDS. **/
    private static final String[] IMPORT_WIZARDS = new String[] {
        /**
         * Breakpoints, Import breakpoints from the local file system.
         */
        "org.eclipse.debug.internal.ui.importexport.breakpoints.WizardImportBreakpoints",

        /**
         * Launch Configurations, Import launch configurations from the local file system.
         */
        "org.eclipse.debug.ui.import.launchconfigurations",

        /**
         * Install Software Items from File, Install a set of software from a description file.
         */
        "org.eclipse.equinox.p2.replication.import",

        /**
         * From Existing Installation, Install software from another product.
         */
        "org.eclipse.equinox.p2.replication.importfrominstallation",

        /**
         * Team Project Set Import a set of projects by fetching the project contents from the appropriate repositories described in a file.
         */
        "org.eclipse.team.ui.ProjectSetImportWizard",

        // Preferences, Import preferences from the local file system.
        // "org.eclipse.ui.wizards.import.Preferences",

        // File System, Import resources from the local file system into an existing project.
        // "org.eclipse.ui.wizards.import.FileSystem",

        // Existing Projects into Workspace, Create new projects from an archive file or directory.
        // "org.eclipse.ui.wizards.import.ExternalProject",

        // Archive File, Import resources from an archive file into an existing project.
        // "org.eclipse.ui.wizards.import.ZipFile",

        /**
         * XML Catalog, Import an existing XML Catalog.
         */
        "org.eclipse.wst.xml.ui.internal.wizards.ImportXMLCatalogWizard",
    };

    /** NOT SHOWN MENU ITEM PATHS. **/
    private static final String[] MENU_ITEM_PATHS = new String[] {

        /**
         * Window -> Editors
         */
        "window/org.eclipse.ui.editors"
    };

    private UnwantedUIRemover() {
        // prevent instantiation
    }

    /**
     * Removes the unwanted perspectives from Application.
     */
    public static void removeUnwantedPerspectives() {
        List<String> ignoredPerspectives = Arrays.asList(PERSPECTIVES);

        IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
        IPerspectiveDescriptor[] perspectiveDescriptors = perspectiveRegistry.getPerspectives();

        List<IPerspectiveDescriptor> removePerspectiveDesc = new ArrayList<>();

        // Add the perspective descriptors with the matching perspective ids to the list
        for (IPerspectiveDescriptor perspectiveDescriptor : perspectiveDescriptors) {
            if (ignoredPerspectives.contains(perspectiveDescriptor.getId())) {
                removePerspectiveDesc.add(perspectiveDescriptor);
            }
        }

        IExtensionChangeHandler extChgHandler = (IExtensionChangeHandler) perspectiveRegistry;
        extChgHandler.removeExtension(null, removePerspectiveDesc.toArray());

    }

    /**
     * Removes the unwanted views from Application.
     */
    public static void removeUnwantedViews() {
        List<String> ignoredViews = Arrays.asList(VIEWS);

        IViewRegistry viewRegistry = PlatformUI.getWorkbench().getViewRegistry();
        IViewDescriptor[] viewDescriptors = viewRegistry.getViews();

        List<IViewDescriptor> removeViewDesc = new ArrayList<>();

        // Add the view descriptors with the matching view ids to the list
        for (IViewDescriptor d : viewDescriptors) {
            if (ignoredViews.contains(d.getId())) {
                removeViewDesc.add(d);
            }
        }
        Platform.getExtensionRegistry().removeExtension(null, removeViewDesc.toArray());
    }

    /**
     * Removes the unwanted "New..." wizards from Application.
     */
    public static void removeUnwantedNewWizards() {
        List<String> ignoredWizards = Arrays.asList(NEW_WIZARDS);

        IWizardRegistry wizardRegistry = PlatformUI.getWorkbench().getNewWizardRegistry();
        IWizardDescriptor[] wizardDescriptors = getAllWizards(wizardRegistry.getRootCategory());

        AbstractExtensionWizardRegistry aewr = (AbstractExtensionWizardRegistry) WorkbenchPlugin.getDefault().getNewWizardRegistry();
        for (IWizardDescriptor d : wizardDescriptors) {
            WorkbenchWizardElement a = (WorkbenchWizardElement) d;
            if (ignoredWizards.contains(d.getId())) {
                aewr.removeExtension(a.getConfigurationElement().getDeclaringExtension(), new Object[] { d });
            }
        }

    }

    /**
     * Removes the unwanted "Import..." wizards from Application.
     */
    public static void removeUnwantedImportWizards() {
        List<String> ignoredWizards = Arrays.asList(IMPORT_WIZARDS);

        IWizardRegistry wizardRegistry = PlatformUI.getWorkbench().getImportWizardRegistry();
        IWizardDescriptor[] wizardDescriptors = getAllWizards(wizardRegistry.getRootCategory());

        AbstractExtensionWizardRegistry aewr = (AbstractExtensionWizardRegistry) WorkbenchPlugin.getDefault().getImportWizardRegistry();
        for (IWizardDescriptor d : wizardDescriptors) {
            WorkbenchWizardElement a = (WorkbenchWizardElement) d;
            if (ignoredWizards.contains(d.getId())) {
                aewr.removeExtension(a.getConfigurationElement().getDeclaringExtension(), new Object[] { d });
            }
        }
    }

    /**
     * Removes the unwanted "Export..." wizards from Application.
     */
    public static void removeUnwantedExportWizards() {
        List<String> ignoredWizards = Arrays.asList(EXPORT_WIZARDS);

        IWizardRegistry wizardRegistry = PlatformUI.getWorkbench().getExportWizardRegistry();
        IWizardDescriptor[] wizardDescriptors = getAllWizards(wizardRegistry.getRootCategory());

        AbstractExtensionWizardRegistry aewr = (AbstractExtensionWizardRegistry) WorkbenchPlugin.getDefault().getExportWizardRegistry();
        for (IWizardDescriptor d : wizardDescriptors) {
            WorkbenchWizardElement a = (WorkbenchWizardElement) d;
            if (ignoredWizards.contains(d.getId())) {
                aewr.removeExtension(a.getConfigurationElement().getDeclaringExtension(), new Object[] { d });
            }
        }
    }

    /**
     * Removes the unwanted menu entries defined by their path from the Application.
     */
    public static void removeUnwantedMenuEntries() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IMenuManager menubarManager = ((WorkbenchWindow) workbenchWindow).getMenuBarManager();
        for (String menuItemPath : MENU_ITEM_PATHS) {
            IContributionItem contributionItem = menubarManager.findUsingPath(menuItemPath);
            if (contributionItem != null) {
                contributionItem.setVisible(false);
            } 
        }
    }

    private static IWizardDescriptor[] getAllWizards(IWizardCategory... categories) {
        List<IWizardDescriptor> results = new ArrayList<>();
        for (IWizardCategory wizardCategory : categories) {
            results.addAll(Arrays.asList(wizardCategory.getWizards()));
            results.addAll(Arrays.asList(getAllWizards(wizardCategory.getCategories())));
        }
        return results.toArray(new IWizardDescriptor[0]);
    }

}
