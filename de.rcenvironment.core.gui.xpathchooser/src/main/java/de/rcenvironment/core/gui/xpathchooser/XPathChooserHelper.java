/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.gui.xpathchooser.model.XSDElement;
import de.rcenvironment.core.utils.common.variables.legacy.VariableType;

/**
 * The GUI creating class used in dialogs and views.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Adrian Stock
 * @author Jan Flink
 */
public class XPathChooserHelper {

    /**
     * For sash creation.
     */
    static final int PERCENT_100 = 100;

    /**
     * For sash creation.
     */
    private static final int PERCENT_85 = 81;

    /**
     * Number of columns in the cpacs tree table.
     */
    private static final int TREE_COLUMNS = 3;
    
    /**
     * The tree table containing the cpacs.
     */
    private Tree tree;

    /**
     * The JFace mechanism to provide custom contents.
     */
    private TreeViewer treeViewer;

    /**
     * The loaded and parsed CPACS.
     */
    private XSDElement doc;

    private VariableEntry xpathEntry;

    private Text text;
    
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
        parent.setLayout(new FormLayout());
        final Group firstGroup = new Group(parent, SWT.NONE);
        firstGroup.setText("XML");
        firstGroup.setLayout(new FillLayout());
        createTree(firstGroup);
        final Sash sash = new Sash(parent, SWT.HORIZONTAL);
        final Group secondGroup = new Group(parent, SWT.None);
        secondGroup.setText("XPath");
        secondGroup.setLayout(new FillLayout());
        createLabel(secondGroup);

        // set layout data to allow sash
        final FormData firstData = new FormData();
        firstData.top = new FormAttachment(/* numerator */0, /* offset */0);
        firstData.bottom = new FormAttachment(sash, 0);
        firstData.left = new FormAttachment(0, 0);
        firstData.right = new FormAttachment(PERCENT_100, 0);
        firstGroup.setLayoutData(firstData);
        final FormData sashData = new FormData();
        sashData.top = new FormAttachment(PERCENT_85, 0);
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
            @Override
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
        tree.addListener(SWT.MeasureItem, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                // Nothing to do here as this is a workaround to avoid a bug in tree expanding on several operating systems.
            }
        });
        treeViewer = new TreeViewer(tree);
        final String[] columnNames = new String[] { "Element", "Attributes", "Values" };
        for (int c = 0; c < TREE_COLUMNS; c++) {
            final TreeViewerColumn tvc = new TreeViewerColumn(treeViewer, SWT.None);
            tvc.getColumn().setText(columnNames[c]);
            tvc.getColumn().setResizable(true);
            tvc.getColumn().setMoveable(false);
            if (c >= 1) { // ignore column "element"
                tvc.setEditingSupport(new XSDEditingSupport(this, treeViewer, c));
            }
        }

        treeViewer.setContentProvider(new XSDContentProvider());
        treeViewer.setLabelProvider(new XSDLabelProvider(this));
        treeViewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
                if (thisSelection == null || thisSelection.isEmpty()) {
                    return;
                }
                Object selectedNode = thisSelection.getFirstElement();

                if (!treeViewer.isExpandable(selectedNode)) {
                    return;
                }

                final XSDContentProvider provider = (XSDContentProvider) treeViewer.getContentProvider();
                if (!provider.hasChildren(selectedNode)) {
                    return;
                }

                treeViewer.setExpandedState(selectedNode, !treeViewer.getExpandedState(selectedNode));

            }
        });

        fillTree();
        
        tree.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                selectItem();

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // nothing to do here.
            }
        });

    }
    
    /**
     * Updates the XPath with the chosen item.
     */
    public void selectItem() {
        final TreeItem item = tree.getSelection()[0];
        xpathEntry = getVariableFromViewItem(item);
        text.setText(xpathEntry.getXpath());
    }

    /**
     * Create the bottom part of the dialog, the label containing the created variable.
     * 
     * @param parent The parent component to add the child to.
     */
    public void createLabel(final Composite parent) {
        text = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
        xpathEntry = new VariableEntry(EVariableDirection.Incoming, "", "", VariableType.String);
    }

    /**
     * Fill the tree with the documents' contents.
     */
    private void fillTree() {
        treeViewer.setInput(doc);
        treeViewer.expandAll();
        final int[] columnWidths = new int[] { 470, 120, 180 };
        for (int c = 0; c < TREE_COLUMNS; c++) {
            tree.getColumn(c).pack();
            tree.getColumn(c).setWidth(columnWidths[c]);
        }
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
     * @param column The column to create choices for
     */
    void setElementValue(final TreeItem item, final String value, final int column) {
        final XSDElement element = findModelItemFromViewItem(item);
        setElementValue(element, value, column);
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
            for (final XSDElement e : element.getElements()) {
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
        String name = xpathEntry.getName().intern();
        if (!name.equals(baseName)) {
            return baseName;
        }
        int counter = 1;
        String newName;
        do {
            newName = baseName + "_" + Integer.toString(counter);
            if (name.equals(newName)) {
                counter++;
                continue;
            }
            break;
        } while (true);
        return newName;
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
                            final int splitPos = pred.indexOf('=');
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
     * Accessor for using classes.
     * 
     * @return The model (all defined variables).
     */
    VariableEntry getVariable() {
        return xpathEntry;
    }

}
