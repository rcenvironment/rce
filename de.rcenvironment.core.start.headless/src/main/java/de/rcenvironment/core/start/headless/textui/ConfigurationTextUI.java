/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.headless.textui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.DefaultBackgroundRenderer;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.component.Label;
import com.googlecode.lanterna.gui.component.PasswordBox;
import com.googlecode.lanterna.gui.component.RadioCheckBoxList;
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
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.ui.LanternaUtils;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.core.mail.SMTPServerConfiguration;
import de.rcenvironment.core.mail.SMTPServerConfigurationService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * An interactive configuration shell. Currently used to configure SSH accounts with secure password handling.
 * 
 * @author Robert Mischke
 */
public class ConfigurationTextUI {

    private static final String OPTION_CHANGE_PASSWORD = "Change password";

    private static final String OPTION_CONVERT_TO_RA_ACCOUNT = "Convert to a Remote Access account (change \"role\")";

    private static final int DEFAULT_TEXT_FIELD_WIDTH = 40;

    private static final String OPTION_ADD_SSH_ACCOUNT = "Remote Access: Add a new SSH account";

    private static final String OPTION_EDIT_SSH_ACCOUNTS = "Remote Access: Edit existing SSH accounts";

    private static final String OPTION_CONFIGURE_SMTP_SERVER = "Mail: Configure SMTP mail server";

    private static final String OPTION_ENABLE_ACCOUNT = "Enable account";

    private static final String OPTION_DISABLE_ACCOUNT = "Disable account";

    private static final String OPTION_DELETE_ACCOUNT = "Permanently delete account";

    private static final String REMOTE_ACCESS_ROLE_ID = "remote_access_user";

    // This was the name of the remote access role in earlier versions, keep for backwards compatibility
    private static final String REMOTE_ACCESS_ROLE_ID_ALIAS = "remote access";

    private final SshAccountConfigurationService sshAccountOperations;

    private final SMTPServerConfigurationService smtpServerConfigurationOperations;

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

