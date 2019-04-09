/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage.Type;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.Activator;

/**
 * 
 * A message dialog displaying a list of error and warning messages.
 *
 * @author Jascha Riedel
 */
public class WorkflowExecutionWizardValidationDialog extends TitleAreaDialog {

    private static final String DIALOG_TITLE = "Validation Report";

    private static final String INDEX = "index";

    private static final String ORDER = "order";

    private static final int MINUS_ONE = -1;

    /**
     * This ensures that the table extends to the end of the window since
     * grabExcessVerticalSpace does not seem to work correctly.
     */
    private static final int MINIMUM_TABLE_SIZE = 10000;

    private Table table;

    /**
     * 
     * Enum to save last order state.
     *
     * @author Jascha Riedel
     */
    private enum OrderState {
        COMPONENT_UP, COMPONENT_DOWN, TYPE_UP, TYPE_DOWN, MESSAGE_UP, MESSAGE_DOWN;
    }

    private final Map<String, List<ComponentValidationMessage>> messagesMap;

    private final WorkflowDescription wfDescription;

    private final PlaceholderPage placeholderPage;

    private final ImageManager imageManger = ImageManager.getInstance();

    public WorkflowExecutionWizardValidationDialog(Shell parentShell,
            Map<String, List<ComponentValidationMessage>> messagesMap, WorkflowDescription wfDescription,
            PlaceholderPage placeholderPage) {
        super(parentShell);
        this.messagesMap = messagesMap;
        this.wfDescription = wfDescription;
        this.placeholderPage = placeholderPage;
    }

    @Override
    protected void setShellStyle(int newShellStyle) {
        super.setShellStyle(newShellStyle | SWT.RESIZE);
    }

    @Override
    public void create() {
        super.create();
        setTitle(DIALOG_TITLE);
        setMessageText();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        area.setLayoutData(new GridData(SWT.FILL, GridData.FILL, true, true));
        Composite container = new Composite(area, SWT.FILL);

        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode()
                .getHeight() * 2 / 5;
        data.widthHint = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode()
                .getWidth() * 2 / 3;
        data.grabExcessVerticalSpace = true;
        container.setLayoutData(data);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;

        container.setLayout(layout);
        initilizeTable(container);
        createColumns();
        fillTable();

        Label horizontalBar = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);

        horizontalBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, 0, Messages.proceedButton, false);
        createButton(parent, 1, Messages.cancelButton, true);
    }

    private void setMessageText() {
        int messageCount = 0;
        for (String key : messagesMap.keySet()) {
            messageCount += messagesMap.get(key).size();
            if (messageCount > 1) {
                break;
            }
        }
        for (String key : placeholderPage.getPlaceholderValidators().keySet()) {
            if (messageCount > 1) {
                break;
            }
            messageCount += placeholderPage.getPlaceholderValidators().get(key).size();
        }
        boolean hasErrorMessage = false;
        for (String key : messagesMap.keySet()) {
            for (ComponentValidationMessage message : messagesMap.get(key)) {
                if (message.getType() == ComponentValidationMessage.Type.ERROR) {
                    hasErrorMessage = true;
                    break;
                }
            }
            if (hasErrorMessage) {
                break;
            }
        }

        String titleText;
        if (messageCount > 1) {
            titleText = Messages.validationProblems;
        } else {
            titleText = Messages.validationProblem;
        }
        if (hasErrorMessage) {
            setMessage(titleText, IMessageProvider.ERROR);
        } else {
            setMessage(titleText, IMessageProvider.WARNING);
        }
    }

    private void initilizeTable(Composite parent) {
        table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.verticalAlignment = GridData.CENTER;
        data.grabExcessVerticalSpace = true;
        data.heightHint = MINIMUM_TABLE_SIZE;

        table.setLayoutData(data);

        table.addKeyListener(new TableCopyKeyListener());

        table.addMouseListener(new TableCopyMouseListener());

    }

    private void createColumns() {
        TableColumn componentColumn = new TableColumn(table, SWT.NONE);
        componentColumn.setText("Component");
        componentColumn.setData(INDEX, 0);
        componentColumn.addSelectionListener(new SortColumnListener());
        componentColumn.setData(ORDER, OrderState.COMPONENT_UP);

        table.notifyListeners(SWT.Selection, new Event());

        TableColumn typeColumn = new TableColumn(table, SWT.NONE);
        typeColumn.setText("Type");
        typeColumn.setData(INDEX, 1);
        typeColumn.addSelectionListener(new SortColumnListener());
        typeColumn.setData(ORDER, OrderState.TYPE_UP);

        TableColumn messageColumn = new TableColumn(table, SWT.NONE);
        messageColumn.setText("Message");
        messageColumn.setData(INDEX, 2);
        messageColumn.addSelectionListener(new SortColumnListener());
        messageColumn.setData(ORDER, OrderState.MESSAGE_UP);
    }

    private void fillTable() {

        for (String nodeIdentifier : messagesMap.keySet()) {

            WorkflowNode currentNode = null;
            for (WorkflowNode node : wfDescription.getWorkflowNodes()) {
                if (node.getIdentifier().equals(nodeIdentifier)) {
                    currentNode = node;
                    break;
                }
            }
            if (currentNode == null) {
                continue;
            }
            String nodeName = currentNode.getName();
            if (placeholderPage.getPlaceholderValidators().containsKey(nodeName)) {
                for (String placeholderName : placeholderPage.getPlaceholderValidators().get(nodeName)) {
                    String dataType = placeholderPage.placeholderHelper.getPlaceholdersDataType().get(nodeName + "." + placeholderName);
                    if (dataType != null && dataType.equals("text")) {
                        addMessageItem(currentNode, Type.ERROR, Messages.textExceedsMaxLength + placeholderName);
                    } else {
                        addMessageItem(currentNode, Type.ERROR, Messages.missingPlaceholder + placeholderName); 
                    }
                }
            }
            for (ComponentValidationMessage message : messagesMap.get(nodeIdentifier)) {
                addMessageItem(currentNode, message.getType(), message.getAbsoluteMessage());
            }
        }

        new SortColumnListener().intialSort();

        table.getColumn(0).pack();
        table.getColumn(1).pack();
        table.getColumn(2).pack();

    }

    private Image getImage(WorkflowNode element) {
        byte[] icon = element.getComponentDescription().getIcon16();
        Image image;
        if (icon != null) {
            image = new Image(Display.getCurrent(), new ByteArrayInputStream(icon));
        } else {
            image = Activator.getInstance().getImageRegistry().get(Activator.IMAGE_RCE_ICON_16);
        }
        return image;
    }

    private void addMessageItem(WorkflowNode node, ComponentValidationMessage.Type type, String message) {
        TableItem item = new TableItem(table, SWT.NONE);

        item.setImage(0, getImage(node));
        item.setText(0, node.getName());

        switch (type) {
        case ERROR:
            item.setImage(1, imageManger.getSharedImage(StandardImages.ERROR_16));
            item.setText(1, type.toString());
            break;
        case WARNING:
            item.setImage(1, imageManger.getSharedImage(StandardImages.WARNING_16));
            item.setText(1, type.toString());
            break;
        default:
            item.setImage(1, imageManger.getSharedImage(StandardImages.ERROR_16));
            item.setText(1, Type.ERROR.toString());
            break;
        }

        item.setText(2, message);

    }

    private String buildCopyString(TableItem[] tableItems) {
        String returnString = "";
        for (int i = 0; i < tableItems.length; i++) {
            returnString += tableItems[i].getText(0);
            returnString += " - ";
            returnString += tableItems[i].getText(1);
            returnString += " - ";
            returnString += tableItems[i].getText(2);
            returnString += "\n";
        }
        return returnString;
    }

    /**
     * 
     * Table copy key listener.
     *
     * @author Jascha Riedel
     */
    private class TableCopyKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent arg0) {
            if (arg0.getSource() instanceof Table) {
                if (arg0.stateMask == SWT.CTRL && arg0.keyCode == 'c') {
                    TableItem[] tableItems = ((Table) arg0.getSource()).getSelection();
                    Clipboard clipboard = new Clipboard(getShell().getDisplay());
                    TextTransfer textTransfer = TextTransfer.getInstance();
                    String copyString = buildCopyString(tableItems);
                    clipboard.setContents(new String[] { copyString }, new Transfer[] { textTransfer });
                    clipboard.dispose();
                }
            }

        }

        @Override
        public void keyReleased(KeyEvent arg0) {
        }

    }

    /**
     * 
     * Table Copy Mouse Listener.
     *
     * @author Jascha Riedel
     */
    private class TableCopyMouseListener implements MouseListener {

        @Override
        public void mouseDoubleClick(MouseEvent arg0) {
        }

        @Override
        public void mouseDown(MouseEvent event) {

            if (event.button == 3) {
                final Menu menu = new Menu(table);
                MenuItem newItem = new MenuItem(menu, SWT.NONE);
                newItem.setText("Copy");
                newItem.setImage(imageManger.getSharedImage(StandardImages.COPY_16));
                menu.setVisible(true);
                newItem.addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent arg0) {
                    }

                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        TableItem[] tableItems = table.getSelection();
                        Clipboard clipboard = new Clipboard(getShell().getDisplay());
                        TextTransfer textTransfer = TextTransfer.getInstance();
                        String copyString = buildCopyString(tableItems);
                        clipboard.setContents(new String[] { copyString }, new Transfer[] { textTransfer });
                        clipboard.dispose();
                        menu.dispose();
                    }

                });
            }

        }

        @Override
        public void mouseUp(MouseEvent arg0) {
        }
    }

    /**
     * Sort Column Listener.
     * 
     * @author Jascha Riedel
     */
    private class SortColumnListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {

            TableColumn tableColumn = (TableColumn) arg0.getSource();
            sortColumn(tableColumn);

        }

        public void intialSort() {
            sortColumn(table.getColumn(0));
        }

        private void sortColumn(TableColumn tableColumn) {
            int index = (int) tableColumn.getData(INDEX);
            final int orderdir;

            orderdir = setOrderDirFromTableColumn(tableColumn);

            List<Object[]> list = new ArrayList<Object[]>();

            Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
            TableItem[] items = table.getItems();

            for (int i = 0; i < items.length; i++) {
                list.add(new Object[] { collator.getCollationKey(items[i].getText(index)), getItemData(items[i]) });
            }

            Collections.sort(list, new Comparator<Object[]>() {

                @Override
                public int compare(Object[] arg0, Object[] arg1) {
                    return orderdir * ((CollationKey) arg0[0]).compareTo((CollationKey) arg1[0]);
                }
            });

            for (int i = 0; i < items.length; i++) {
                setItemData(items[i], (Object[]) list.get(i)[1]);
            }
        }

        private int setOrderDirFromTableColumn(TableColumn tableColumn) {
            int orderdir;
            OrderState orderState = (OrderState) tableColumn.getData(ORDER);
            switch ((Integer) tableColumn.getData(INDEX)) {
            case 0:
                switch (orderState) {
                case COMPONENT_UP:
                    orderdir = 1;
                    tableColumn.setData(ORDER, OrderState.COMPONENT_DOWN);
                    break;
                case COMPONENT_DOWN:
                    orderdir = MINUS_ONE;
                    tableColumn.setData(ORDER, OrderState.COMPONENT_UP);
                    break;
                default:
                    orderdir = 1;
                    tableColumn.setData(ORDER, OrderState.COMPONENT_DOWN);
                    break;
                }
                break;
            case 1:
                switch (orderState) {
                case TYPE_UP:
                    orderdir = 1;
                    tableColumn.setData(ORDER, OrderState.TYPE_DOWN);
                    break;
                case TYPE_DOWN:
                    orderdir = MINUS_ONE;
                    tableColumn.setData(ORDER, OrderState.TYPE_UP);
                    break;
                default:
                    orderdir = 1;
                    tableColumn.setData(ORDER, OrderState.TYPE_DOWN);
                    break;
                }
                break;
            case 2:
                switch (orderState) {
                case MESSAGE_UP:
                    orderdir = 1;
                    tableColumn.setData(ORDER, OrderState.MESSAGE_DOWN);
                    break;
                case MESSAGE_DOWN:
                    orderdir = MINUS_ONE;
                    tableColumn.setData(ORDER, OrderState.MESSAGE_UP);
                    break;
                default:
                    orderdir = 1;
                    tableColumn.setData(ORDER, OrderState.MESSAGE_DOWN);
                    break;
                }
                break;
            default:
                orderdir = 1;
                tableColumn.setData(ORDER, OrderState.COMPONENT_DOWN);
                break;
            }
            return orderdir;
        }

        private Object[] getItemData(TableItem item) {
            return new Object[] { item.getText(0), item.getText(1), item.getText(2), item.getImage(0), item.getImage(1),
                    item.getImage(2) };
        }

        private void setItemData(TableItem item, Object[] object) {
            String[] values = { (String) object[0], (String) object[1], (String) object[2] };
            Image[] images = { (Image) object[3], (Image) object[4], (Image) object[5] };
            item.setText(values);
            item.setImage(images);
        }

    }

}
