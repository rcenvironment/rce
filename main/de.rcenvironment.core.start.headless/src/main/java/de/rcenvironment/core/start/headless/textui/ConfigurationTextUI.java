/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.headless.textui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.DefaultBackgroundRenderer;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.component.Button;
import com.googlecode.lanterna.gui.component.Label;
import com.googlecode.lanterna.gui.component.Panel;
import com.googlecode.lanterna.gui.component.PasswordBox;
import com.googlecode.lanterna.gui.component.TextBox;
import com.googlecode.lanterna.gui.dialog.DialogButtons;
import com.googlecode.lanterna.gui.dialog.DialogResult;
import com.googlecode.lanterna.gui.dialog.ListSelectDialog;
import com.googlecode.lanterna.gui.dialog.MessageBox;
import com.googlecode.lanterna.gui.dialog.TextInputDialog;
import com.googlecode.lanterna.gui.listener.WindowAdapter;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.input.Key.Kind;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * An interactive configuration shell. Currently used to configure SSH accounts with secure password handling.
 * 
 * @author Robert Mischke
 */
public class ConfigurationTextUI {

    private static final int WORD_WRAPPING_MAX_LINE_LENGTH = 60;

    private static final String OPTION_CHANGE_PASSWORD = "Change password";

    private static final String OPTION_CONVERT_TO_RA_ACCOUNT = "Convert to a Remote Access account (change \"role\")";

    private static final String DIALOG_TITLE_SUCCESS = "Success";

    private static final String DIALOG_TITLE_ERROR = "Error";

    private static final int DEFAULT_TEXT_FIELD_WIDTH = 40;

    private static final String OPTION_ADD_SSH_ACCOUNT = "Remote Access: Add a new SSH account";

    private static final String OPTION_EDIT_SSH_ACCOUNTS = "Remote Access: Edit existing SSH accounts";

    private static final String OPTION_ENABLE_ACCOUNT = "Enable account";

    private static final String OPTION_DISABLE_ACCOUNT = "Disable account";

    private static final String OPTION_DELETE_ACCOUNT = "Permanently delete account";

    private static final String REMOTE_ACCESS_ROLE_ID = "remote access";

    // private static final String OPTION_EXIT = "Exit";

    private final ConfigurationService configurationService;

    private final SshAccountConfigurationService sshAccountOperations;

    private GUIScreen guiScreen;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * UI representation of an SSH account.
     * 
     * @author Robert Mischke
     */
    private class SshAccountUIEntry {

        private String displayText;

        private SshAccount account;

        public SshAccountUIEntry(String displayText, SshAccount account) {
            this.displayText = displayText;
            this.account = account;
        }

        public SshAccount getAccount() {
            return account;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    /**
     * Overrides the default behavior on "enter", which is to move to the next UI element. Instead, a custom {@link Action} can be executed.
     * 
     * @author Robert Mischke
     */
    private class CustomTextBox extends TextBox {

        private Action enterAction;

        public CustomTextBox(String initialContent, int width, Action enterAction) {
            super(initialContent, width);
            this.enterAction = enterAction;
        }

        @Override
        public Result keyboardInteraction(Key key) {
            if (key.getKind() == Kind.Enter) {
                if (enterAction != null) {
                    enterAction.doAction();
                }
                return Result.EVENT_HANDLED;
            }
            return super.keyboardInteraction(key);
        }
    }

    /**
     * Overrides the default behavior on "enter", which is to move to the next UI element. Instead, a custom {@link Action} can be executed.
     * 
     * @author Robert Mischke
     */
    private class CustomPasswordBox extends PasswordBox {

        private Action enterAction;

        public CustomPasswordBox(String initialContent, int width, Action enterAction) {
            super(initialContent, width);
            this.enterAction = enterAction;
        }

        @Override
        public Result keyboardInteraction(Key key) {
            if (key.getKind() == Kind.Enter) {
                if (enterAction != null) {
                    enterAction.doAction();
                }
                return Result.EVENT_HANDLED;
            }
            return super.keyboardInteraction(key);
        }
    }

    /**
     * Dialog for entering parameters for a new SSH account.
     * 
     * @author Robert Mischke
     */
    private class AddAccountWindow extends Window {

        private TextBox textBoxName;

        private PasswordBox textBoxPassword;

        public AddAccountWindow() {
            super("Add a new Remote Access account");

            final Action okAction = new Action() {

                @Override
                public void doAction() {
                    try {
                        final String loginName = textBoxName.getText();
                        final String password = textBoxPassword.getText();
                        sshAccountOperations.createAccount(loginName, password, true);
                        // success -> close dialog
                        AddAccountWindow.this.close();
                        showSuccessMessageBox("The account \"" + loginName + "\" was successfully added.");
                    } catch (ConfigurationException e) {
                        showErrorMessageBox("Failed to create the account: " + e.getMessage());
                    }
                }
            };
            final Action cancelAction = new Action() {

                @Override
                public void doAction() {
                    AddAccountWindow.this.close();
                }
            };

            textBoxName = new CustomTextBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);
            textBoxPassword = new CustomPasswordBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);

            addComponent(new Label("Login name:"));
            addComponent(textBoxName);
            addComponent(new Label("Password:"));
            addComponent(textBoxPassword);

            addComponent(createOkCancelButtonPanel(okAction, cancelAction));

            addWindowListener(new WindowAdapter() {

                @Override
                public void onUnhandledKeyboardInteraction(Window arg0, Key key) {
                    if (key.getKind() == Key.Kind.Escape) {
                        cancelAction.doAction();
                        return;
                    }
                    if (key.getKind() == Key.Kind.Enter) {
                        okAction.doAction();
                        return;
                    }
                    log.debug("Unhandled key in text-mode UI: " + key);
                }

            });
        }

    }