        SshAccountUIEntry(String displayText, SshAccount account) {
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

        CustomTextBox(String initialContent, int width, Action enterAction) {
            super(initialContent, width);
            this.enterAction = enterAction;
        }

        @Override
        public void setText(String text) {
            if (text != null) {
                super.setText(text);
            }
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

        CustomPasswordBox(String initialContent, int width, Action enterAction) {
            super(initialContent, width);
            this.enterAction = enterAction;
        }

        @Override
        public void setText(String text) {
            if (text != null) {
                super.setText(text);
            }
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
     * This check box allows a selection based on an item.
     *
     * @author Tobias Rodehutskors
     */
    private class SetCheckedItemRadioCheckBoxList extends RadioCheckBoxList {

        private static final int CLEAR_SELECTION_INDEX = -1;

        /**
         * @param item If the item is not known to the check box, no item is selected. Otherwise, the given item is selected.
         */
        public void setCheckedItem(Object item) {
            this.setCheckedItemIndex(CLEAR_SELECTION_INDEX); // clear the current selection

            for (int i = 0; i < this.getNrOfItems(); i++) {
                if (this.getItemAt(i).equals(item)) {
                    this.setCheckedItemIndex(i);
                    break;
                }
            }
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

        AddAccountWindow() {
            super("Add a new Remote Access account");

            final Action okAction = new Action() {

                @Override
                public void doAction() {
                    try {
                        final String loginName = textBoxName.getText();
                        final String password = textBoxPassword.getText();
                        sshAccountOperations.createAccount(loginName, password);
                        // success -> close dialog
                        AddAccountWindow.this.close();
                        LanternaUtils.showSuccessMessageBox(guiScreen, "The account \"" + loginName + "\" was successfully added.");
                    } catch (ConfigurationException e) {
                        LanternaUtils.showErrorMessageBox(guiScreen, "Failed to create the account: " + e.getMessage());
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

            addComponent(LanternaUtils.createOkCancelButtonPanel(okAction, cancelAction));

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

    /**
     * Dialog for entering the SMTP mail server configuration.
     * 
     * @author Tobias Rodehutskors
     */
    private class ConfigureSMTPServerWindow extends Window {

        private TextBox textBoxHost;

        private TextBox textBoxPort;

        private SetCheckedItemRadioCheckBoxList radioCheckBoxEncryption;

        private TextBox textBoxUsername;

        private PasswordBox passwordBoxPassword;

        private TextBox textBoxSender;

        ConfigureSMTPServerWindow() {
            super("SMTP mail server configuration");

            final Action okAction = new Action() {

                @Override
                public void doAction() {
                    try {
                        final String host = textBoxHost.getText();
                        final int port;
                        try {
                            port = Integer.parseInt(textBoxPort.getText());
                        } catch (NumberFormatException e) {
                            throw new ConfigurationException("Invalid port number.");
                        }
                        final String encryption = (String) radioCheckBoxEncryption.getCheckedItem();
                        final String username = textBoxUsername.getText();
                        final String password = passwordBoxPassword.getText();
                        final String sender = textBoxSender.getText();

                        smtpServerConfigurationOperations.configureSMTPServer(host, port, encryption, username, password, sender);
                        // success -> close dialog
                        ConfigureSMTPServerWindow.this.close();
                        LanternaUtils.showSuccessMessageBox(guiScreen, "Successfully stored the SMTP server configuration.");
                    } catch (ConfigurationException e) {
                        LanternaUtils.showErrorMessageBox(guiScreen, "Unable to store the configuration: " + e.getMessage());
                    }
                }
            };
            final Action cancelAction = new Action() {

                @Override
                public void doAction() {
                    ConfigureSMTPServerWindow.this.close();
                }
            };

            addComponent(new Label("Host:"));
            textBoxHost = new CustomTextBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);
            addComponent(textBoxHost);
            addComponent(new Label("Port:"));
            textBoxPort = new CustomTextBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);
            addComponent(textBoxPort);
            addComponent(new Label("Encryption:"));
            radioCheckBoxEncryption = new SetCheckedItemRadioCheckBoxList();
            radioCheckBoxEncryption.addItem(SMTPServerConfiguration.EXPLICIT_ENCRYPTION);
            radioCheckBoxEncryption.addItem(SMTPServerConfiguration.IMPLICIT_ENCRYPTION);
            addComponent(radioCheckBoxEncryption);
            addComponent(new Label("Username:"));
            textBoxUsername = new CustomTextBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);
            addComponent(textBoxUsername);
            addComponent(new Label("Password:"));
            passwordBoxPassword = new CustomPasswordBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);
            addComponent(passwordBoxPassword);
            addComponent(new Label("Sender:"));
            textBoxSender = new CustomTextBox("", DEFAULT_TEXT_FIELD_WIDTH, okAction);
            addComponent(textBoxSender);

            SMTPServerConfiguration smtpServerConfiguration = smtpServerConfigurationOperations.getSMTPServerConfiguration();
            if (smtpServerConfiguration != null) {
                textBoxHost.setText(smtpServerConfiguration.getHost());
                textBoxPort.setText(Integer.toString(smtpServerConfiguration.getPort()));
                radioCheckBoxEncryption.setCheckedItem(smtpServerConfiguration.getEncryption());
                textBoxUsername.setText(smtpServerConfiguration.getUsername());
                passwordBoxPassword.setText(smtpServerConfiguration.getPassword());
                textBoxSender.setText(smtpServerConfiguration.getSenderAsString());
            }

            addComponent(LanternaUtils.createOkCancelButtonPanel(okAction, cancelAction));

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

    public ConfigurationTextUI(SshAccountConfigurationService sshConfigurationService,
        SMTPServerConfigurationService smtpServerConfigurationOperations) {
        // this.configurationService = configurationService;
        this.sshAccountOperations = sshConfigurationService;
        this.smtpServerConfigurationOperations = smtpServerConfigurationOperations;
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
        String profileName = BootstrapConfiguration.getInstance().getProfile().getLocationDependentName();
        guiScreen.setBackgroundRenderer(new DefaultBackgroundRenderer("RCE Configuration Shell, editing profile : " + profileName));
        guiScreen.getScreen().startScreen();

        String verifyError = sshAccountOperations.verifyExpectedStateForConfigurationEditing();
        if (verifyError == null) {
            try {
                runMainLoop();
            } catch (RuntimeException e) {
                LanternaUtils.showErrorMessageBox(guiScreen, "There was an internal error running the configuration menu. "
                    + "Please check the log file for details");
                log.error("Uncaught RuntimeException in text UI", e);
            }
        } else {
            LanternaUtils.showErrorMessageBox(guiScreen, verifyError);
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
            case OPTION_CONFIGURE_SMTP_SERVER:
                guiScreen.showWindow(new ConfigureSMTPServerWindow(), GUIScreen.Position.CENTER);
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
        options.add(OPTION_CONFIGURE_SMTP_SERVER);
        String result = (String) ListSelectDialog.showDialog(guiScreen, "Select Action", null, options.toArray());
        return result;
    }

    // @Deprecated
    // // left in for now; can be deleted when UI is finished
    // private void showGeneratePasswordHashDialog() {
    // final String name =
    // TextInputDialog.showTextInputBox(guiScreen, "Password hash generation (temporary)",
    // "Enter an id (this will determine the output file's name)", "");
    // if (name == null) {
    // return;
    // }
    // final String pw =
    // TextInputDialog.showPasswordInputBox(guiScreen, "Password hash generation (temporary)", "Enter the new password", "");
    // if (pw == null) {
    // return;
    // }
    // final String hash = sshAccountOperations.generatePasswordHash(pw);
    // final File outputDir = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT);
    // final File outFile = new File(outputDir, "pwhash_" + name + ".txt");
    // try {
    // FileUtils.write(outFile, hash);
    // log.info("Password hash written to " + outFile.getAbsolutePath());
    // } catch (IOException e) {
    // log.error(e.getStackTrace());
    // }
    // }

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
            LanternaUtils.showErrorMessageBox(guiScreen, e.getMessage());
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
            LanternaUtils.showErrorMessageBox(guiScreen, "There are no SSH accounts (yet)!");
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
                    LanternaUtils.showErrorMessageBox(guiScreen, "Password change aborted");
                    return;
                }
                sshAccountOperations.updatePasswordHash(loginName, newPW);
                LanternaUtils.showSuccessMessageBox(guiScreen, "Account password updated");
                break;
            case OPTION_CONVERT_TO_RA_ACCOUNT:
                sshAccountOperations.updateRole(loginName, REMOTE_ACCESS_ROLE_ID);
                LanternaUtils.showSuccessMessageBox(guiScreen, "Converted to \"Remote Access\" account");
                break;
            case OPTION_ENABLE_ACCOUNT:
                sshAccountOperations.setAccountEnabled(loginName, true);
                LanternaUtils.showSuccessMessageBox(guiScreen, "Account enabled");
                return;
            case OPTION_DISABLE_ACCOUNT:
                sshAccountOperations.setAccountEnabled(loginName, false);
                LanternaUtils.showSuccessMessageBox(guiScreen, "Account disabled");
                return;
            case OPTION_DELETE_ACCOUNT:
                DialogResult confirmation =
                    MessageBox.showMessageBox(guiScreen, "Confirm account deletion", "Really delete account \"" + loginName
                        + "\"?", DialogButtons.YES_NO);
                if (confirmation != DialogResult.YES) {
                    return;
                }
                sshAccountOperations.deleteAccount(loginName);
                LanternaUtils.showSuccessMessageBox(guiScreen, "Account \"" + loginName + "\" deleted");
                return;
            default:
                LanternaUtils.showErrorMessageBox(guiScreen, "Internal error: no such option");
                break;
            }
        } catch (ConfigurationException e) {
            LanternaUtils.showErrorMessageBox(guiScreen, "Operation failed: " + e.getMessage());
        }
    }

    private boolean isRemoteAccessAccount(SshAccount account) {
        return REMOTE_ACCESS_ROLE_ID.equals(account.getRole()) || REMOTE_ACCESS_ROLE_ID_ALIAS.equals(account.getRole()); // tolerate null
                                                                                                                         // values
    }
}
