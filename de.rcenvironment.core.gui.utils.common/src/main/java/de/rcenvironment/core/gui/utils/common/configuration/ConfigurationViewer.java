/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetEntry;

import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerContentProvider.Element;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerContentProvider.Leaf;

/**
 * A viewer to represent a hierarchical property tree including means to change property values.
 * 
 * @author Christian Weiss
 */
public class ConfigurationViewer extends TreeViewer {

    /**
     * The interface VisibilityAction extends the {@link IAction} interface with the capability to
     * hold the 'visibility' state of an action.
     * 
     * @author Christian Weiss
     */
    public interface VisibilityAction extends IAction {

        /**
         * Sets the visible.
         * 
         * @param visible the new visible
         */
        void setVisible(boolean visible);

        /**
         * Checks if is visible.
         * 
         * @return true, if is visible
         */
        boolean isVisible();
    }

    private static final Log LOGGER = LogFactory.getLog(ConfigurationViewer.class);

    /** The default ratio of the property label column. */
    private static final float DEFAULT_PROPERTY_COLUMN_RATIO = 0.3f;

    /** The Constant COLUMN_LABELS. */
    private static final String[] COLUMN_LABELS = { Messages.propertyKey, Messages.propertyValue };

    /** The index of the editable column. */
    private static final int EDIT_COLUMN_INDEX = 1;

    private static final int MAX_EDITABLE_TEXT_LENGTH = 2048;

    /** The context menu items. */
    private final List<Object> contextMenuItems = new LinkedList<Object>();

    /** The tree. */
    private final Tree tree;

    /** The composite used to facilitate dynamic resizing of the tree. */
    private final LayoutComposite layoutComposite;

    /** The menu manager. */
    private MenuManager menuManager;

    /** The tree editor. */
    private final TreeEditor treeEditor;

    /** The cell editor. */
    private CellEditor cellEditor;

    /** The editor listener. */
    private ICellEditorListener editorListener;

    /** The status line manager for showing messages. */
    private IStatusLineManager statusLineManager;

    private final PropertyChangeListener propertyChangeListener = new UpdatingPropertyChangeListener();

    /**
     * Instantiates a new property viewer.
     * 
     * @param parent the parent
     */
    public ConfigurationViewer(final Composite parent) {
        this(parent, SWT.NONE);
    }

    /**
     * Instantiates a new property viewer.
     * 
     * @param parent the parent
     * @param style the style
     */
    public ConfigurationViewer(final Composite parent, final int style) {
        super(new LayoutComposite(parent, SWT.NONE), style | SWT.FULL_SELECTION | SWT.SINGLE
            | SWT.HIDE_SELECTION);
        tree = getTree();
        this.layoutComposite = (LayoutComposite) tree.getParent();

        // configure the widget
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);

        // configure the columns
        addColumns();

        final TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
        this.layoutComposite.setLayout(treeColumnLayout);
        final Widget[] columns = tree.getColumns();
        final int i100 = 100;
        final int minWidth = 200;
        final int column0weight = (int) (100.0f * DEFAULT_PROPERTY_COLUMN_RATIO);
        final int column0minWidth = minWidth * column0weight / i100;
        final int column1weight = i100 - column0weight;
        final int column1minWidth = minWidth - column0minWidth;
        treeColumnLayout.setColumnData(columns[0], new ColumnWeightData(column0weight, column0minWidth));
        treeColumnLayout.setColumnData(columns[1], new ColumnWeightData(column1weight, column1minWidth));

        // add our listeners to the widget
        hookControl();

        // create a new tree editor
        treeEditor = new TreeEditor(tree);

        // // create the entry and editor listener
        // createEntryListener();
        createEditorListener();

        // createPropertyChangeListener();

