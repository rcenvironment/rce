/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Utililty class to remove items from context menus. As context menus are created at runtime, it is not possible to remove the items in
 * advanced at a centralized location.
 * 
 * @author Oliver Seebach
 */
public final class ContextMenuItemRemover {

    private static List<String> itemsToRemove;

    private ContextMenuItemRemover() {

    }

    /**
     * Removes unwanted context menu entries from the given control.
     * 
     * @param control The control where the context menu should be edited.
     */
    public static void removeUnwantedMenuEntries(Control control) {

        final Properties unwanted = new Properties();
        try {
            unwanted.load(ContextMenuItemRemover.class.getResourceAsStream("unwanted.properties"));
        } catch (IOException e) {
            return;
        }
        String unwantedEntriesProperty = unwanted.getProperty("unwantedContextMenuEntries");
        itemsToRemove = new ArrayList<String>(Arrays.asList(unwantedEntriesProperty.split(",")));

        if (!control.isDisposed() && control.getMenu() != null) {
            // prevent multiple listener registration
            if (control.getData("MenuDetectToken") == null) {
                control.setData("MenuDetectToken", true);
                control.addMenuDetectListener(new RemoveItemsMenuDetectListener());
            }
        }

    }

    /**
     * Menu listener to remove unwanted items. Items are removed if they are in the list of unwanted items or if they are a separator and
     * preceeded by a separator (which can occur when other items are removed).
     * 
     * @author Oliver Seebach
     */
    private static class RemoveItemsMenuListener implements MenuListener {

        private final Menu menu;

        /**
         * Submenu listener to remove unwanted items. Items are removed if they are in the list of unwanted items or if they are a separator
         * and preceeded by a separator (which can occur when other items are removed).
         * 
         * @author Oliver Seebach
         */
        private final class CustomSubMenuListener implements MenuListener {

            @Override
            public void menuShown(MenuEvent event) {
                MenuItem preceedingSubMenuItem = null;
                final Menu subMenu = ((Menu) event.widget);
                for (MenuItem subMenuItem : subMenu.getItems()) {
                    if (preceedingSubMenuItem != null && preceedingSubMenuItem.isDisposed()) {
                        preceedingSubMenuItem = null;
                    }
                    if (itemsToRemove.contains(subMenuItem.getText())
                        || (preceedingSubMenuItem != null
                            && preceedingSubMenuItem.getText().isEmpty()
                            && subMenuItem.getText().isEmpty())) {
                        subMenuItem.dispose();
                    } else {
                        preceedingSubMenuItem = subMenuItem;
                    }
                }
            }

            @Override
            public void menuHidden(MenuEvent arg0) {}
        }

        RemoveItemsMenuListener(Menu menu) {
            this.menu = menu;
        }

        @Override
        public void menuShown(MenuEvent menuEvent) {

            MenuItem preceedingMainMenuItem = null;

            for (MenuItem mainMenuItem : menu.getItems()) {
                MenuItem itemToHandle = null;
                if (!mainMenuItem.isDisposed()) {
                    itemToHandle = mainMenuItem;
                    if (itemsToRemove.contains(mainMenuItem.getText())
                        || (preceedingMainMenuItem != null
                            && preceedingMainMenuItem.getText().isEmpty()
                            && itemToHandle.getText().isEmpty())) {
                        itemToHandle.dispose();
                    } else {
                        preceedingMainMenuItem = itemToHandle;
                    }
                    // handle submenu
                    if (!itemToHandle.isDisposed() && itemToHandle.getMenu() != null) {
                        // prevent multiple listener registration
                        if (itemToHandle.getMenu().getData("SubMenuListenerToken") == null) {
                            itemToHandle.getMenu().setData("SubMenuListenerToken", true);
                            itemToHandle.getMenu().addMenuListener(new CustomSubMenuListener());
                        }
                    }
                }
            }
        }

        @Override
        public void menuHidden(MenuEvent arg0) {}
    }

    /**
     * Menu detect listener to remove unwanted items.
     * 
     * @author Oliver Seebach
     */
    private static class RemoveItemsMenuDetectListener implements MenuDetectListener {

        @Override
        public void menuDetected(MenuDetectEvent event) {
            final Menu menu = ((Control) event.widget).getMenu();
            if (!menu.isDisposed() && menu != null) {
                // prevent multiple listener registration
                if (menu.getData("MenuListenerToken") == null) {
                    menu.setData("MenuListenerToken", true);
                    menu.addMenuListener(new RemoveItemsMenuListener(menu));
                }
            }
        }
    }

}
