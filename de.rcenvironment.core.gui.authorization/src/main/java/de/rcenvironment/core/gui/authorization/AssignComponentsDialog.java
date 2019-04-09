/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;

import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog to associate components with groups.
 *
 * @author Oliver Seebach
 * @author Robert Mischke
 * @author Jan Flink
 */
public class AssignComponentsDialog extends TitleAreaDialog {

    private static final int MINUS_ONE = -1;

    private Map<NamedComponentAuthorizationSelector, Boolean> originalComponentToStateMapping;

    private Map<NamedComponentAuthorizationSelector, Boolean> changedComponentToStateMappings;

    private String groupName;

    private CheckboxTableViewer componentsTableViewer;

    private AuthorizationService authorizationService;

    private ListSortAction sortActionAscending;

    private ListSortAction sortActionDescending;

    private ComponentsViewerComparator comparator;

    protected AssignComponentsDialog(Shell parentShell, AuthorizationService authorizationService,
        Map<NamedComponentAuthorizationSelector, Boolean> componentToStateMapping, String groupName) {
        super(parentShell);
        this.groupName = groupName;
        this.originalComponentToStateMapping = componentToStateMapping;
        this.changedComponentToStateMappings = new HashMap<>();
        this.authorizationService = authorizationService;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Assign Components");
        setMessage(StringUtils.format("Assign components to group '%s'.", groupName), IMessageProvider.INFORMATION);
    }

    @Override
    protected void okPressed() {
        changedComponentToStateMappings.clear();
        List<Object> checkedElements = Arrays.asList(componentsTableViewer.getCheckedElements());
        for (TableItem tableItem : componentsTableViewer.getTable().getItems()) {
            final NamedComponentAuthorizationSelector selector = (NamedComponentAuthorizationSelector) tableItem.getData();
            final boolean newState = checkedElements.contains(selector);
            final Boolean oldState =
                originalComponentToStateMapping.get(selector) != null && originalComponentToStateMapping.get(selector);

            if (newState != oldState) {
                changedComponentToStateMappings.put(selector, newState);
            }
        }
        super.okPressed();
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite) super.createDialogArea(parent);

        Composite content = new Composite(container, SWT.NONE);
        content.setLayout(new GridLayout(1, true));
        GridData contentData = new GridData(GridData.FILL_BOTH);
        contentData.heightHint = AuthorizationConstants.DIALOG_HEIGHT_HINT;
        content.setLayoutData(contentData);

        ToolBar toolbar = new ToolBar(content, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
        GridData toolbarGridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
        toolbar.setLayoutData(toolbarGridData);

        ScrolledComposite scrolledComposite = new ScrolledComposite(content, SWT.V_SCROLL);
        scrolledComposite.setLayout(new GridLayout(1, false));
        scrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_WIDTH,
            AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_HEIGHT);

        componentsTableViewer =
            CheckboxTableViewer.newCheckList(scrolledComposite,
                SWT.BORDER | SWT.V_SCROLL | SWT.NO_FOCUS | SWT.FULL_SELECTION);
        componentsTableViewer.getTable().setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
        componentsTableViewer.setContentProvider(new ComponentsTableContentProvider());
        componentsTableViewer.setCheckStateProvider(new ComponentCheckStateProvider());
        componentsTableViewer.setLabelProvider(new AuthorizationLabelProvider());
        comparator = new ComponentsViewerComparator();
        componentsTableViewer.setComparator(comparator);
        componentsTableViewer.setInput(originalComponentToStateMapping.keySet());
        componentsTableViewer.getTable().addListener(SWT.Selection, event -> {
            int index = componentsTableViewer.getTable().getSelectionIndex();
            componentsTableViewer.setSelection(StructuredSelection.EMPTY);
            if (event.detail == SWT.CHECK || index == MINUS_ONE) {
                return;
            }
            componentsTableViewer.setChecked(componentsTableViewer.getElementAt(index),
                !componentsTableViewer.getChecked(componentsTableViewer.getElementAt(index)));
        });
        scrolledComposite.setContent(componentsTableViewer.getControl());

        makeAction();
        fillLocalToolbar(toolbar);

        String publicGroupName =
            authorizationService.getDefaultAuthorizationObjects().accessGroupPublicInLocalNetwork().getDisplayName();

        if (!groupName
            .equals(publicGroupName)) {
            Label note = new Label(content, SWT.FILL);
            note.setText(StringUtils.format(
                "Note:\nComponents that are member of the group \"%s\" are not shown in this dialog!", publicGroupName));
        }
        scrolledComposite.setFocus();
        return container;
    }

    private void fillLocalToolbar(ToolBar toolbar) {
        new ToolbarFilterWidget(componentsTableViewer, "Filter components...").fill(toolbar, 0);
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
            componentsTableViewer.refresh();
            sortActionAscending.setChecked(direction == AuthorizationConstants.ASCENDING);
            sortActionDescending.setChecked(direction == AuthorizationConstants.DESCENDING);
        }

    }

    public Map<NamedComponentAuthorizationSelector, Boolean> getComponentToStateMapping() {
        return changedComponentToStateMappings;
    }

    /**
     * Check state provider for the components list.
     *
     * @author Jan Flink
     */
    private final class ComponentCheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(Object arg0) {
            return originalComponentToStateMapping.get(arg0);
        }

        @Override
        public boolean isGrayed(Object arg0) {
            return false;
        }

    }

    /**
     * Content Provider for the components table.
     *
     * @author Jan Flink
     */
    private final class ComponentsTableContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object object) {

            ArrayList<NamedComponentAuthorizationSelector> components = new ArrayList<>();
            for (NamedComponentAuthorizationSelector component : originalComponentToStateMapping.keySet()) {
                components.add(component);
            }
            return components.toArray();
        }
    }

    
    /**
     * Comparator for component tree.
     *
     * @author Jan Flink
     */
    public class ComponentsViewerComparator extends ViewerComparator {

        /**
         * Sort direction alphabetically descending.
         */
        public static final int DESCENDING = MINUS_ONE;

        /**
         * Sort direction alphabetically ascending.
         */
        public static final int ASCENDING = 1;

        private int direction = ASCENDING;

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public int getDirection() {
            return direction;
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (e1 instanceof NamedComponentAuthorizationSelector && e2 instanceof NamedComponentAuthorizationSelector) {
                NamedComponentAuthorizationSelector component1 = (NamedComponentAuthorizationSelector) e1;
                NamedComponentAuthorizationSelector component2 = (NamedComponentAuthorizationSelector) e2;
                int returncode = component1.compareToIgnoreCase(component2);
                if (direction == DESCENDING) {
                    returncode = -returncode;
                }
                return returncode;
            }

            return super.compare(viewer, e1, e2);
        }
    }
}
