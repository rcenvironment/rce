/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import java.io.File;
import java.util.LinkedList;
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
 * @author Kathrin Schaffert (#17159, #15457)
 */
public class ProfileSelectionUI {

    /**
     * Parent class for the menu entries in the main menu of the profile selection UI.
     *
     * @author Alexander Weinert
     */
    private abstract class MainMenuAction {

        private final String label;

        MainMenuAction(String label) {
            this.label = label;
        }

        public abstract void execute() throws ProfileException;

        @Override
        public String toString() {
            return this.label;
        }
    }

    /**
     * This field exists to enforce a dependency to the LayoutParameter class. Otherwise, the associated bundle import might get removed,
     * which would result in a NoClassDefFound error at runtime.
     */
    private static LayoutParameter lp;

    private static final String OPTION_SELECT_PROFILE = "Select a profile and start RCE.";

    private static final String OPTION_SELECT_DEFAULT_PROFILE = "Select the default profile for future runs.";

    private static final String BACKGROUND_MESSAGE = "Profile Selection";

    private static final int PROFILE_LIST_WIDTH = 100;

    private static final String DEFAULT_WARNING_TEXT =
        "Note: If you select a default profile here, RCE will use this profile on the next start or restart. By using the "
            + "profile option (\"-p\"), you can still start RCE with a specific profile. "
            + "This is also the case if you have configured RCE as a service.";

    private static final String WARNING_TEXT_COULD_NOT_DETERMINE_VERSION_OF_PROFILE = "Could not determine version of profile \"%s\".";

    private static final String WARNING_TEXT_SEE_LOG = " See log for more details.";

    private GUIScreen guiScreen;

    private Profile selectedProfile = null;

    // TODO it should be possible to combine this dialog with the configuration ui
    // dialog. There should be a modular way to do that.

    private Terminal terminal;

    private boolean exitChooserUI = false;

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
        while (!exitChooserUI) {
            final MainMenuAction selectProfileAction = new MainMenuAction(OPTION_SELECT_PROFILE) {

                @Override
                public void execute() {
                    try {
                        File profilesParentDirectory = ProfileUtils.getProfilesParentDirectory();
                        guiScreen.showWindow(new SelectProfileWindow(profilesParentDirectory),
                            GUIScreen.Position.CENTER);

                        // check if a profile was selected
                        if (selectedProfile != null) {
                            exitChooserUI = true;
                        }

                    } catch (ProfileException e) {
                        LanternaUtils.showErrorMessageBox(guiScreen,
                            "The profiles parent directory cannot be created or it is not a directory.");
                    }
                }
            };

            final MainMenuAction selectDefaultProfileAction = new MainMenuAction(OPTION_SELECT_DEFAULT_PROFILE) {

                @Override
                public void execute() throws ProfileException {
                    guiScreen.showWindow(new SelectDefaultProfileWindow(), GUIScreen.Position.CENTER);
                }
            };
            final MainMenuAction action = showMainMenu(selectProfileAction, selectDefaultProfileAction);
            if (action == null) {
                exitChooserUI = true;
                return;
            }
            action.execute();
        }
    }

    /**
     * @param selectProfileAction An action to select the profile for the current startup of RCE
     * @param selectDefaultProfileAction An action to select the default profile for all future startups of RCE
     * @return The action chosen by the user. May be null if the user cancels the menu.
     */
    private MainMenuAction showMainMenu(MainMenuAction selectProfileAction, MainMenuAction selectDefaultProfileAction) {
        return ListSelectDialog.showDialog(guiScreen, "Select Action", null, selectProfileAction,
            selectDefaultProfileAction);
    }

    /**
     * Window to select the profile which should be used to start RCE with right now.
     * 
     * @author Tobias Brieden
     */
    private class SelectProfileWindow extends Window {

        SelectProfileWindow(File profileParentDirectory) {
            super(OPTION_SELECT_PROFILE);

            Panel defaultProfilePanel = createDefaultProfilePanel();
            addComponent(defaultProfilePanel);

            Panel recentProfilePanel = createRecentProfilePanel();
            addComponent(recentProfilePanel);

            Panel allProfilesPanel = createAllProfilesPanel(profileParentDirectory);
            addComponent(allProfilesPanel);

            addWindowListener(new WindowAdapter() {

                @Override
                public void onUnhandledKeyboardInteraction(Window arg0, Key key) {
                    if (key.getKind() == Key.Kind.Escape) {
                        SelectProfileWindow.this.close();
                    }
                }

            });

            // if one of the panels uses more space than preferred, because its content is
            // bigger, make sure that the other panels have the
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

        private Panel createDefaultProfilePanel() {
            Panel defaultProfilePanel = new Panel("Start RCE with the default profile");
            defaultProfilePanel.setBorder(new Border.Standard());
            ActionListBox defaultProfileListBox = new ActionListBox();

            try {
                final File defaultProfilePath = ProfileUtils.getDefaultProfilePath();
                Action startWithDefaultProfileAction = () -> {
                    try {
                        selectedProfile = new Profile.Builder(defaultProfilePath).create(true).migrate(false)
                            .buildUserProfile();
                        SelectProfileWindow.this.close();
                    } catch (ProfileException e) {
                        LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
                    }
                };
                defaultProfileListBox.addAction(defaultProfilePath.getAbsolutePath(), startWithDefaultProfileAction);
                defaultProfilePanel.addComponent(defaultProfileListBox);
            } catch (ProfileException e) {
                defaultProfilePanel
                    .addComponent(new Label("Unable to determine the default profile.", PROFILE_LIST_WIDTH));
            }
            return defaultProfilePanel;
        }

        private Panel createRecentProfilePanel() {
            Panel recentProfilePanel = new Panel("Recently used profiles");
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
                recentProfilePanel
                    .addComponent(new Label("Unable to load most recently used profiles.", PROFILE_LIST_WIDTH));
            } else {
                // We only want to show those profiles to the user that they may actually be
                // able to open.
                final List<Profile> compatibleProfiles = new LinkedList<>();

                for (final Profile recentlyUsedProfile : recentlyUsedProfiles) {
                    try {
                        if (recentlyUsedProfile.hasUpgradeableVersion() || recentlyUsedProfile.hasCurrentVersion()) {
                            compatibleProfiles.add(recentlyUsedProfile);
                        }
                    } catch (ProfileException e) {
                        final String errorMessage = WARNING_TEXT_COULD_NOT_DETERMINE_VERSION_OF_PROFILE;
                        recentProfilePanel.addComponent(
                            new Label(errorMessage + WARNING_TEXT_SEE_LOG, PROFILE_LIST_WIDTH));
                        log.error(String.format(errorMessage, recentlyUsedProfile.getName()), e);
                    }
                }

                for (final Profile profile : compatibleProfiles) {

                    final Action startWithProfileAction = () -> {
                        selectedProfile = profile;
                        SelectProfileWindow.this.close();
                    };

                    recentProfileListBox.addAction(profile.getProfileDirectory().getAbsolutePath(),
                        startWithProfileAction);
                }
                recentProfilePanel.addComponent(recentProfileListBox);
            }
            return recentProfilePanel;
        }

        private Panel createAllProfilesPanel(File profileParentDirectory) {
            // all profiles in the profiles home
            String allProfilesPanelLabel = StringUtils.format("All available profiles in %s",
                profileParentDirectory.getAbsolutePath());
            Panel allProfilesPanel = new Panel(allProfilesPanelLabel);
            allProfilesPanel.setBorder(new Border.Standard());
            ActionListBox allProfilesListBox = new ActionListBox();

            final List<Profile> allProfiles = ProfileUtils.listProfiles(profileParentDirectory);

            for (final Profile profile : allProfiles) {

                final Action startWithProfileAction = () -> {
                    selectedProfile = profile;
                    SelectProfileWindow.this.close();
                };

                allProfilesListBox.addAction(profile.getName(), startWithProfileAction);
            }

            allProfilesPanel.addComponent(allProfilesListBox);
            return allProfilesPanel;
        }

    }

    /**
     * Window to select the default profile which should be used to start RCE by default.
     * 
     * @author Tobias Brieden
     */
    private class SelectDefaultProfileWindow extends Window {

        private final RadioCheckBoxList allProfilesListBox = new DeselectableRadioCheckBoxList();

        private final RadioCheckBoxList recentProfilesListBox = new DeselectableRadioCheckBoxList();

        SelectDefaultProfileWindow() throws ProfileException {
            super(OPTION_SELECT_DEFAULT_PROFILE);

            File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();

            Panel recentProfilePanel = createRecentProfilePanel();
            addComponent(recentProfilePanel);

            Panel allProfilesPanel = createAllProfilesPanel(profileParentDirectory);
            addComponent(allProfilesPanel);

            int w1 = allProfilesPanel.getPreferredSize().getColumns();
            int w2 = recentProfilePanel.getPreferredSize().getColumns();
            int width = Math.max(Math.max(PROFILE_LIST_WIDTH, w1), w2);
            allProfilesPanel.setPreferredSize(new TerminalSize(width, 7));
            recentProfilePanel.setPreferredSize(new TerminalSize(width, 7));

            markDefaultProfile();

            Label warning = new Label(LanternaUtils.applyWordWrapping(DEFAULT_WARNING_TEXT, width));
            addComponent(warning);

            final Action okAction = this::performOKAction;

            final Action cancelAction = SelectDefaultProfileWindow.this::close;
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

        private void markDefaultProfile() {
            File savedDefaultProfile = null;
            try {
                savedDefaultProfile = CommonProfileUtils.getSavedDefaultProfile();
            } catch (CommonProfileException e) {
                LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
            }

            boolean checked = false;
            if (savedDefaultProfile == null) {
                return;
            }
            for (int i = 0; i < allProfilesListBox.getNrOfItems(); i++) {
                Profile profile = (Profile) allProfilesListBox.getItemAt(i);
                if (savedDefaultProfile.getAbsolutePath().equals(profile.getProfileDirectory().getAbsolutePath())) {
                    allProfilesListBox.setCheckedItemIndex(i);
                    checked = true;
                }
            }
            // mark recently used profile only if the profile is not located in the .rce profile directory
            if (!checked) {
                for (int i = 0; i < recentProfilesListBox.getNrOfItems(); i++) {
                    String path = (String) recentProfilesListBox.getItemAt(i);
                    if (savedDefaultProfile.getAbsolutePath().equals(path)) {
                        recentProfilesListBox.setCheckedItemIndex(i);
                    }
                }
            }

        }

        private void performOKAction() {

            Profile checkedProfile = getCheckedProfile();

            if (checkedProfile == null) {
                try {
                    CommonProfileUtils.clearDefaultProfile();
                    LanternaUtils.showSuccessMessageBox(guiScreen,
                        "No profile is selected as default profile.");
                } catch (CommonProfileException e) {
                    // exception is never thrown by the method
                }
            } else {
                try {
                    checkedProfile.markAsDefaultProfile();
                    LanternaUtils.showSuccessMessageBox(guiScreen, StringUtils
                        .format("Marked \"%s\" as the default profile.", checkedProfile.getName()));
                } catch (ProfileException e) {
                    LanternaUtils.showErrorMessageBox(guiScreen, "Unable to store default profile selection.");
                }
            }

            SelectDefaultProfileWindow.this.close();
        }

        private Profile getCheckedProfile() {
            Profile checkedProfile = null;
            if (allProfilesListBox.getCheckedItem() != null) {
                // the return value is already a Profile
                checkedProfile = (Profile) allProfilesListBox.getCheckedItem();
            }
            if (recentProfilesListBox.getCheckedItem() != null) {
                // the return value is the path to the Profile
                try {
                    new File((String) recentProfilesListBox.getCheckedItem());
                    checkedProfile =
                        new Profile.Builder(new File((String) recentProfilesListBox.getCheckedItem())).create(true).migrate(false)
                            .buildUserProfile();
                } catch (ProfileException e) {
                    LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
                }
            }
            return checkedProfile;
        }

        /**
         * This RadioCheckBoxList allows to toggle the selection of a previously selected entry by checking it again.
         *
         * @author Tobias Brieden
         */
        public class DeselectableRadioCheckBoxList extends RadioCheckBoxList {

            private static final int UNSELECTED = -1;

            // Override this method to make the background of the List grey (as opposed to
            // blue)
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

                    if (Boolean.TRUE.equals(isChecked(getSelectedIndex()))) {
                        setCheckedItemIndex(UNSELECTED);
                    } else {
                        if (allProfilesListBox.getCheckedItem() != null) {
                            allProfilesListBox.setCheckedItemIndex(UNSELECTED);
                        }
                        if (recentProfilesListBox.getCheckedItem() != null) {
                            recentProfilesListBox.setCheckedItemIndex(UNSELECTED);
                        }
                        setCheckedItemIndex(getSelectedIndex());
                    }

                    return Result.EVENT_HANDLED;
                }

                return Result.EVENT_NOT_HANDLED;
            }
        }

        private Panel createAllProfilesPanel(File profileParentDirectory) {
            String allProfilesPanelLabel = StringUtils.format("All available profiles in %s",
                profileParentDirectory.getAbsolutePath());
            Panel allProfilesPanel = new Panel(allProfilesPanelLabel);
            allProfilesPanel.setBorder(new Border.Standard());

            for (final Profile profile : ProfileUtils.listProfiles(profileParentDirectory)) {
                allProfilesListBox.addItem(profile);
            }

            allProfilesPanel.addComponent(allProfilesListBox);
            return allProfilesPanel;
        }

        private Panel createRecentProfilePanel() {
            Panel recentProfilePanel = new Panel("Recently used profiles");
            recentProfilePanel.setBorder(new Border.Standard());

            List<Profile> recentlyUsedProfiles = null;

            try {
                recentlyUsedProfiles = CommonProfileUtils.getRecentlyUsedProfiles();
            } catch (CommonProfileException e) {
                LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
            }

            final List<Profile> compatibleProfiles = new LinkedList<>();
            if (recentlyUsedProfiles != null) {
                for (final Profile recentlyUsedProfile : recentlyUsedProfiles) {
                    try {
                        if (recentlyUsedProfile.hasUpgradeableVersion() || recentlyUsedProfile.hasCurrentVersion()) {
                            compatibleProfiles.add(recentlyUsedProfile);
                        }
                    } catch (ProfileException e) {
                        final String errorMessage = WARNING_TEXT_COULD_NOT_DETERMINE_VERSION_OF_PROFILE;
                        recentProfilePanel.addComponent(
                            new Label(errorMessage + WARNING_TEXT_SEE_LOG, PROFILE_LIST_WIDTH));
                        log.error(String.format(errorMessage, recentlyUsedProfile.getName()), e);
                    }
                }
            }

            for (final Profile profile : compatibleProfiles) {
                recentProfilesListBox.addItem(profile.getProfileDirectory().getAbsolutePath());
            }

            recentProfilePanel.addComponent(recentProfilesListBox);
            return recentProfilePanel;
        }
    }
}
