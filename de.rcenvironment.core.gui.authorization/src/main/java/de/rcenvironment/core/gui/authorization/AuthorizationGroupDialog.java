/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.authorization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.SelectionProviderAction;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationIdRules;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.management.api.PermissionMatrixChangeListener;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * A dialog for managing authorization groups. The dialog facilitates to create or delete groups and to import and export group keys.
 * 
 * @author Oliver Seebach
 * @author Jan Flink
 * @author Robert Mischke (changed validation)
 */
public class AuthorizationGroupDialog extends TitleAreaDialog {

    private static final int DEFAULT_TEXTFIELD_WIDTH = 300;

    private static final String APOSTROPHE = "'";

    private static final String DOT = ".";

    private CreateGroupAction createGroupAction;

    private DeleteGroupAction deleteGroupAction;

    private ImportGroupKeyAction importGroupKeyAction;

    private ExportGroupKeyAction exportGroupKeyAction;

    private String groupNameForCreation;

    private TableViewer groupsTable;

    private Log log = LogFactory.getLog(getClass());

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private final AuthorizationService authorizationService;

    private final LocalComponentRegistrationService localComponentRegistrationService;

    private AuthorizationGroupViewerComparator comparator = new AuthorizationGroupViewerComparator();

    private ListSortAction sortActionAscending;

    private ListSortAction sortActionDescending;

    private ShowAuthorizationGroupIdAction showIdAction;

    public AuthorizationGroupDialog(Shell shell) {
        super(shell);
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        localComponentRegistrationService = serviceRegistryAccess.getService(LocalComponentRegistrationService.class);
        authorizationService = serviceRegistryAccess.getService(AuthorizationService.class);

    }

