/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.xpathchooser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.xml.sax.SAXException;

import de.rcenvironment.core.utils.common.variables.legacy.VariableType;
import de.rcenvironment.cpacs.gui.xpathchooser.model.XSDElement;
import de.rcenvironment.cpacs.gui.xpathchooser.model.XSDGenerator;

/**
 * The GUI creating class used in dialogs and views.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XPathChooserHelper {

    /**
     * For sash creation.
     */
    static final int PERCENT_100 = 100;

    /**
     * For sash creation.
     */
    private static final int PERCENT_SEP = 70;

    /**
     * Constant for row detection.
     */
    private static final int NOT_FOUND = -1;

    /**
     * Number of columns in the cpacs tree table.
     */
    private static final int TREE_COLUMNS = 3;

    /**
     * Number of columns in the variables table.
     */
    private static final int TABLE_COLUMNS = 1;

    /**
     * The tree table containing the cpacs.
     */
    private Tree tree;

    /**
     * The JFace mechanism to provide custom contents.
     */
    private TreeViewer treeViewer;

    /**
     * The table containing defined variables.
     */
    private Table table;

    /**
     * The table viewer.
     */
    private TableViewer tableViewer;

    /**
     * The loaded and parsed CPACS.
     */
    private XSDElement doc;

    /**
     * The "input" model for the table viewer.
     */
    private List<VariableEntry> tableModel;

    /**
     * The "last line" entry.
     */
    private VariableEntry newEntry;

    /**
     * Remove a line or chart.
     */
    private Image imageRemove;

    /**
     * The allowed number of selections in the list.
     */
    private int maxEntries = 1;

    /**
     * Create the view.
     * 
     * @param root The XML document root
     */
    public XPathChooserHelper(final XSDElement root) {
        doc = root;
    }

    /**
     * Create the view's contents.
     * 
     * @param parent The parent component (e.g. shell, dialog)
     * @return The created contents
     */
    public Composite createContents(final Composite parent) {
        if (getClass().getResourceAsStream("/resources/images/remove16.gif") == null) {
            imageRemove = new Image(parent.getDisplay(), "./resources/images/remove16.gif");
        } else {
            imageRemove = new Image(parent.getDisplay(), getClass().getResourceAsStream("/resources/images/remove16.gif"));
        }
        parent.setLayout(new FormLayout());
        final Group firstGroup = new Group(parent, SWT.None);
        firstGroup.setText("CPACS");
        firstGroup.setLayout(new FillLayout());
        createTree(firstGroup);
        final Sash sash = new Sash(parent, SWT.HORIZONTAL);
        final Group secondGroup = new Group(parent, SWT.None);
        secondGroup.setText("Variables");
        secondGroup.setLayout(new FillLayout());
        createTable(secondGroup);

        // set layout data to allow sash
        final FormData firstData = new FormData();
        firstData.top = new FormAttachment(/* numerator */0, /* offset */0);
        firstData.bottom = new FormAttachment(sash, 0);
        firstData.left = new FormAttachment(0, 0);
        firstData.right = new FormAttachment(PERCENT_100, 0);
        firstGroup.setLayoutData(firstData);
        final FormData sashData = new FormData();
        sashData.top = new FormAttachment(PERCENT_SEP, 0);
        sashData.left = new FormAttachment(0, 0);
        sashData.right = new FormAttachment(PERCENT_100, 0);
        sash.setLayoutData(sashData);
        final FormData secondData = new FormData();
        secondData.top = new FormAttachment(sash, 0);
        secondData.bottom = new FormAttachment(PERCENT_100, 0);
        secondData.left = new FormAttachment(0, 0);
        secondData.right = new FormAttachment(PERCENT_100, 0);
        secondGroup.setLayoutData(secondData);

        // make sash movable
        sash.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent event) {
                ((FormData) sash.getLayoutData()).top = new FormAttachment(0, event.y);
                sash.getParent().layout();
            }
        });

        return secondGroup; // to allow further attachments
    }

    /**
     * Create the left/top part of the dialog, the tree table containing the CPACS.
     * 
     * @param parent The parent component to add the child to.
     */
    private void createTree(final Composite parent) {
        tree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        tree.setDragDetect(true);
        treeViewer = new TreeViewer(tree);
        final String[] columnNames = new String[] { "Element", "UID Name", "Values" };
        final int[] columnWidths = new int[] { 300, 100, 100 };
        for (int c = 0; c < TREE_COLUMNS; c++) {
            final TreeViewerColumn tvc = new TreeViewerColumn(treeViewer, SWT.None);
            tvc.getColumn().setText(columnNames[c]);
            tvc.getColumn().setWidth(columnWidths[c]);
            tvc.getColumn().setResizable(true);
            tvc.getColumn().setMoveable(true);
            if (c >= 1) { // ignore column "tree"
                tvc.setEditingSupport(new XSDEditingSupport(this, treeViewer, c));
            }
        }
        treeViewer.setContentProvider(new XSDContentProvider());
        treeViewer.setLabelProvider(new XSDLabelProvider(parent.getDisplay()));
        fillTree();

        // set up drag facilities
        final DragSource dragSource = new DragSource(tree, DND.DROP_COPY);
        dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        dragSource.addDragListener(new DragSourceListener() {

            /**
             * Detect if drag allowed from the location.
             * 
             * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
             */
            @Override
            public void dragStart(final DragSourceEvent event) {
                if (tree.getSelectionCount() == 0) {
                    event.doit = false;
                    return;
                }
                final TreeItem item = tree.getSelection()[0]; // single selection
                if (item == null) {
                    event.doit = false;
                    return;
                }
                final Point clickLocation = new Point(event.x, event.y);
                final int column = getTreeColumnFromClickLocation(item, clickLocation);
                if (column > 0) {
                    event.doit = false;
                    return;
                }
            }

            /**
             * Set the transferable data.
             * 
             * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
             */
            @Override
            public void dragSetData(final DragSourceEvent event) {
                if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                    if (tree.getSelectionCount() == 0) {
                        return;
                    }
                    final TreeItem item = tree.getSelection()[0]; // single selection
                    if (item == null) {
                        return;
                    }
                    final VariableEntry var = getVariableFromViewItem(item);
                    event.data = var.toString();
                }
            }

            /**
             * Clean up, e.g. after a MOVE.
             * 
             * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
             */
            @Override
            public void dragFinished(final DragSourceEvent event) {}
        });
    }

    /**
     * Create the right/bottom part of the dialog, the table containing the created variables.
     * 
     * @param parent The parent component to add the child to.
     */
    private void createTable(final Composite parent) {
        table = new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        tableViewer = new TableViewer(table);
        final String[] columnNames = new String[] { "XPath" };
        final int[] columnWidths = new int[] { 600 };
        for (int c = 0; c < TABLE_COLUMNS; c++) {
            final TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.None);
            tvc.getColumn().setText(columnNames[c]);
            tvc.getColumn().setWidth(columnWidths[c]);
            tvc.getColumn().setMoveable(true);
            if (c >= 1) { // ignore column "direction"
                tvc.setEditingSupport(new VariableEditingSupport(this, tableViewer, c));
            }
        }
        tableViewer.setContentProvider(new VariableContentProvider());
        final VariableLabelProvider labelProvider = new VariableLabelProvider(parent.getDisplay());
        tableViewer.setLabelProvider(labelProvider);

        newEntry = fillTable();

        addTableListeners(labelProvider);

        // set up drop facilities
        final DropTarget dropTarget = new DropTarget(table, DND.DROP_COPY | DND.DROP_DEFAULT);
        dropTarget.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        dropTarget.addDropListener(new DropTargetListener() {

            @Override
            public void drop(final DropTargetEvent event) {
                if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    final String text = (String) event.data;
                    final VariableEntry var = new VariableEntry(text);
                    if ((maxEntries == 1) || (tableModel.size() <= maxEntries)) { // if only 1 entry
                                                                                  // allowed, then
                                                                                  // replace it
                        if ((maxEntries == 1) && (tableModel.size() > 1)) {
                            tableModel.remove(tableModel.size() - 2);
                        }
                        tableModel.add(tableModel.size() - 1, var);
                        tableViewer.refresh();
                    }
                }
            }

            @Override
            public void dragEnter(final DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if ((event.operations & DND.DROP_COPY) != 0) {
                        event.detail = DND.DROP_COPY;
                    } else {
                        event.detail = DND.DROP_NONE;
                    }
                } else {
                    event.detail = DND.DROP_NONE;
                }
            }

            @Override
            public void dragOperationChanged(final DropTargetEvent event) {
                dragEnter(event); // it's exactly the same
            }

            @Override
            public void dragOver(final DropTargetEvent event) {
                event.feedback = DND.FEEDBACK_INSERT_BEFORE | DND.FEEDBACK_SCROLL;
            }

            @Override
            public void dropAccept(final DropTargetEvent event) {}

            @Override
            public void dragLeave(DropTargetEvent droptargetevent) {}
        });

        // add popup-context menu
        table.addListener(SWT.MenuDetect, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                final int rowIndex = table.getSelectionIndex();
                final Point clickLocation = new Point(event.x, event.y);
                if ((rowIndex < 0) || ((rowIndex + 1) >= tableModel.size())) {
                    return;
                }
                final Menu popup = new Menu(parent.getShell(), SWT.POP_UP);
                final MenuItem removeItem = new MenuItem(popup, SWT.PUSH);
                removeItem.setText("Remove definition");
                removeItem.setImage(imageRemove);
                removeItem.addListener(SWT.Selection, new Listener() {

                    @Override
                    public void handleEvent(final Event event) {
                        tableModel.remove(rowIndex);
                        tableViewer.refresh();
                    }
                });
                popup.setLocation(clickLocation);
                popup.setVisible(true);
                while (!popup.isDisposed() && popup.isVisible()) {
                    if (!popup.getDisplay().readAndDispatch()) {
                        popup.getDisplay().sleep();
                    }
                }
                popup.dispose();
            }
        });
    }

    /**
     * Fill the tree with the documents' contents.
     */
    private void fillTree() {
        treeViewer.setInput(doc);
        treeViewer.expandAll();
        for (int c = 0; c < TREE_COLUMNS; c++) {
            tree.getColumn(c).pack();
        }
    }

    /**
     * Fill the table with some stuff.
     * 
     * @return The last (empty) entry
     */
    private VariableEntry fillTable() {
        final List<VariableEntry> entries = new ArrayList<VariableEntry>();
        final VariableEntry emptyNewEntry = new VariableEntry(EVariableDirection.Incoming, "", "", VariableType.String);
        entries.add(emptyNewEntry);
        tableViewer.setInput(entries);
        tableModel = entries;
        return emptyNewEntry;
    }

    /**
     * Helper to set all the table listeners.
     * 
     * @param labelProvider l
     */
    private void addTableListeners(final VariableLabelProvider labelProvider) {
        table.addMouseListener(new MouseAdapter() {

            public void mouseDown(final MouseEvent event) {
                if (!(event.button != 1)) {
                    final Point clickLocation = new Point(event.x, event.y);
                    final Point location = getTableIndexFromClickLocation(clickLocation); // TODO
                                                                                          // does
                                                                                          // it
                                                                                          // work?
                    if ((location.x == NOT_FOUND) || (table.getSelectionCount() == 0)) {
                        return;
                    }
                    final TableItem item = table.getSelection()[0]; // single selection
                    if (item == null) {
                        return;
                    }

                    // find column of click
                    int col = 0;
                    for (int i = 1; i < TABLE_COLUMNS; i++) {
                        final Rectangle rect = item.getBounds(i);
                        if (rect.contains(clickLocation)) {
                            col = i;
                            break;
                        }
                    }
                    final int column = col;
                    final VariableEntry entry = tableModel.get(table.getSelectionIndex());
                    if (column == 0) { // direction switch
                        if (entry.getDirection() == EVariableDirection.Incoming) {
                            entry.setDirection(EVariableDirection.Outgoing);
                            item.setText(0, entry.getDirection().toString());
                        } else { // switch the other way around
                            entry.setDirection(EVariableDirection.Incoming);
                            item.setText(0, entry.getDirection().toString());
                        }
                        tableViewer.refresh();
                        return;
                    }
                }
            }
        });
    }

    /**
     * Before editing, determine model value of tree item.
     * 
     * @param element The selected model entry
     * @param column The column to create choices for
     * @return The values
     */
    String[] getAttributeValuesForCurrentTreeItem(final XSDElement element, final int column) {
        assert element != null;
        assert column >= 1;
        if (column == 1) {
            return element.getAttributeNames();
        } else if (column == 2) {
            return element.getAttributeValues();
        }
        return XSDElement.EMPTY_STRING;
    }

    /**
     * Before editing, determine model value of tree item.
     * 
     * @param item The item selected
     * @param column The column to create choices for
     * @return The values
     */
    String[] getAttributeValuesForCurrentTreeItem(final TreeItem item, final int column) {
        final XSDElement element = findModelItemFromViewItem(item);
        return getAttributeValuesForCurrentTreeItem(element, column);
    }

    /**
     * Get the value from the model.
     * 
     * @param element The selected model entry
     * @param column The column to create choices for
     * @return The string
     */
    static String getCurrentElementValue(final XSDElement element, final int column) {
        assert column >= 1;
        if (column == 1) {
            return element.getCurrentAttributeName();
        } else if (column == 2) {
            return element.getCurrentAttributeValue();
        }
        return null; // should never happen
    }

    /**
     * Set the value to the model.
     * 
     * @param element The selected model entry
     * @param column The column to create choices for
     * @param value The string
     */
    static void setCurrentElementValue(final XSDElement element, final int column, final String value) {
        assert column >= 1;
        if (column == 1) {
            element.setCurrentAttributeName(value);
        } else if (column == 2) {
            element.setCurrentAttributeValue(value);
        }
    }

    /**
     * After editing, set the value back to the model.
     * 
     * @param element The selected model entry
     * @param text The string to set
     * @param column The column to create choices for
     */
    static void setElementValue(final XSDElement element, final String text, final int column) {
        assert column >= 1;
        if (column == 1) {
            element.setAttributeName(text);
        } else if (column == 2) {
            element.setAttributeValue(text);
        }
    }

    /**
     * After editing, set the value back to the model.
     * 
     * @param item The item selected
     * @param text The string to set
     * @param column The column to create choices for
     */
    void setElementValue(final TreeItem item, final String text, final int column) {
        final XSDElement element = findModelItemFromViewItem(item);
        setElementValue(element, text, column);
    }

    /**
     * Restrict the number of entries allowed to choose. This is useful for e.g. single-selections.
     * 
     * @param num The allowed number of selections in the list. If 1, then replacing an already set variable is allowed by dropping.
     */
    void setMaximumNumberOfEntries(final int num) {
        maxEntries = num;
    }

    /**
     * Determine path to tree (view) root from a selected/pointed at tree item.
     * 
     * @param item The (sub)item to start from
     * @return The list of tree items from root to leaf
     */
    private List<String> getPathFromViewItems(final TreeItem item) {
        final List<String> path = new LinkedList<String>(); // don't use "Generics.new*" here Why?
                                                            // because we need a linked list?)
        path.add(item.getText());
        TreeItem current = item;
        // traverse upwards to view root
        while (current.getParentItem() != null) {
            current = current.getParentItem();
            path.add(0, current.getText());
        }
        return path;
    }

    /**
     * Determine model entries for a previously calculated view path.
     * 
     * @param path The path
     * @return The model entries
     */
    private List<XSDElement> getModelEntriesFromPath(final List<String> path) {
        final List<XSDElement> elements = new ArrayList<XSDElement>();
        XSDElement element = doc;
        // traverse downwards to model item
        for (final String name : path) {
            for (final XSDElement e : ((XSDElement) element).getElements()) {
                if (e.getName().equals(name)) {
                    element = e;
                    elements.add(element);
                    continue;
                }
            }
        }
        return elements;
    }

    /**
     * Useful helper before and after editing.
     * 
     * @param item The tree item on which any action is performed
     * @return The corresponding model element
     */
    private XSDElement findModelItemFromViewItem(final TreeItem item) {
        final List<String> path = getPathFromViewItems(item);
        final List<XSDElement> elements = getModelEntriesFromPath(path);
        return elements.get(elements.size() - 1);
    }

    /**
     * Helper to create an XPath from a drag operation on the tree.
     * 
     * @param item The item dragged
     * @return The XPath for all the parent elements and selected attribute/values
     */
    private VariableEntry getVariableFromViewItem(final TreeItem item) {
        final List<String> path = getPathFromViewItems(item);
        final List<XSDElement> elements = getModelEntriesFromPath(path);
        final StringBuilder sb = new StringBuilder();
        for (final XSDElement element : elements) {
            if ((element.getCurrentAttributeName() == null)
                || (element.getCurrentAttributeValue() == null)
                || element.getCurrentAttributeName().equals("")
                || element.getCurrentAttributeValue().equals("")) {
                sb.append("/").append(element.getName()); // not predicate
            } else {
                sb.append("/").append(element.getName())
                    .append("[@").append(element.getCurrentAttributeName())
                    .append("='").append(element.getCurrentAttributeValue()).append("']");
            }
        }
        final String baseName = elements.get(elements.size() - 1).getName();
        final VariableEntry var = new VariableEntry(
            EVariableDirection.Incoming,
            createUniqueVariableName(baseName),
            sb.toString(),
            VariableType.String);
        return var;
    }

    /**
     * Check if an automatically created variable name already exists. If it exists, create a new non-existing numbered version
     * 
     * @param baseName The original name
     * @return The unique name version
     */
    private String createUniqueVariableName(final String baseName) {
        final Set<String> names = new HashSet<String>();
        final int max = tableModel.size() - 1;
        for (int i = 0; i < max; i++) {
            names.add(tableModel.get(i).getName().intern());
        }
        if (!names.contains(baseName)) {
            return baseName;
        }
        int counter = 1;
        String newName;
        do {
            newName = baseName + "_" + Integer.toString(counter);
            if (names.contains(newName)) {
                counter++;
                continue;
            }
            break;
        } while (true);
        return newName;
    }

    /**
     * Copies a "last line" entry to a new one and shifts the "empty" entry down one position. This leads to the reference staying untouched
     * and able to detect new "new last line entries" again.
     * 
     * @param entry The just edited variable entry (possibly the last one)
     */
    void copyNewEntryIfNecessary(final VariableEntry entry) {
        if (entry == newEntry) { // reference comparison
            final int row = table.getSelectionIndex();
            final VariableEntry copyEntry = new VariableEntry(entry);
            // now reset "new entry" entry
            newEntry.setDirection(EVariableDirection.Incoming);
            newEntry.setName("");
            newEntry.setXpath("");
            tableModel.add(row, copyEntry);
            updateTreeAttributesFromTableEntry(copyEntry);
        } else {
            updateTreeAttributesFromTableEntry(entry);
        }
        tableViewer.refresh();
    }

    /**
     * Performed when an existing variable entry is selected in the table. Then the attribute names and values in the tree get updated.
     * WARNING: This method assumes predicates to be only
     * 
     * @key='value', nothing more complex TODO
     * 
     * @param entry The entry to update
     */
    void updateTreeAttributesFromTableEntry(final VariableEntry entry) {
        final XPathLocation xpath = XPathParser.parse(entry.getXpath());
        XSDElement element = doc;
        for (final XPathStep step : xpath.getSteps()) {
            for (final XSDElement child : element.getElements()) {
                if (child.getName().equals(step.getValue())) { // found step in model
                    element = child;
                    if (step instanceof XPathNode) {
                        final XPathNode node = (XPathNode) step;
                        final XPathPredicate predicate = node.getPredicate();
                        if (predicate != null) {
                            final String pred = predicate.getValue();
                            final int splitPos = pred.indexOf("=");
                            if (splitPos >= 0) {
                                child.setAttributeName(pred.substring(1, splitPos));
                                child.setAttributeValue(pred.substring(splitPos + 2, pred.length() - 1));
                            }
                        }
                    }
                    break;
                }
            }
        }
        treeViewer.refresh();
    }

    /**
     * Determine the horizontal click location.
     * 
     * @param item The item (row) clicked on
     * @param clickLocation The mouse event location
     * @return The column clicked or NOT_FOUND
     */
    private int getTreeColumnFromClickLocation(final TreeItem item, final Point clickLocation) {
        int column = NOT_FOUND;
        for (int i = 0; i < TREE_COLUMNS; i++) {
            final Rectangle rect = item.getBounds(i);
            if (rect.contains(clickLocation)) {
                column = i;
                break;
            }
        }
        return column;
    }

    /**
     * Determine click location on table.
     * 
     * @param clickLocation The mouse event location (mouse down)
     * @return A point containing x = column and y = row index
     */
    private Point getTableIndexFromClickLocation(final Point clickLocation) {
        final Rectangle clientArea = table.getClientArea();
        int index = table.getTopIndex(); // top-visible index
        while (index < table.getItemCount()) {
            boolean visible = false;
            final TableItem item = table.getItem(index);
            for (int i = 0; i < TABLE_COLUMNS; i++) {
                final Rectangle rect = item.getBounds(i);
                if (rect.contains(clickLocation)) {
                    return new Point(i, index);
                }
                if (!visible && rect.intersects(clientArea)) {
                    visible = true;
                }
            }
            if (!visible) {
                return new Point(NOT_FOUND, NOT_FOUND);
            }
            index++;
        }
        return new Point(NOT_FOUND, NOT_FOUND);
    }

    /**
     * Accessor for using classes.
     * 
     * @return The model (all defined variables).
     */
    Set<VariableEntry> getVariables() {
        final Set<VariableEntry> vars = new HashSet<VariableEntry>();
        vars.addAll(tableModel);
        vars.remove(newEntry); // remove the "empty next new entry"
        return vars;
    }

    /**
     * This is for testing purposes only.
     * 
     * @param args Args
     * @throws ParserConfigurationException
     * @throws SAXException ex
     * @throws IOException ex
     * @throws ParserConfigurationException ex
     * @throws XMLStreamException ex
     */
    public static void main(final String[] args) throws SAXException, IOException, ParserConfigurationException, XMLStreamException {
        final XSDElement doc = new XSDElement(null, "root");
        doc.setElements(XSDGenerator.generate("test.xml").getElements());
        doc.getElements().get(0).getAttributes().remove(0); // remove schema location
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());
        shell.setText("Test");
        final Button ok = new Button(shell, SWT.None);
        ok.setText("Open");
        ok.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                final XPathChooserDialog dialog = new XPathChooserDialog(shell, doc);
                dialog.setMaximumNumberOfEntries(1);
                Set<VariableEntry> selectedVariables = new HashSet<VariableEntry>();
                selectedVariables.add(new VariableEntry(
                    EVariableDirection.Outgoing, "bla", "/cpacs/header[@xyz='abc']/name", VariableType.String));
                dialog.setSelectedVariables(selectedVariables);
                if (dialog.open() == SWT.OK) {
                    dialog.getSelectedVariables();
                }
            }
        });
        shell.pack();
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    /**
     * Initialize the view with already known variables.
     * 
     * @param variables The variables
     */
    public void setSelectedVariables(final Collection<VariableEntry> variables) {
        Set<VariableEntry> toRetain = new HashSet<VariableEntry>();
        toRetain.add(newEntry);
        tableModel.retainAll(toRetain);
        for (final VariableEntry variable : variables) {
            tableModel.add(tableModel.size() - 1, variable);
        }
        tableViewer.refresh();
    }

}
