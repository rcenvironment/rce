/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.start.gui.internal;

import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.PlatformUI;

/**
 * Utililty class to remove UI Elements from preferences dialog.
 * @author Hendrik Abbenhaus
 */
public final class EclipsePreferencesUIOrganizer {
    
    /**
     * Run/Debug.
     */
    private static final String RUNDEBUG = "org.eclipse.debug.ui.DebugPreferencePage";
    
    /**
     * Team.
     */
    private static final String TEAM = "org.eclipse.team.ui.TeamPreferences";
    
    /**
     * General.
     * Compare/Patch: org.eclipse.compare.internal.ComparePreferencePage 
     * Security: org.eclipse.equinox.security.ui.category //
     * Secure Storage: org.eclipse.equinox.security.ui.storage // 
     * Search: org.eclipse.search.preferences.SearchPreferencePage // 
     * Web Browser: org.eclipse.ui.browser.preferencePage // 
     * Editors: org.eclipse.ui.preferencePages.Editors // 
     * Text Editors: org.eclipse.ui.preferencePages.GeneralTextEditor // 
     * Annotations: org.eclipse.ui.editors.preferencePages.Annotations // 
     * Quick Diff: org.eclipse.ui.editors.preferencePages.QuickDiff // 
     * Accessibility: org.eclipse.ui.editors.preferencePages.Accessibility // 
     * Spelling: org.eclipse.ui.editors.preferencePages.Spelling //
     * Linked Mode: org.eclipse.ui.editors.preferencePages.LinkedModePreferencePage //
     * Hyperlinking: org.eclipse.ui.editors.preferencePages.HyperlinkDetectorsPreferencePage // 
     * File Associations: org.eclipse.ui.preferencePages.FileEditors // 
     * Structured Text Editors: org.eclipse.wst.sse.ui.preferences.editor // 
     * Task Tags: org.eclipse.wst.sse.ui.preferences.tasktags // 
     * Perspectives: org.eclipse.ui.preferencePages.Perspectives // 
     * Appearance: org.eclipse.ui.preferencePages.Views // 
     * Colors and Fonts: org.eclipse.ui.preferencePages.ColorsAndFonts // 
     * Label Decorations: org.eclipse.ui.preferencePages.Decorators // 
     * Workspace: org.eclipse.ui.preferencePages.Workspace // 
     * Build Order: org.eclipse.ui.preferencePages.BuildOrder // 
     * Local History: org.eclipse.ui.preferencePages.FileStates // 
     * Linked Resources: org.eclipse.ui.preferencePages.LinkedResources // 
     * Keys: org.eclipse.ui.preferencePages.Keys // 
     * Content Types: org.eclipse.ui.preferencePages.ContentTypes // 
     * Startup and Shutdown: org.eclipse.ui.preferencePages.Startup // 
     * Workspaces: org.eclipse.ui.preferencePages.Startup.Workspaces
     */
    private static final String GENERAL = "org.eclipse.ui.preferencePages.Workbench";
    
    private static final String GENERAL_WORKSPACE = "org.eclipse.ui.preferencePages.Workspace";
    
    private static final String GENERAL_WORKSPACE_BUILDORDER = "org.eclipse.ui.preferencePages.BuildOrder";
    
    private static final String GENERAL_WORKSPACE_LOCALHISTORY = "org.eclipse.ui.preferencePages.FileStates";
    
    /**
     * RCE.
     * Workflows: de.rcenvironment.core.gui.workflow.UpateWorkflowFileAutomaticallyPreferencesPage
     */
    //private static final String RCE = "de.rcenvironment.core";
    
    /**
     * Network Connections.
     * SSH2: "org.eclipse.jsch.ui.SSHPreferences"
     * Cache: "org.eclipse.wst.internet.cache.internal.preferences.CachePreferencePage"
     */
    private static final String GENERAL_NETWORKCONNECTIONS = "org.eclipse.ui.net.NetPreferences";
    
    /** NOT SHOWN PREFERENCES. **/
    private static final String[] PREF_PAGES = new String[] {
   
        
        //RCE,
        /**
         * Pdf4Eclipse
         */
        //"de.vonloesch.pdf4eclipse.preferences.MainPreferencePage",
        
        RUNDEBUG,
        /**
         * Install/Update
         * Available Software Sites: "org.eclipse.equinox.internal.p2.ui.sdk.SitesPreferencePage"
         * Automatic Updates: "org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatesPreferencePage"
         */
        //"org.eclipse.equinox.internal.p2.ui.sdk.ProvisioningPreferencePage",
        /**
         * Help
         * Content: "org.eclipse.help.ui.contentPreferencePage"
         */
        //"org.eclipse.help.ui.browsersPreferencePage",
        
        TEAM,
        
        //GENERAL,
        /**
         * Install/Update
         * Automatic Updates: "org.eclipse.update.scheduler.AutomaticUpdatesPreferencePage"
         */
        //"org.eclipse.update.internal.ui.preferences.MainPreferencePage",
        /**
         * Validation
         */
        //"ValidationPreferencePage",
        /**
         * XML.
         *  DTD Files: org.eclipse.wst.dtd.ui.preferences.dtd
         *      Templates: org.eclipse.wst.sse.ui.preferences.dtd.templates
         *      Syntax Coloring: org.eclipse.wst.sse.ui.preferences.dtd.styles
         *      Editor: org.eclipse.wst.dtd.ui.preferences.editor
         *  XML Files: org.eclipse.wst.xml.ui.preferences.xml.xml
         *      Editor: org.eclipse.wst.sse.ui.preferences.xml.source
         *           Content Assist: org.eclipse.wst.sse.ui.preferences.xml.contentassist
         *           Templates: org.eclipse.wst.sse.ui.preferences.xml.templates
         *           Syntax Coloring: org.eclipse.wst.sse.ui.preferences.xml.colors
         *           Typing: org.eclipse.wst.sse.ui.preferences.xml.typing
         *      Validation: org.eclipse.wst.sse.ui.preferences.xml.validation
         *  XML Catalog: org.eclipse.wst.xml.core.ui.XMLCatalogPreferencePage
         *  XML Schema Files: org.eclipse.wst.xsd.ui.internal.preferences.XSDPreferencePage
         *      Editor: org.eclipse.wst.xsd.ui.internal.preferences.XSDEditorPreferencePage
         *      Validation: org.eclipse.wst.xsd.ui.internal.preferences.XSDValidatorPreferencePage
         */
        //"org.eclipse.wst.xml.ui.preferences.xml"
    };
    
    private EclipsePreferencesUIOrganizer(){
        
    }
    
    /**
     * Removes the unwanted preference pages from Application.
     */
    public static void removeUnwantetPreferencePages() {

        PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();
        
        for (String pn : PREF_PAGES) {
            pm.remove(pn);
        }
        pm.find(GENERAL).findSubNode(GENERAL_WORKSPACE).remove(GENERAL_WORKSPACE_BUILDORDER);
        pm.find(GENERAL).findSubNode(GENERAL_WORKSPACE).remove(GENERAL_WORKSPACE_LOCALHISTORY);
        pm.find(GENERAL).remove(GENERAL_NETWORKCONNECTIONS);
    }
    


}
