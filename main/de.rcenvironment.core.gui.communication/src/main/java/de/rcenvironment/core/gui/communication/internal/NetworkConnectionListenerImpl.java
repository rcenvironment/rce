/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.internal;

import static de.rcenvironment.core.communication.connection.api.DisconnectReason.ERROR;
import static de.rcenvironment.core.communication.connection.api.DisconnectReason.REMOTE_SHUTDOWN;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;

import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListener;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListenerAdapter;
import de.rcenvironment.core.communication.connection.api.DisconnectReason;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Listener class to show warning or information dialogs when network connections changed.
 * 
 * @author Oliver Seebach
 * @author Robert Mischke
 */
public class NetworkConnectionListenerImpl implements IStartup {

    private static final String POPUP_TEXT_CONNECTION_WILL_BE_REATTEMPTED =
        "The connection will be automatically reattempted in the background.";

    private static final String POPUP_TEXT_CONNECTION_WILL_NOT_BE_REATTEMPTED =
        "The connection will not auto-reconnect. You can trigger a manual reconnect in the Network View.";

    private static final String DIALOG_TITLE = "Warning";

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private boolean warningOpen = false;

    public NetworkConnectionListenerImpl() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * TODO register even sooner (in non-gui bundle) and store messages until the GUI becomes available? - misc_ro
     */
    public void registerListener() {
        serviceRegistryAccess.registerService(ConnectionSetupListener.class, new ConnectionSetupListenerAdapter() {

            @Override
            public void onConnectionAttemptFailed(final ConnectionSetup setup, final boolean firstConsecutiveFailure,
                final boolean willAutoRetry) {
                // only show the first failure when auto-retrying
                if (!firstConsecutiveFailure) {
                    return;
                }
                final String message = StringUtils.format("Failed to establish network connection to \"%s\".\n\n%s",
                    setup.getDisplayName(), getAutoRetryRemark(willAutoRetry));
                showMessageDialogAsync(message);
            }

            @Override
            public void onConnectionClosed(ConnectionSetup setup, DisconnectReason disconnectReason, boolean willAutoRetry) {
                if (setup.getDisconnectReason() == ERROR) {
                    String message = StringUtils.format("Network connection to \"%s\" was closed due to a connection error.\n\n%s",
                        setup.getDisplayName(), getAutoRetryRemark(willAutoRetry));
                    showMessageDialogAsync(message);
                } else if (setup.getDisconnectReason() == REMOTE_SHUTDOWN) {
                    String message = StringUtils.format(
                        "Network connection to \"%s\" was closed because the remote node is shutting down.\n\n%s",
                        setup.getDisplayName(), getAutoRetryRemark(willAutoRetry));
                    showMessageDialogAsync(message);
                }
            }

            private String getAutoRetryRemark(final boolean willAutoRetry) {
                if (willAutoRetry) {
                    return POPUP_TEXT_CONNECTION_WILL_BE_REATTEMPTED;
                } else {
                    return POPUP_TEXT_CONNECTION_WILL_NOT_BE_REATTEMPTED;
                }
            }

            private void showMessageDialogAsync(final String message) {

                if (!warningOpen) {
                    final Display display = Display.getDefault();
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            Log log = LogFactory.getLog(getClass());
                            Shell shell = display.getActiveShell();
                            if (shell == null) {
                                log.debug("No active shell to open message dialog; using fallback");
                                for (Shell testShell : display.getShells()) {
                                    if (testShell.isVisible()) {
                                        shell = testShell;
                                        break;
                                    }
                                }
                                if (shell == null) {
                                    log.error("Failed to open message dialog; message text: " + message);
                                    return;
                                }
                            }
                            warningOpen = true;
                            MessageBox warning = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                            warning.setMessage(message);
                            warning.setText(DIALOG_TITLE);
                            int id = warning.open();
                            if (id == SWT.OK || id == SWT.CLOSE) {
                                warningOpen = false;
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void earlyStartup() {
        registerListener();
    }

}
