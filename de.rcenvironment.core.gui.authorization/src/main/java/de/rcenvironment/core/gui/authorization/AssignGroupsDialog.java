/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog to associate components with groups.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 */
public class AssignGroupsDialog extends TitleAreaDialog {

    private static final int MINUS_ONE = -1;

    private static final int LEFT_BUTTON = 1;

    private Map<AuthorizationAccessGroup, Boolean> groupToStateMapping;

    private AuthorizationPermissionSet permissionSet;

    private Button publicButton;

    private Button localButton;

    private Button customButton;

    private PublishingType type;

    private String componentName;

    private CheckboxTableViewer groupTableViewer;

    private ListSortAction sortActionAscending;

    private ListSortAction sortActionDescending;

    private AuthorizationGroupViewerComparator comparator;

    protected AssignGroupsDialog(Shell parentShell,
        Map<AuthorizationAccessGroup, Boolean> groupToStateMapping,
        AuthorizationPermissionSet permissionSet, String componentName) {
        super(parentShell);
        this.groupToStateMapping = groupToStateMapping;
        this.permissionSet = permissionSet;
        this.componentName = componentName;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Assign Groups");
        setMessage(StringUtils.format("Assign groups to component '%s':", componentName), IMessageProvider.INFORMATION);
    }

    @Override
    protected void okPressed() {
        groupToStateMapping.clear();
        List<Object> checkedElements = Arrays.asList(groupTableViewer.getCheckedElements());
        for (TableItem tableItem : groupTableViewer.getTable().getItems()) {
            final AuthorizationAccessGroup group = (AuthorizationAccessGroup) tableItem.getData();
            groupToStateMapping.put(group, checkedElements.contains(group));
        }

        if (publicButton.getSelection()) {
            type = PublishingType.PUBLIC;
        } else if (localButton.getSelection()) {
            type = PublishingType.LOCAL;
        } else if (customButton.getSelection()) {
            type = PublishingType.CUSTOM;
        }
        super.okPressed();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        Composite content = new Composite(container, SWT.NONE);
        content.setLayout(new GridLayout(2, true));
        GridData contentData = new GridData(GridData.FILL_BOTH);
        contentData.heightHint = AuthorizationConstants.DIALOG_HEIGHT_HINT;
        content.setLayoutData(contentData);

        Group groupsGroup = new Group(content, SWT.NONE);
        GridLayout groupsGroupGridLayout = new GridLayout(3, false);
        groupsGroup.setLayout(groupsGroupGridLayout);
        GridData groupsGroupGridData = new GridData();
        groupsGroupGridData.grabExcessHorizontalSpace = true;
        groupsGroupGridData.grabExcessVerticalSpace = false;
        groupsGroup.setLayoutData(groupsGroupGridData);
        groupsGroup.setText("Group Assignment Type");

        localButton = new Button(groupsGroup, SWT.RADIO);
        localButton.setText("Local");
        customButton = new Button(groupsGroup, SWT.RADIO);
        customButton.setText("Custom");
        publicButton = new Button(groupsGroup, SWT.RADIO);
        publicButton.setText("Public Access");

        ToolBar toolbar = new ToolBar(content, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
        GridData toolbarGridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
        toolbar.setLayoutData(toolbarGridData);

        ScrolledComposite scrolledComposite = new ScrolledComposite(content, SWT.V_SCROLL);
        scrolledComposite.setLayout(new GridLayout(1, false));
        GridData scrollGridData = new GridData(GridData.FILL_BOTH);
        scrollGridData.horizontalSpan = 2;
        scrolledComposite.setLayoutData(scrollGridData);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        scrolledComposite.setMinSize(AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_WIDTH,
            AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_HEIGHT);

        groupTableViewer =
            CheckboxTableViewer.newCheckList(scrolledComposite, SWT.BORDER | SWT.V_SCROLL | SWT.NO_FOCUS | SWT.FULL_SELECTION);
        groupTableViewer.getTable().setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        groupTableViewer.getTable().setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
        groupTableViewer.setContentProvider(new GroupsTableContentProvider());
        groupTableViewer.setCheckStateProvider(new GroupsCheckStateProvider());
        groupTableViewer.setLabelProvider(new AuthorizationLabelProvider());
        groupTableViewer.setInput(groupToStateMapping.keySet());
        comparator = new AuthorizationGroupViewerComparator();
        groupTableViewer.setComparator(comparator);
        groupTableViewer.getTable().addListener(SWT.MouseUp, event -> {
            int index = groupTableViewer.getTable().getSelectionIndex();
            groupTableViewer.setSelection(StructuredSelection.EMPTY);
            if (event.button != LEFT_BUTTON || event.detail == SWT.CHECK || index == MINUS_ONE
                || !groupTableViewer.getTable().getItem(index).getBounds().contains(new Point(event.x, event.y))) {
                return;
            }
            groupTableViewer.setChecked(groupTableViewer.getElementAt(index),
                !groupTableViewer.getChecked(groupTableViewer.getElementAt(index)));
        });
        scrolledComposite.setContent(groupTableViewer.getControl());

        makeAction();
        fillLocalToolbar(toolbar);

        // handle en-/disabling of group assignment checkboxes
        customButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (customButton.getSelection()) {
                    groupTableViewer.getTable().setEnabled(true);
                    toolbar.setEnabled(true);
                } else {
                    groupTableViewer.getTable().setEnabled(false);
                    toolbar.setEnabled(false);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        // radio button initial state setting
        if (permissionSet.isLocalOnly()) {
            // local
            localButton.setSelection(true);
            groupTableViewer.getTable().setEnabled(false);
            toolbar.setEnabled(false);
        } else if (permissionSet.isPublic()) {
            // public
            publicButton.setSelection(true);
            groupTableViewer.getTable().setEnabled(false);
            toolbar.setEnabled(false);
        } else {
            if (groupToStateMapping.isEmpty()) {
                // empty -> local
                localButton.setSelection(true);
                groupTableViewer.getTable().setEnabled(false);
                toolbar.setEnabled(false);
            } else {
                // custom
                customButton.setSelection(true);
                groupTableViewer.getTable().setEnabled(true);
                toolbar.setEnabled(true);
            }
        }

        scrolledComposite.setFocus();
        return container;
    }

    private void fillLocalToolbar(ToolBar toolbar) {
        new ToolbarFilterWidget(groupTableViewer, "Filter groups...").fill(toolbar, 0);
        new ActionContributionItem(sortActionAscending).fill(toolbar, 1);
        new ActionContributionItem(sortActionDescending).fill(toolbar, 2);
    }

    private void makeAction() {
        sortActionAscending = new ListSortAction(AuthorizationConstants.ASCENDING);
        sortActionAscending.setImageDescriptor(AuthorizationConstants.SORT_ASC);
        sortActionAscending.setText("Sort alphabetically ascending");
        sortActionDescending = new ListSortAction(AuthorizationConstants.DESCENDING);
        sortActionDescending.setImageDescriptor(AuthorizationConstants.SORT_DESC);
        sortActionDescending.setText("Sort alphabetically descending");
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
            groupTableViewer.refresh();
            sortActionAscending.setChecked(direction == AuthorizationConstants.ASCENDING);
            sortActionDescending.setChecked(direction == AuthorizationConstants.DESCENDING);
        }

    }

    public PublishingType getType() {
        return type;
    }

    /**
     * Content provider for groups table.
     *
     * @author Jan Flink
     */
    public class GroupsTableContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object arg0) {
            ArrayList<AuthorizationAccessGroup> groups = new ArrayList<>();
            for (AuthorizationAccessGroup group : groupToStateMapping.keySet()) {
                groups.add(group);
            }
            return groups.toArray();
        }

    }

    /**
     * Check state provider for groups table.
     *
     * @author Jan Flink
     */
    public class GroupsCheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(Object arg0) {
            return groupToStateMapping.get(arg0);
        }

        @Override
        public boolean isGrayed(Object arg0) {
            return false;
        }

    }
}
