/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.Border;
import com.googlecode.lanterna.gui.DefaultBackgroundRenderer;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Interactable;
import com.googlecode.lanterna.gui.Theme;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.component.ActionListBox;
import com.googlecode.lanterna.gui.component.Label;
import com.googlecode.lanterna.gui.component.Panel;
import com.googlecode.lanterna.gui.component.RadioCheckBoxList;
import com.googlecode.lanterna.gui.dialog.ListSelectDialog;
import com.googlecode.lanterna.gui.layout.LayoutParameter;
import com.googlecode.lanterna.gui.listener.WindowAdapter;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalSize;

import de.rcenvironment.core.configuration.bootstrap.profile.CommonProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.CommonProfileUtils;
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileUtils;
import de.rcenvironment.core.configuration.ui.LanternaUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * UI that allows the user to select a profile to start from and to select a default profile.
 *
 * @author Tobias Brieden
 */
public class ProfileSelectionUI {

    /**
     * This field exists to enforce a dependency to the LayoutParameter class. Otherwise, the associated bundle import might get removed,
     * which would result in a NoClassDefFound error at runtime.
     */
    private static LayoutParameter lp;

    private static final String OPTION_SELECT_PROFILE = "Select a profile and start RCE.";

    private static final String OPTION_SELECT_DEFAULT_PROFILE = "Select the default profile for future runs.";

    private static final String BACKGROUND_MESSAGE = "Profile Selection";

    private static final int PROFILE_LIST_WIDTH = 60;

    private static final String DEFAULT_WARNING_TEXT =
        "Note: If you select a default profile here, RCE will use this profile on the next start (or restart), if you do not use the "
            + "profile option (\"-p\"). This is also the case if you have configured RCE as a service.";

    private GUIScreen guiScreen;

    private Profile selectedProfile = null;

    // TODO it should be possible to combine this dialog with the configuration ui dialog. There should be a modular way to do that.

    private Terminal terminal;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * This constructor can be used to inject a dummy terminal for automated testing.
     * 
     * @param terminal
     */
    public ProfileSelectionUI(Terminal terminal) {
        this.terminal = terminal;
    }

    public ProfileSelectionUI() {}

    /**
     * Main method of the interactive configuration shell.
     * 
     * @return Returns the selected Profile or null if no profile was selected in this dialog.
     * @throws ProfileException Thrown if there is a problem with the profiles parent directory.
     */
    public Profile run() throws ProfileException {

        if (terminal != null) {
            guiScreen = TerminalFacade.createGUIScreen(terminal);
        } else {
            guiScreen = TerminalFacade.createGUIScreen();
        }

        if (guiScreen == null) {
            log.error("Failed to initialize text-mode UI; terminating");
            return null;
        }
        guiScreen.setBackgroundRenderer(new DefaultBackgroundRenderer(BACKGROUND_MESSAGE));
        guiScreen.getScreen().startScreen();

        runMainLoop(); // TODO catch RuntimeExceptions as it is done in ConfigurationTextUI?

        guiScreen.getScreen().stopScreen();

        return this.selectedProfile;
    }

    private void runMainLoop() throws ProfileException {
        while (true) {
            String action = showMainMenu();
            if (action == null) {
                return; // exit
            }
            switch (action) {
            case OPTION_SELECT_PROFILE:

                try {
                    File profilesParentDirectory = ProfileUtils.getProfilesParentDirectory();
                    guiScreen.showWindow(new SelectProfileWindow(profilesParentDirectory), GUIScreen.Position.CENTER);

                    // check if a profile was selected
                    if (this.selectedProfile != null) {
                        return; // exit
                    }

                } catch (ProfileException e) {
                    LanternaUtils.showErrorMessageBox(guiScreen,
                        "The profiles parent directory cannot be created or it is not a directory.");
                }

                break;
            case OPTION_SELECT_DEFAULT_PROFILE:

                guiScreen.showWindow(new SelectDefaultProfileWindow(), GUIScreen.Position.CENTER);

                break;
            default:
                log.error(StringUtils.format("Invalid action: %s", action));
            }
        }
    }

    /**
     * Window to select the profile which should be used to start RCE with right now.
     * 
     * @author Tobias Brieden
     */
    private class SelectProfileWindow extends Window {