    public ConfigurationTextUI(ConfigurationService configurationService, SshAccountConfigurationService sshConfigurationService) {
        this.configurationService = configurationService;
        this.sshAccountOperations = sshConfigurationService;
    }

    /**
     * Main method of the interactive configuration shell.
     */
    public void run() {
        guiScreen = TerminalFacade.createGUIScreen();
        if (guiScreen == null) {
            log.error("Failed to initialize text-mode UI; terminating");
            return;

        }
        guiScreen.setBackgroundRenderer(new DefaultBackgroundRenderer("RCE Configuration Shell"));
        guiScreen.getScreen().startScreen();

        String verifyError = sshAccountOperations.verifyExpectedStateForConfigurationEditing();
        if (verifyError == null) {
            try {
                runMainLoop();
            } catch (RuntimeException e) {
                showErrorMessageBox("There was an internal error running the configuration menu. "
                    + "Please check the log file for details");
                log.error("Uncaught RuntimeException in text UI", e);
            }
        } else {
            showErrorMessageBox(verifyError);
        }

        guiScreen.getScreen().stopScreen();
    }

    private void runMainLoop() {
        while (true) {
            String action = showMainMenu();
            if (action == null) {
                return; // exit
            }
            switch (action) {
            case OPTION_ADD_SSH_ACCOUNT:
                showAddAccountDialog();
                break;
            case OPTION_EDIT_SSH_ACCOUNTS:
                showSelectExistingAccountDialog();
                break;
            default:
                log.error("Invalid action: " + action);
            }
        }
    }

    private String showMainMenu() {
        List<String> options = new ArrayList<>();
        options.add(OPTION_ADD_SSH_ACCOUNT);
        options.add(OPTION_EDIT_SSH_ACCOUNTS);
        String result = (String) ListSelectDialog.showDialog(guiScreen, "Select Action", null, options.toArray());
        return result;
    }