        // initialize the context menu manager
        initContextMenuManager();
    }

    @Override
    protected void inputChanged(final Object input, final Object oldInput) {
        if (oldInput != null) {
            try {
                final Method removeMethod = oldInput.getClass().getMethod("removePropertyChangeListener", PropertyChangeListener.class);
                if (removeMethod != null) {
                    removeMethod.invoke(oldInput, propertyChangeListener);
                }
            } catch (SecurityException e) {
                e = null;
            } catch (NoSuchMethodException e) {
                e = null;
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
            } catch (IllegalAccessException e) {
                LOGGER.error(e);
            } catch (InvocationTargetException e) {
                LOGGER.error(e);
            }
        }
        if (input != null) {
            try {
                final Method addMethod = input.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class);
                if (addMethod != null) {
                    addMethod.invoke(input, propertyChangeListener);
                }
            } catch (SecurityException e) {
                e = null;
            } catch (NoSuchMethodException e) {
                e = null;
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
            } catch (IllegalAccessException e) {
                LOGGER.error(e);
            } catch (InvocationTargetException e) {
                LOGGER.error(e);
            }
        }
        super.inputChanged(input, oldInput);
    }

    /**
     * Inits the context menu manager.
     */
    private void initContextMenuManager() {
        menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        // clear the context menu before it is shown
        menuManager.setRemoveAllWhenShown(true);
        // rebuild the context menu before it is shown
        menuManager.addMenuListener(new IMenuListener() {

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
             */
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                for (final Object itemObject : contextMenuItems) {
                    if (itemObject instanceof IContributionItem) {
                        final IContributionItem item = (IContributionItem) itemObject;
                        menuManager.add(item);
                    } else if (itemObject instanceof VisibilityAction) {
                        final VisibilityAction item = (VisibilityAction) itemObject;
                        if (item.isVisible()) {
                            menuManager.add(item);
                        }
                    } else if (itemObject instanceof IAction) {
                        final IAction item = (IAction) itemObject;
                        menuManager.add(item);
                    } else {
                        throw new AssertionError();
                    }
                }
            }

        });
        final Menu menu = menuManager.createContextMenu(tree);
        tree.setMenu(menu);
    }

    /**
     * Adds an {@link IAction} item to the context menu.
     * 
     * @param item the item
     */
    public void addContextMenuItem(final IAction item) {
        contextMenuItems.add(item);
    }

    /**
     * Adds an item {@link IContributionItem} to the context menu.
     * 
     * @param item the item
     */
    public void addContextMenuItem(final IContributionItem item) {
        contextMenuItems.add(item);
    }

    /**
     * Fire selection changed.
     * 
     * @param event the event {@inheritDoc}
     * @see org.eclipse.jface.viewers.Viewer#fireSelectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    @Override
    protected void fireSelectionChanged(final SelectionChangedEvent event) {
        SelectionChangedEvent proceedEvent = event;
        final ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            final Object element = structuredSelection
                .getFirstElement();
            if (element instanceof ConfigurationViewerContentProvider.Element) {
                final Object value = ((ConfigurationViewerContentProvider.Element) element).getPropertyDescriptor();
                if (value != null) {
                    final IStructuredSelection replacementSelection = new StructuredSelection(value);
                    final SelectionChangedEvent replacementEvent =
                        new SelectionChangedEvent(event.getSelectionProvider(), replacementSelection);
                    proceedEvent = replacementEvent;
                } else {
                    proceedEvent = null;
                }
            }
        }
        if (proceedEvent != null) {
            super.fireSelectionChanged(proceedEvent);
        }
    }

    /**
     * Activate a cell editor for the given selected tree item.
     * 
     * @param item the selected tree item
     */
    private void activateCellEditor(TreeItem item) {
        // ensure the cell editor is visible
        tree.showSelection();

        // Get the entry for this item
        final Element activeProperty = (Element) item.getData();

        // Get the cell editor for the entry.
        // Note that the editor parent must be the Tree control
        cellEditor = activeProperty.createPropertyEditor(tree);

        if (cellEditor == null) {
            // unable to create the editor
            return;
        }

        boolean editable = true;

        // only set the value of the editor if a non-null value is set, otherwise
        // IllegalArgumentException would occur (thrown by JFace)
        final Object value = activeProperty.getValue();
        if (value != null) {
            final Object editorValue;
            if (cellEditor instanceof TextCellEditor) {
                /*
                 * Max length for TextCellEditor is for some reason 5632. As this is a heuristic
                 * value, the editable length is truncated to a meaningful default.
                 */

                final String textValue = value.toString();
                if (textValue.length() > MAX_EDITABLE_TEXT_LENGTH) {
                    editable = false;
                }
                editorValue = textValue;
            } else {
                editorValue = value;
            }
            if (!editable) {
                return;
            }
            cellEditor.setValue(editorValue);
        }

        // activate the cell editor
        cellEditor.activate();

        // if the cell editor has no control we can stop now
        Control control = cellEditor.getControl();
        if (control == null) {
            cellEditor.deactivate();
            cellEditor = null;
            return;
        }

        // add our editor listener
        cellEditor.addListener(editorListener);

        // set the layout of the tree editor to match the cell editor
        CellEditor.LayoutData layout = cellEditor.getLayoutData();
        treeEditor.horizontalAlignment = layout.horizontalAlignment;
        treeEditor.grabHorizontal = layout.grabHorizontal;
        treeEditor.minimumWidth = layout.minimumWidth;
        treeEditor.setEditor(control, item, EDIT_COLUMN_INDEX);

        // set the error text from the cell editor
        setErrorMessage(cellEditor.getErrorMessage());

        // give focus to the cell editor
        cellEditor.setFocus();
    }

    /**
     * Add columns to the tree and set up the layout manager accordingly.
     */
    private void addColumns() {
        // create the columns
        final TreeColumn[] columns = tree.getColumns();
        for (int i = 0; i < COLUMN_LABELS.length; i++) {
            final String string = COLUMN_LABELS[i];
            if (string != null) {
                final TreeColumn column;
                if (i < columns.length) {
                    column = columns[i];
                } else {
                    column = new TreeColumn(tree, 0);
                }
                column.setText(string);
                /*
                 * The tree must distribute the available space amongst the columns, thus the width
                 * of the last column is determined by the layout and must not be changed by the
                 * user.
                 */
                column.setResizable(i < COLUMN_LABELS.length - 1);
            }
        }
    }

    /**
     * Apply editor value.
     * 
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ColumnViewer#applyEditorValue()
     */
    @Override
    public void applyEditorValue() {
        if (cellEditor == null) {
            return;
        }

        // Check if editor has a valid value
        if (!cellEditor.isValueValid()) {
            setErrorMessage(cellEditor.getErrorMessage());
            return;
        }

        // get the element
        final TreeItem treeItem = treeEditor.getItem();
        // treeItem can be null when view is opened
        if (treeItem == null || treeItem.isDisposed()) {
            return;
        }
        final Element property = (Element) treeItem.getData();

        // get the new value
        final Object newValue;
        final IPropertyDescriptor propertyDescriptor = property.getPropertyDescriptor();
        if (propertyDescriptor instanceof SelectionPropertyDescriptor) {
            newValue = ((SelectionPropertyDescriptor) propertyDescriptor).getValue((Integer) cellEditor.getValue());
        } else {
            newValue = cellEditor.getValue();
        }

        // get the old value
        final Object oldValue = property.getValue();

        //
        if (property instanceof Leaf //
            && oldValue != newValue
            || (oldValue != null && !oldValue.equals(newValue))) {
            property.setValue(newValue);
            update(property, null);
            update(property.getParent(), null);
        }

        // cellEditor = null;
    }

    /**
     * Creates a new cell editor listener.
     */
    private void createEditorListener() {
        editorListener = new ICellEditorListener() {

            @Override
            public void cancelEditor() {
                deactivateCellEditor();
            }

            @Override
            public void editorValueChanged(boolean oldValidState,
                boolean newValidState) {
                // Do nothing
            }

            @Override
            public void applyEditorValue() {
                ConfigurationViewer.this.applyEditorValue();
            }
        };
    }

    /**
     * Deactivate the currently active cell editor.
     */
    private void deactivateCellEditor() {
        treeEditor.setEditor(null, null, EDIT_COLUMN_INDEX);
        if (cellEditor != null) {
            cellEditor.deactivate();
            // fireCellEditorDeactivated(cellEditor);
            cellEditor.removeListener(editorListener);
            cellEditor = null;
        }
        // clear any error message from the editor
        setErrorMessage(null);
    }

    /**
     * Sends out a selection changed event for the entry tree to all registered listeners.
     */
    private void entrySelectionChanged() {
        SelectionChangedEvent changeEvent = new SelectionChangedEvent(this,
            getSelection());
        fireSelectionChanged(changeEvent);
    }

    /**
     * Selection in the viewer occurred. Check if there is an active cell editor. If yes, deactivate
     * it and check if a new cell editor must be activated.
     * 
     * @param selection the TreeItem that is selected
     */
    private void handleSelect(TreeItem selection) {
        // deactivate the current cell editor
        if (cellEditor != null) {
            deactivateCellEditor();
        }

        if (selection == null) {
            setMessage(null);
            setErrorMessage(null);
        } else {
            Object object = selection.getData();
            if (object instanceof Element) {
                final Element activeProperty = (Element) object;
                setMessage(activeProperty.getDescription());
                activateCellEditor(selection);
            }
            if (object instanceof IPropertySheetEntry) {
                // get the entry for this item
                IPropertySheetEntry activeEntry = (IPropertySheetEntry) object;

                // display the description for the item
                setMessage(activeEntry.getDescription());

                // activate a cell editor on the selection
                activateCellEditor(selection);
            }
        }
        entrySelectionChanged();
    }

    /**
     * Establish this viewer as a listener on the control.
     */
    private void hookControl() {
        // Handle selections in the Tree
        // Part1: Double click only (allow traversal via keyboard without
        // activation
        tree.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse .swt.events.
             * SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                // The viewer only owns the status line when there is
                // no 'active' cell editor
                if (cellEditor == null || !cellEditor.isActivated()) {
                    updateStatusLine(e.item);
                }
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
             * (org.eclipse.swt.events .SelectionEvent)
             */
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.item instanceof TreeItem) {
                    handleSelect((TreeItem) e.item);
                }
            }
        });
        // Part2: handle single click activation of cell editor
        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent event) {
                // only activate if there is a cell editor
                Point pt = new Point(event.x, event.y);
                TreeItem item = tree.getItem(pt);
                if (item != null) {
                    handleSelect(item);
                }
            }
        });

        // Add a tree listener to expand and collapse which
        // allows for lazy creation of children
        tree.addTreeListener(new TreeListener() {

            @Override
            public void treeExpanded(final TreeEvent event) {
                handleTreeExpand(event);
            }

            @Override
            public void treeCollapsed(final TreeEvent event) {
                handleTreeCollapse(event);
            }
        });

        // Refresh the tree when F5 pressed
        tree.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.ESC) {
                    deactivateCellEditor();
                } else if (e.keyCode == SWT.F5) {
                    // The following will simulate a reselect
                    setInput(getInput());
                }
            }
        });
    }

    /**
     * Update the status line based on the data of item.
     * 
     * @param item the item
     */
    protected void updateStatusLine(Widget item) {
        setMessage(null);
        setErrorMessage(null);
    }

    /**
     * Sets the error message to be displayed in the status line.
     * 
     * @param errorMessage the message to be displayed, or <code>null</code>
     */
    private void setErrorMessage(String errorMessage) {
        // show the error message
        if (statusLineManager != null) {
            statusLineManager.setErrorMessage(errorMessage);
        }
    }

    /**
     * Sets the message to be displayed in the status line. This message is displayed when there is
     * no error message.
     * 
     * @param message the message to be displayed, or <code>null</code>
     */
    private void setMessage(String message) {
        // show the message
        if (statusLineManager != null) {
            statusLineManager.setMessage(message);
        }
    }

    /**
     * Layout composite to allow for dynamic resizing of the tree.
     * 
     * <p>
     * The calculation of the width of this <code>Composite</code> is based on the hints provided by
     * {@link #computeSize(int, int, boolean)} calls. Therefor such a hint is saved in a local
     * variable ({@link #widthHint}) and used whenever {@link SWT#DEFAULT} is used instead of a
     * meaningful width hint.
     * </p>
     * 
     * @author Christian Weiss
     */
    private static final class LayoutComposite extends Composite {

        /**
         * State memorizer used to ignore the first with hint.
         * <p>
         * The first width hint has to be ignored as the <code>ControlListener</code> gets
         * registered too late to get the first meaningful width hint.
         * </p>
         */
        private boolean first = true;

        /** Buffer variable to store/remember the last meaningful width hint. */
        private Integer widthHint = 0;

        LayoutComposite(Composite parent, int style) {
            super(parent, style);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            /*
             * If a width hint is provided. >> Ignore, if it is the first one. >> Store, otherwise.
             */
            if (wHint != SWT.DEFAULT) {
                if (!first) {
                    this.widthHint = wHint;
                }
                first = false;
            }
            /*
             * Use the last meaningful width hint for size calculation. This way the (meaningful)
             * hint is used to calculate the table size and not the actual width of the columns.
             */
            if (widthHint != null) {
                wHint = Math.min(widthHint, getClientArea().width);
            }
            final Point result = super.computeSize(wHint, hHint, changed);
            /*
             * Store the default (min) width of the tree, if this is the very first call using no
             * width hint.
             */
            if (first && wHint == SWT.DEFAULT) {
                this.widthHint = result.x;
            }
            // System.err.printf("%d :: %d\n", wHint, hHint);
            // System.err.printf("%s :: %s :: %s\n", getClientArea(), getParent().getClientArea(),
            // result);
            result.x = 0;
            return result;
        }

    }

    /**
     * {@link PropertyChangeListener} to update the tree upon {@link PropertyChangeEvent}s.
     * 
     * @author Christian Weiss
     */
    public class UpdatingPropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if (!tree.isDisposed()) {
                tree.update();
            }
        }

    }

}