        SelectProfileWindow(File profileParentDirectory) {
            super(OPTION_SELECT_PROFILE);

            // default profile
            Panel defaultProfilePanel = new Panel("Start RCE with the default profile");
            defaultProfilePanel.setBorder(new Border.Standard());
            ActionListBox defaultProfileListBox = new ActionListBox();

            try {
                final File defaultProfilePath = ProfileUtils.getDefaultProfilePath();
                Action startWithDefaultProfileAction = new Action() {

                    @Override
                    public void doAction() {
                        try {
                            selectedProfile = new Profile(defaultProfilePath);
                            SelectProfileWindow.this.close();
                        } catch (ProfileException e) {
                            LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
                        }
                    }
                };
                defaultProfileListBox.addAction(defaultProfilePath.getAbsolutePath(), startWithDefaultProfileAction);
                defaultProfilePanel.addComponent(defaultProfileListBox);
            } catch (ProfileException e) {
                defaultProfilePanel.addComponent(new Label("Unable to determine the default profile.", PROFILE_LIST_WIDTH));
            }

            addComponent(defaultProfilePanel);

            // recent profiles
            Panel recentProfilePanel = new Panel();
            recentProfilePanel.setTitle("Recently used profiles");
            recentProfilePanel.setBorder(new Border.Standard());

            ActionListBox recentProfileListBox = new ActionListBox();
            List<Profile> recentlyUsedProfiles = null;

            try {
                recentlyUsedProfiles = CommonProfileUtils.getRecentlyUsedProfiles();
            } catch (CommonProfileException e) {
                // catch ...
            }

            // ... check if the previous call was successful
            if (recentlyUsedProfiles == null) {
                recentProfilePanel.addComponent(new Label("Unable to load most recently used profiles.", PROFILE_LIST_WIDTH));
            } else {
                for (final Profile profile : recentlyUsedProfiles) {

                    Action startWithProfileAction = new Action() {

                        @Override
                        public void doAction() {
                            selectedProfile = profile;
                            SelectProfileWindow.this.close();
                        }
                    };

                    recentProfileListBox.addAction(profile.getProfileDirectory().getAbsolutePath(), startWithProfileAction);
                }
                recentProfilePanel.addComponent(recentProfileListBox);
            }

            addComponent(recentProfilePanel);

            // all profiles in the profiles home
            String allProfilesPanelLabel = StringUtils.format("All available profiles in %s", profileParentDirectory.getAbsolutePath());
            Panel allProfilesPanel = new Panel(allProfilesPanelLabel);
            allProfilesPanel.setBorder(new Border.Standard());
            ActionListBox allProfilesListBox = new ActionListBox();

            for (final Profile profile : ProfileUtils.listProfiles(profileParentDirectory)) {

                Action startWithProfileAction = new Action() {

                    @Override
                    public void doAction() {
                        selectedProfile = profile;
                        SelectProfileWindow.this.close();
                    }
                };

                allProfilesListBox.addAction(profile.getName(), startWithProfileAction);
            }

            allProfilesPanel.addComponent(allProfilesListBox);
            addComponent(allProfilesPanel);

            addWindowListener(new WindowAdapter() {

                @Override
                public void onUnhandledKeyboardInteraction(Window arg0, Key key) {
                    if (key.getKind() == Key.Kind.Escape) {
                        SelectProfileWindow.this.close();
                        return;
                    }
                }

            });

            // if one of the panels uses more space than preferred, because its content is bigger, make sure that the other panels have the
            // same width
            int w1 = defaultProfilePanel.getPreferredSize().getColumns();
            int w2 = recentProfilePanel.getPreferredSize().getColumns();
            int w3 = allProfilesPanel.getPreferredSize().getColumns();
            int width = Math.max(Math.max(PROFILE_LIST_WIDTH, w1), Math.max(w2, w3)); // TODO subtract panel border?
            // the panel border will take up two additional rows
            defaultProfilePanel.setPreferredSize(new TerminalSize(width, 1 + 2));
            recentProfilePanel.setPreferredSize(new TerminalSize(width, 5 + 2));
            allProfilesPanel.setPreferredSize(new TerminalSize(width, 5 + 2));

        }

    }

    /**
     * This RadioCheckBoxList allows to toggle the selection of a previously selected entry by checking it again.
     *
     * @author Tobias Brieden
     */
    public class DeselectableRadioCheckBoxList extends RadioCheckBoxList {

        private static final int UNSELECTED = -1;