    private void registerChangeListeners() {
        serviceRegistryAccess.registerService(PermissionMatrixChangeListener.class, new PermissionMatrixChangeListener() {

            @Override
            public void onPermissionMatrixChanged(boolean accessGroupsChanged, boolean componentSelectorsChanged,
                boolean assignmentsChanged) {
                if (accessGroupsChanged) {
                    Display.getDefault().asyncExec(() -> {
                        if (!groupsTable.getControl().isDisposed()) {
                            groupsTable.refresh();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void create() {
        super.create();
        setTitle("Manage Authorization Groups");
        setMessage("Here you can create or delete authorization groups.\n"
            + " To share group access it is possible to import or export group keys.", IMessageProvider.INFORMATION);
        getButton(IDialogConstants.OK_ID).setVisible(false);
        getButton(IDialogConstants.CANCEL_ID).setText("Close");
    }

    @Override
    public Control createDialogArea(Composite parent) {

        Composite composite = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
        GridLayout gridlayout = new GridLayout(2, false);
        composite.setLayout(gridlayout);
        GridData compositeData = new GridData(GridData.FILL_BOTH);
        compositeData.heightHint = AuthorizationConstants.DIALOG_HEIGHT_HINT;
        composite.setLayoutData(compositeData);

        ToolBar toolbar = new ToolBar(composite, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
        GridData toolbarGridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
        toolbar.setLayoutData(toolbarGridData);

        Label placeholderGroupLabel = new Label(composite, SWT.NONE);
        placeholderGroupLabel.setEnabled(false); // "use" label for checkstyle

        ScrolledComposite scrollComposite = new ScrolledComposite(composite, SWT.H_SCROLL
            | SWT.V_SCROLL);
        scrollComposite.setLayout(new GridLayout(1, false));
        scrollComposite.setMinSize(AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_WIDTH,
            AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_HEIGHT);
        scrollComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        scrollComposite.setExpandHorizontal(true);
        scrollComposite.setExpandVertical(true);

        groupsTable = new TableViewer(scrollComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        groupsTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        groupsTable.getTable().addKeyListener(new GroupsListKeyListener());

        groupsTable.setContentProvider(new GroupsListContentProvider());
        groupsTable.setLabelProvider(new AuthorizationLabelProvider());
        groupsTable.setInput(authorizationService.getDefaultAuthorizationObjects().accessGroupPublicInLocalNetwork());
        groupsTable.setComparator(comparator);
        scrollComposite.setContent(groupsTable.getControl());

        createActions();
        fillLocalToolbar(toolbar);

        Composite buttonsComposite = new Composite(composite, SWT.NONE);
        GridLayout buttonsCompositeLayout = new GridLayout(1, true);
        buttonsComposite.setLayout(buttonsCompositeLayout);
        GridData buttonsCompositeData = new GridData(SWT.LEFT, SWT.TOP, false, true);
        buttonsComposite.setLayoutData(buttonsCompositeData);

        ActionContributionItem createGroupContribution = new ActionContributionItem(createGroupAction);
        createGroupContribution.fill(buttonsComposite);

        Button createGroupButton = (Button) createGroupContribution.getWidget();
        createGroupButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ActionContributionItem deleteGroupContribution = new ActionContributionItem(deleteGroupAction);
        deleteGroupContribution.fill(buttonsComposite);

        Button deleteGroupButton = (Button) deleteGroupContribution.getWidget();
        deleteGroupButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ActionContributionItem importKeyContribution = new ActionContributionItem(importGroupKeyAction);
        importKeyContribution.fill(buttonsComposite);

        Button importGroupButton = (Button) importKeyContribution.getWidget();
        importGroupButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ActionContributionItem exportKeyContribution = new ActionContributionItem(exportGroupKeyAction);
        exportKeyContribution.fill(buttonsComposite);
        Button exportGroupButton = (Button) exportKeyContribution.getWidget();
        exportGroupButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label label = new Label(composite, SWT.None);
        label.setText(
            "Note: Authorization groups for publishing components over\nUplink Connections must have the name prefix 'external_'.");

        registerChangeListeners();

        hookContextMenu();

        scrollComposite.setFocus();
        return composite;
    }

    private void fillLocalToolbar(ToolBar toolbar) {
        new ToolbarFilterWidget(groupsTable, "Filter groups...").fill(toolbar, 0);
        new ActionContributionItem(sortActionAscending).fill(toolbar, 1);
        new ActionContributionItem(sortActionDescending).fill(toolbar, 2);
    }

    private void createActions() {
        sortActionAscending = new ListSortAction(AuthorizationGroupViewerComparator.ASCENDING);
        sortActionAscending.setImageDescriptor(AuthorizationConstants.SORT_ASC);
        sortActionAscending.setText("Sort alphabetically ascending");
        sortActionDescending = new ListSortAction(AuthorizationGroupViewerComparator.DESCENDING);
        sortActionDescending.setImageDescriptor(AuthorizationConstants.SORT_DESC);
        sortActionDescending.setText("Sort alphabetically descending");
        importGroupKeyAction = new ImportGroupKeyAction(authorizationService);
        exportGroupKeyAction = new ExportGroupKeyAction(groupsTable, authorizationService);
        exportGroupKeyAction.setEnabled(false);
        createGroupAction = new CreateGroupAction();
        deleteGroupAction = new DeleteGroupAction(groupsTable);
        deleteGroupAction.setEnabled(false);
        showIdAction = new ShowAuthorizationGroupIdAction(groupsTable);
    }

    private void hookContextMenu() {
        final MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(groupsTable.getTable());
        menuManager.add(sortActionAscending);
        menuManager.add(sortActionDescending);
        menuManager.add(new Separator());
        menuManager.add(createGroupAction);
        menuManager.add(deleteGroupAction);
        menuManager.add(importGroupKeyAction);
        menuManager.add(exportGroupKeyAction);
        menuManager.add(new Separator());
        menuManager.add(showIdAction);

        groupsTable.getTable().setMenu(menu);
    }

    /**
     * Content Provider for the authorization group list.
     *
     * @author Oliver Seebach
     */
    private final class GroupsListContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object object) {
            java.util.List<AuthorizationAccessGroup> groups = new ArrayList<>();
            for (AuthorizationAccessGroup group : localComponentRegistrationService.listAvailableAuthorizationAccessGroups()) {
                groups.add(group);
            }
            return groups.toArray();
        }
    }

    /**
     * Key listener for the authorization group list.
     *
     * @author Oliver Seebach
     */
    private final class GroupsListKeyListener extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent event) {
            if (event.keyCode == SWT.DEL && deleteGroupAction.isEnabled()) {
                deleteGroupAction.run();
            }
        }
    }

    /**
     * Action class to create an authorization group.
     *
     * @author Oliver Seebach
     */
    private final class CreateGroupAction extends Action {

        private CreateGroupAction() {
            super("Create Group...");
        }

        @Override
        public void run() {
            final Display display = Display.getDefault();
            Shell shell = display.getActiveShell();
            CreateGroupDialog createGroupDialog = new CreateGroupDialog(shell, AuthorizationGroupDialog.this);
            int id = createGroupDialog.open();
            if (id == 0) {
                try {
                    authorizationService.createLocalGroup(groupNameForCreation);
                } catch (OperationFailureException e) {
                    log.warn("Failed to create authorization group '" + groupNameForCreation + APOSTROPHE + DOT);
                }
            }
        }
    }

    /**
     * Action class to delete an authorization group.
     *
     * @author Oliver Seebach
     */
    private final class DeleteGroupAction extends SelectionProviderAction {

        private DeleteGroupAction(ISelectionProvider selectionProvider) {
            super(selectionProvider, "Delete Group...");
        }

        @Override
        public void run() {

            IStructuredSelection selection = getStructuredSelection();

            if (selection.isEmpty()) {
                return;
            }

            MessageBox warning = new MessageBox(getParentShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
            StringBuilder message = new StringBuilder("Do you really want to delete ");
            if (selection.size() == 1 && selection.getFirstElement() instanceof AuthorizationAccessGroup) {
                message
                    .append(StringUtils.format("group '%s'", ((AuthorizationAccessGroup) selection.getFirstElement()).getDisplayName()));
            } else {
                message.append("the selected groups");
            }
            message.append("?\nPlease note that all associations to components will be discarded, too.");
            warning.setMessage(message.toString());
            warning.setText("Delete Group");
            int id = warning.open();
            if (id == SWT.OK) {
                Iterator<?> i = selection.iterator();
                i.forEachRemaining(o -> {
                    if (o instanceof AuthorizationAccessGroup) {
                        authorizationService.deleteLocalGroupData((AuthorizationAccessGroup) o);
                    }
                });
            }

        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            if (!selection.isEmpty()) {
                setEnabled(true);
                Iterator<?> i = selection.iterator();
                i.forEachRemaining(o -> {
                    if (o instanceof AuthorizationAccessGroup && authorizationService.isPublicAccessGroup((AuthorizationAccessGroup) o)) {
                        setEnabled(false);
                    }
                });
                return;
            }
            setEnabled(false);
        }
    }

    /**
     * Dialog to create a group.
     *
     * @author Oliver Seebach
     */
    private class CreateGroupDialog extends Dialog {

        private static final int VALIDATION_LABEL_HEIGHT_HINT = 55;

        private AuthorizationGroupDialog view = null;

        private Text groupNameTextfield;

        protected CreateGroupDialog(Shell parentShell, AuthorizationGroupDialog view) {
            super(parentShell);
            this.view = view;
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Create Authorization Group");
        }

        @Override
        protected void okPressed() {
            view.groupNameForCreation = groupNameTextfield.getText();
            super.okPressed();
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            GridLayout containerLayout = new GridLayout(2, false);
            container.setLayout(containerLayout);

            Label groupNameLabel = new Label(container, SWT.NULL);
            groupNameLabel.setText("Group name: ");

            groupNameTextfield = new Text(container, SWT.BORDER);
            GridData groupNameGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
            groupNameGridData.widthHint = DEFAULT_TEXTFIELD_WIDTH;
            groupNameTextfield.setLayoutData(groupNameGridData);

            Composite validationMessage = new Composite(container, SWT.NONE);
            GridData validationMessageGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
            validationMessageGridData.horizontalSpan = 2;
            validationMessage.setLayoutData(validationMessageGridData);
            validationMessage.setLayout(new GridLayout(2, false));
            validationMessage.setVisible(false);
            Label validationIconLabel = new Label(validationMessage, SWT.NONE);
            validationIconLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16));
            validationIconLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, true));
            Label validationTextLabel = new Label(validationMessage, SWT.WRAP);
            GridData validationLabelGridData = new GridData(SWT.FILL, SWT.NONE, true, false);
            validationLabelGridData.widthHint = DEFAULT_TEXTFIELD_WIDTH;
            validationLabelGridData.heightHint = VALIDATION_LABEL_HEIGHT_HINT;
            validationTextLabel.setLayoutData(validationLabelGridData);

            groupNameTextfield.addModifyListener(event -> {
                String input = groupNameTextfield.getText();
                Optional<String> validationError = AuthorizationIdRules.validateAuthorizationGroupId(input);
                if (!input.isEmpty() && validationError.isPresent()) {
                    validationTextLabel.setText(
                        StringUtils.format("The chosen group name is not valid.\n%s.", validationError.get().replaceAll("&", "&&")));
                    validationMessage.setVisible(true);
                } else {
                    validationTextLabel.setText("");
                    validationMessage.setVisible(false);
                }
                getButton(IDialogConstants.OK_ID).setEnabled(!validationError.isPresent());
            });

            return container;
        }
    }

    /**
     * List sorting action.
     *
     * @author Jan Flink
     */
    private final class ListSortAction extends Action {

        private int direction;

        protected ListSortAction(int direction) {
            super();
            this.direction = direction;
            setChecked(comparator.getDirection() == direction);
        }

        @Override
        public void run() {
            comparator.setDirection(direction);
            groupsTable.refresh();
            sortActionAscending.setChecked(direction == AuthorizationGroupViewerComparator.ASCENDING);
            sortActionDescending.setChecked(direction == AuthorizationGroupViewerComparator.DESCENDING);
        }

    }
}
