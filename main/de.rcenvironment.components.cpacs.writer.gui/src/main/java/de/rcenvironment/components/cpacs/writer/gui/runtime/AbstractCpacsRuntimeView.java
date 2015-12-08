/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.runtime;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.gui.workflow.view.ComponentRuntimeView;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.SimpleNotificationService;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Abstract super class of all runtime views in this bundle.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Sascha Zur
 */
public abstract class AbstractCpacsRuntimeView extends ViewPart implements ComponentRuntimeView {

    protected static final TempFileService TEMP_MANAGER = TempFileServiceAccess.getInstance();

    private static final int MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT = 300;

    private static File managedTempDir = null;

    /**
     * Component information of this component view.
     */
    protected ComponentExecutionInformation componentInstanceDescriptor = null;

    /**
     * Notification service to get notified on new history objects.
     */
    protected SimpleNotificationService notificationService = new SimpleNotificationService();

    /**
     * The simplified version for DM reference accesses.
     */
    protected DataManagementService dataManagementService;

    /**
     * The form for the widgets.
     */
    protected ScrolledForm form = null;

    private Text fileContentText;

    private Log log;

    private File cpacsFile = null;

    private Button mappedB;

    @Deprecated
    public AbstractCpacsRuntimeView() {
        super();
    }

    @Override
    public void initializeData(ComponentExecutionInformation componentExecutionInformation) {
        componentInstanceDescriptor = componentExecutionInformation;
        if (managedTempDir == null) {
            try {
                managedTempDir = TEMP_MANAGER.createManagedTempDir();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        dataManagementService = serviceRegistryAccess.getService(DataManagementService.class);
        final String tempFileReference = getFileReference();
        if (tempFileReference != null) {
            try {
                cpacsFile = new File(managedTempDir, tempFileReference + ".xml");
                if (!cpacsFile.exists()) {
                    // As the file is disposed when the view is disposed, the file will never exist at that point and must be fetched
                    // anew. As the view won't be re-opened very often and won't be executed an a remote node very often, this behavior
                    // is acceptable for now. Will be changed if temp file clean up on RCE shutdown is improved -seid_do
                    dataManagementService.copyReferenceToLocalFile(
                        tempFileReference, cpacsFile, componentInstanceDescriptor.getDefaultStorageNodeId());
                }
            } catch (final IOException | CommunicationException e) {
                log.error("Fetching CPACS file failed", e);
            }
        }
    }

    @Override
    public void initializeView() {
        if (mappedB != null) { // This button only exists on Windows
            mappedB.setEnabled(cpacsFile != null);
        }
        if (cpacsFile != null && cpacsFile.exists()) {
            try {
                fileContentText.setText(FileUtils.readFileToString(cpacsFile, Charset.defaultCharset()));
            } catch (IOException e) {
                log.error(e);
            }
        } else {
            fileContentText.setText("");
        }
    }

    /**
     * To be used by inheriting classes.
     * 
     * @param parent The parent frame
     * @return null or an error
     */
    protected void createPartControl(final Composite parent, final Log logger) {
        final FormToolkit tk = new FormToolkit(parent.getDisplay()); // factory to create fancy form
        this.log = logger;
        // controls and layout
        form = tk.createScrolledForm(parent);
        form.getBody().setLayout(new GridLayout());

        if (Platform.OS_WIN32.equals(Platform.getOS())) {
            final Composite main = tk.createComposite(form.getBody());
            main.setLayout(new FillLayout(SWT.HORIZONTAL));
            mappedB = tk.createButton(main, "Show in TiGL Viewer", SWT.PUSH);
            mappedB.setEnabled(false);
            mappedB.addListener(SWT.Selection, new Listener() {

                @Override
                public void handleEvent(final Event event) {
                    if (cpacsFile != null) {
                        performShowAction(cpacsFile);
                    }
                }
            });
        }
        GridData gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gridData.heightHint = MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT;
        fileContentText = tk.createText(form.getBody(), "", SWT.V_SCROLL | SWT.H_SCROLL);
        fileContentText.setEditable(false);
        fileContentText.setLayoutData(gridData);
    }

    /**
     * This needs to be implemented by the specific view/editor.
     * 
     * @param file The file to show
     */
    protected abstract void performShowAction(final File file);

    /**
     * Check if there is a recent history entry notification. This is needed to show the last version of the incoming + mapped CPACS
     * document.
     * 
     * @return The object or null if unavailable
     */
    private String getFileReference() {
        final Map<String, List<Notification>> all =
            notificationService.getNotifications(componentInstanceDescriptor.getExecutionIdentifier()
                + CpacsWriterComponentConstants.RUNTIME_CPACS_UUIDS, componentInstanceDescriptor.getNodeId());
        if (all.size() >= 1) {
            final List<Notification> notifications = all.values().iterator().next(); // get list of
            // notifications
            // for first (and
            // only) id in map
            if (notifications.size() < 1) {
                return null; // no notification in queue
            }
            final Serializable object = notifications.get(0).getBody();
            if (object instanceof String) {
                return (String) object;
            }
        }

        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (cpacsFile != null) {
            try {
                TEMP_MANAGER.disposeManagedTempDirOrFile(cpacsFile);
            } catch (IOException e) {
                log.error("Deleting CPACS file in temp directory failed", e);
            }
        }
    }
}