        // Override this method to make the background of the List grey (as opposed to blue)
        @Override
        protected Theme.Definition getListItemThemeDefinition(Theme theme) {
            return theme.getDefinition(Theme.Category.DIALOG_AREA);
        }

        @Override
        protected Interactable.Result unhandledKeyboardEvent(Key key) {
            if (getSelectedIndex() == UNSELECTED) {
                return Interactable.Result.EVENT_NOT_HANDLED;
            }

            if (key.getKind() == Key.Kind.Enter || key.getCharacter() == ' ') {

                if (isChecked(getSelectedIndex())) {
                    setCheckedItemIndex(UNSELECTED);
                } else {
                    setCheckedItemIndex(getSelectedIndex());
                }

                return Result.EVENT_HANDLED;
            }

            return Result.EVENT_NOT_HANDLED;
        }
    }

    /**
     * Window to select the default profile which should be used to start RCE by default.
     * 
     * @author Tobias Brieden
     */
    private class SelectDefaultProfileWindow extends Window {

        SelectDefaultProfileWindow() throws ProfileException {
            super(OPTION_SELECT_DEFAULT_PROFILE);

            File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();

            String allProfilesPanelLabel = StringUtils.format("All available profiles in %s", profileParentDirectory.getAbsolutePath());
            Panel allProfilesPanel = new Panel(allProfilesPanelLabel);
            allProfilesPanel.setBorder(new Border.Standard());
            final RadioCheckBoxList allProfilesListBox = new DeselectableRadioCheckBoxList();
            allProfilesListBox.setPreferredSize(new TerminalSize(PROFILE_LIST_WIDTH, 10));

            File savedDefaultProfile = null;
            try {
                savedDefaultProfile = CommonProfileUtils.getSavedDefaultProfile();
            } catch (CommonProfileException e) {
                LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
            }

            for (final Profile profile : ProfileUtils.listProfiles(profileParentDirectory)) {
                allProfilesListBox.addItem(profile);
                // mark the default profile
                if (savedDefaultProfile != null
                    && savedDefaultProfile.getAbsolutePath().equals(profile.getProfileDirectory().getAbsolutePath())) {

                    int index = allProfilesListBox.getNrOfItems() - 1;
                    allProfilesListBox.setCheckedItemIndex(index);
                }
            }

            allProfilesPanel.addComponent(allProfilesListBox);
            addComponent(allProfilesPanel);

            int width = allProfilesPanel.getPreferredSize().getColumns();
            Label warning = new Label(LanternaUtils.applyWordWrapping(DEFAULT_WARNING_TEXT, width));
            addComponent(warning);

            final Action okAction = new Action() {

                @Override
                public void doAction() {
                    Object checkedItem = allProfilesListBox.getCheckedItem();
                    if (checkedItem == null) {
                        try {
                            CommonProfileUtils.clearDefaultProfile();
                            LanternaUtils.showSuccessMessageBox(guiScreen, "No profile is selected as default profile.");
                        } catch (CommonProfileException e) {
                            // exception is never thrown by the method
                        }

                    } else {
                        Profile checkedProfile = (Profile) checkedItem;
                        try {
                            checkedProfile.markAsDefaultProfile();
                            LanternaUtils.showSuccessMessageBox(guiScreen,
                                StringUtils.format("Marked \"%s\" as the default profile.", checkedProfile.getName()));
                        } catch (ProfileException e) {
                            LanternaUtils.showErrorMessageBox(guiScreen, "Unable to store default profile selection.");
                        }

                    }

                    SelectDefaultProfileWindow.this.close();
                }
            };

            final Action cancelAction = new Action() {

                @Override
                public void doAction() {
                    SelectDefaultProfileWindow.this.close();
                }
            };

            addComponent(LanternaUtils.createOkCancelButtonPanel(okAction, cancelAction));

            addWindowListener(new WindowAdapter() {

                @Override
                public void onUnhandledKeyboardInteraction(Window arg0, Key key) {
                    if (key.getKind() == Key.Kind.Escape) {
                        cancelAction.doAction();
                    }
                }

            });
        }

    }

    private String showMainMenu() {
        List<String> options = new ArrayList<>();
        options.add(OPTION_SELECT_PROFILE);
        options.add(OPTION_SELECT_DEFAULT_PROFILE);
        String result = (String) ListSelectDialog.showDialog(guiScreen, "Select Action", null, options.toArray());
        return result;
    }
}
