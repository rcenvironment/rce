/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui;

import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Encapsulates the data stored for the custom workspace chooser dialog.
 * 
 * @author Robert Mischke
 */
public final class WorkspaceSettings {

    private static final String PERSISTENT_SETTINGS_KEY_LAST_WORKSPACE = "rce.workspace.lastLocation";

    private static final String PERSISTENT_SETTINGS_KEY_RECENT_WORKSPACES = "rce.workspace.recentLocations";

    private static final String PERSISTENT_SETTINGS_KEY_DONT_ASK_AGAIN = "rce.workspace.dontAskAgain";

    private static final String DONT_ASK_AGAIN_TRUE_VALUE = "true";

    private static final WorkspaceSettings INSTANCE = new WorkspaceSettings();

    private PersistentSettingsService pss;

    private WorkspaceSettings() {
        pss = ServiceRegistry.createAccessFor(this).getService(PersistentSettingsService.class);
    }

    public static WorkspaceSettings getInstance() {
        return INSTANCE;
    }

    public boolean getDontAskAgainSetting() {
        return DONT_ASK_AGAIN_TRUE_VALUE.equals(pss.readStringValue(PERSISTENT_SETTINGS_KEY_DONT_ASK_AGAIN));
    }

    /**
     * Sets the new value for the setting to suppress the workspace chooser dialog and use the last location.
     * 
     * @param dontAskValue the new value
     */
    public void setDontAskAgainSetting(boolean dontAskValue) {
        if (dontAskValue) {
            pss.saveStringValue(PERSISTENT_SETTINGS_KEY_DONT_ASK_AGAIN, DONT_ASK_AGAIN_TRUE_VALUE);
        } else {
            pss.delete(PERSISTENT_SETTINGS_KEY_DONT_ASK_AGAIN);
        }
    }

    public String getLastLocation() {
        return pss.readStringValue(PERSISTENT_SETTINGS_KEY_LAST_WORKSPACE);
    }

    String getRecentLocationData() {
        return pss.readStringValue(PERSISTENT_SETTINGS_KEY_RECENT_WORKSPACES);
    }

    void updateLocationHistory(String currentWorkspace, String recentLocationData) {
        pss.saveStringValue(PERSISTENT_SETTINGS_KEY_LAST_WORKSPACE, currentWorkspace);
        pss.saveStringValue(PERSISTENT_SETTINGS_KEY_RECENT_WORKSPACES, recentLocationData);
    }

}