    @Deprecated
    // left in for now; can be deleted when UI is finished
    private void showGeneratePasswordHashDialog() {
        final String name =
            TextInputDialog.showTextInputBox(guiScreen, "Password hash generation (temporary)",
                "Enter an id (this will determine the output file's name)", "");
        if (name == null) {
            return;
        }
        final String pw =
            TextInputDialog.showPasswordInputBox(guiScreen, "Password hash generation (temporary)", "Enter the new password", "");
        if (pw == null) {
            return;
        }
        final String hash = sshAccountOperations.generatePasswordHash(pw);
        final File outputDir = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT);
        final File outFile = new File(outputDir, "pwhash_" + name + ".txt");
        try {
            FileUtils.write(outFile, hash);
            log.info("Password hash written to " + outFile.getAbsolutePath());
        } catch (IOException e) {
            log.error(e.getStackTrace());
        }
    }

    private void showAddAccountDialog() {
        guiScreen.showWindow(new AddAccountWindow(), GUIScreen.Position.CENTER);
    }

    private void showSelectExistingAccountDialog() {
        List<SshAccountUIEntry> options = new ArrayList<>();
        SortedMap<String, SshAccount> accountMap;
        try {
            accountMap = sshAccountOperations.getAllAccountsByLoginName();
        } catch (ConfigurationException e) {
            log.error("Error getting account data", e);
            showErrorMessageBox(e.getMessage());
            return;
        }
        for (Entry<String, SshAccount> accountEntry : accountMap.entrySet()) {
            final String id = accountEntry.getKey();
            final SshAccount account = accountEntry.getValue();
            String loginName = account.getLoginName();
            // should be the same
            if (!id.equals(loginName)) {
                log.error(StringUtils.format(
                    "Internal consistency error: account returned with map id '%s', but the account user name is '%s'",
                    id, loginName));
            }
            String displayText;
            if (isRemoteAccessAccount(account)) {
                displayText = loginName;
            } else {
                displayText = loginName + " [custom configuration]";
            }
            if (!account.isEnabled()) {
                displayText += " [disabled]";
            }
            options.add(new SshAccountUIEntry(displayText, account));
        }
        if (options.isEmpty()) {
            showErrorMessageBox("There are no SSH accounts (yet)!");
            return;
        }
        SshAccountUIEntry accountEntry =
            (SshAccountUIEntry) ListSelectDialog.showDialog(guiScreen, "Select an account to edit", null, options.toArray());
        if (accountEntry == null) {
            return; // exit
        }

        showEditSelectedAccountDialog(accountEntry.getAccount());
    }

    private void showEditSelectedAccountDialog(SshAccount account) {
        final String loginName = account.getLoginName();
        final List<String> options = new ArrayList<>();

        // assemble options
        options.add(OPTION_CHANGE_PASSWORD);
        if (!isRemoteAccessAccount(account)) {
            options.add(OPTION_CONVERT_TO_RA_ACCOUNT);
        }
        if (account.isEnabled()) {
            options.add(OPTION_DISABLE_ACCOUNT);
        } else {
            options.add(OPTION_ENABLE_ACCOUNT);
        }
        options.add(OPTION_DELETE_ACCOUNT);

        String selection = (String) ListSelectDialog.showDialog(guiScreen, "Configure Account \"" + loginName + "\"", null,
            options.toArray());
        if (selection == null) {
            return;
        }

        try {
            switch (selection) {
            case OPTION_CHANGE_PASSWORD:
                String newPW =
                    TextInputDialog.showPasswordInputBox(guiScreen, OPTION_CHANGE_PASSWORD,
                        "Enter the new passwword for account \"" + loginName + "\":", "");
                if (StringUtils.isNullorEmpty(newPW)) {
                    showErrorMessageBox("Password change aborted");
                    return;
                }
                sshAccountOperations.updatePasswordHash(loginName, newPW);
                showSuccessMessageBox("Account password updated");
                break;
            case OPTION_CONVERT_TO_RA_ACCOUNT:
                sshAccountOperations.updateRole(loginName, REMOTE_ACCESS_ROLE_ID);
                showSuccessMessageBox("Converted to \"Remote Access\" account");
                break;
            case OPTION_ENABLE_ACCOUNT:
                sshAccountOperations.setAccountEnabled(loginName, true);
                showSuccessMessageBox("Account enabled");
                return;
            case OPTION_DISABLE_ACCOUNT:
                sshAccountOperations.setAccountEnabled(loginName, false);
                showSuccessMessageBox("Account disabled");
                return;
            case OPTION_DELETE_ACCOUNT:
                DialogResult confirmation =
                    MessageBox.showMessageBox(guiScreen, "Confirm account deletion", "Really delete account \"" + loginName
                        + "\"?", DialogButtons.YES_NO);
                if (confirmation != DialogResult.YES) {
                    return;
                }
                sshAccountOperations.deleteAccount(loginName);
                showSuccessMessageBox("Account \"" + loginName + "\" deleted");
                return;
            default:
                showErrorMessageBox("Internal error: no such option");
                break;
            }
        } catch (ConfigurationException e) {
            showErrorMessageBox("Operation failed: " + e.getMessage());
        }
    }

    private Panel createOkCancelButtonPanel(final Action okAction, final Action cancelAction) {
        Button buttonOk = new Button("Ok", okAction);
        Button buttonCancel = new Button("Cancel", cancelAction);
        Panel buttonPanel = new Panel(Panel.Orientation.HORISONTAL);
        // apparently, this is the standard way to do this; see the ActionListDialog() constructor
        // TODO improve by calculating indentation width?
        buttonPanel.addComponent(new Label("                   "));
        buttonPanel.addComponent(buttonOk);
        buttonPanel.addComponent(buttonCancel);
        return buttonPanel;
    }

    private void showSuccessMessageBox(final String message) {
        MessageBox.showMessageBox(guiScreen, DIALOG_TITLE_SUCCESS, applyWordWrapping(message));
    }

    private void showErrorMessageBox(String message) {
        MessageBox.showMessageBox(guiScreen, DIALOG_TITLE_ERROR, applyWordWrapping(message));
    }

    private String applyWordWrapping(String input) {
        // note: assuming screen width of 80 characters; can it be different?
        return WordUtils.wrap(input, WORD_WRAPPING_MAX_LINE_LENGTH, "\n", true); // true = break long words
    }

    private boolean isRemoteAccessAccount(SshAccount account) {
        return REMOTE_ACCESS_ROLE_ID.equals(account.getRole()); // tolerate null values
    }
}
