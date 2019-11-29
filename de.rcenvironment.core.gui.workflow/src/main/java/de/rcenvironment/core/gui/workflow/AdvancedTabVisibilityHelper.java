/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow;


/**
 * Helper class that stores advanced tab visibility state.
 *
 * @author Oliver Seebach
 */
public final class AdvancedTabVisibilityHelper {

    private static boolean showAdvancedTab = false;
    
    private AdvancedTabVisibilityHelper() {
    }
    
    public static boolean isShowAdvancedTab() {
        return showAdvancedTab;
    }

    
    public static void setShowAdvancedTab(boolean showAdvancedTab) {
        AdvancedTabVisibilityHelper.showAdvancedTab = showAdvancedTab;
    }

    
}
