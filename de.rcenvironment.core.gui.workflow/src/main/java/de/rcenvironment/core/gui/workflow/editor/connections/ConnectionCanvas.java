/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionDeleteCommand;

/**
 * Composite that displays the connections.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Oliver Seebach
 */
public class ConnectionCanvas extends FigureCanvas {

    protected static final int DEFAULT_TOLERANCE = 5;

    /** The root figure. */
    private Figure parentFigure;

    /** The context menu. */
    private Menu contextMenu;

    /** Delete menu item. */
    private MenuItem deleteMenuItem;

    /** Currently selected Figure. */
    private Set<ConnectionFigure> currentSelection = new HashSet<ConnectionFigure>();

    /** The associated WorkflowDescription. */
    private WorkflowDescription description;

    /** TreeViewer on the left of the canvas. */
    private EndpointTreeViewer sourceTreeViewer;

    /** TreeViewer on the right of the canvas. */
    private EndpointTreeViewer targetTreeViewer;
    
    /** TreeViewer on the right of the canvas. */
    private CommandStack editorsCommandStack;

    /**
     * Constructor.
     * 
     * @param parent Cf. parent {@link FigureCanvas#FigureCanvas(Composite, int)}.
     * @param style Cf. parent {@link FigureCanvas#FigureCanvas(Composite, int)}.
     */
    public ConnectionCanvas(Composite parent, int style) {
        super(parent, style);
        
        // create canvas
        parentFigure = new Figure();
        parentFigure.setLayoutManager(new XYLayout());
        setContents(parentFigure);
        // add a MouseListener to handle the selection of connections
        parentFigure.addMouseListener(new MouseListener() {

            public void mouseDoubleClicked(MouseEvent e) {
                //
            }

            public void mousePressed(MouseEvent e) {
                if (e.button == 1) {
                    // clear the previous selection
                    setSelection(null, (e.getState() & SWT.CONTROL) != 0);
                    // gather the connections indicated with the mouse click
                    Set<ConnectionFigure> selection = findConnectionsAt(e.x, e.y, DEFAULT_TOLERANCE);
                    // set the new selection
                    setSelection(selection, (e.getState() & SWT.CONTROL) != 0);
                    // enable the 'delete' menu item, if connections are selected, disable otherwise
                    deleteMenuItem.setEnabled(!selection.isEmpty());
                }
            }

            public void mouseReleased(MouseEvent e) {
                //
            }

        });
        // add a KeyListener to handle deletion of connections
        this.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent e) {
                // if DEL was pressed delete the currently selected connections
                if (e.character == SWT.DEL) {
                    deleteSelectedConnections();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                //
            }
        });
        // create the context menu
        contextMenu = new Menu(this);
        setMenu(contextMenu);
        deleteMenuItem = new MenuItem(contextMenu, SWT.NONE);
        deleteMenuItem.setEnabled(false);
        deleteMenuItem.setText(Messages.delete);
        // add a SelectionListener to the 'delete' menu item to handle deletion of connections
        deleteMenuItem.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                //
            }

            public void widgetSelected(SelectionEvent e) {
                deleteSelectedConnections();
            }
        });
    }

    private void setSelection(final Set<ConnectionFigure> connections, boolean controlDown) {
        // clear the former selection
        if (!controlDown) {
            for (ConnectionFigure connection : currentSelection) {
                connection.setSelected(false);
            }
            currentSelection.clear();
        }
        // null indicates a request to just clear the current selection, so return early
        if (connections != null) {
            if (controlDown) {
                List<ConnectionFigure> toDelete = new LinkedList<ConnectionFigure>();
                for (ConnectionFigure c : connections) {
                    if (currentSelection.contains(c)) {
                        toDelete.add(c);
                    } else {
                        currentSelection.add(c);
                        c.setSelected(true);
                    }
                }
                for (ConnectionFigure c : toDelete) {
                    currentSelection.remove(c);
                    c.setSelected(false);
                }
            } else {
                currentSelection.addAll(connections);
                // highlight the new selection
                for (ConnectionFigure connection : currentSelection) {
                    connection.setSelected(true);
                }
            }
        }
    }

    private Set<ConnectionFigure> getSelection() {
        return Collections.unmodifiableSet(currentSelection);
    }

    public void setEditorsCommandStack(CommandStack editorsCommandStack) {
        this.editorsCommandStack = editorsCommandStack;
    }

    private void deleteSelectedConnections() {
        // delete the currently selected connections
        deleteConnections(getSelection());
        // clear the selection
        setSelection(null, false);
    }

    private void deleteConnections(Collection<ConnectionFigure> connections) {
        boolean dirty = false;
        List<Connection> connectionsToDelete = new ArrayList<>();
        for (ConnectionFigure connectionFigure : connections) {
            connectionsToDelete.add(connectionFigure.getConnection());
            dirty = true;
        }
        
        if (editorsCommandStack == null) {
            description.removeConnections(connectionsToDelete);
            targetTreeViewer.refresh();
            sourceTreeViewer.refresh();
        } else {
            ConnectionDeleteCommand connectionDeleteCommand = new ConnectionDeleteCommand(description, connectionsToDelete);
            editorsCommandStack.execute(connectionDeleteCommand);
        }
        
        if (dirty){
            repaint();
        }
        
    }

    /**
     * Returns the 'first' {@link ConnectionFigure} at the given coordinates, using the tolerance as
     * a 'growing tolerance window'.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param tolerance the maximum value for the tolerance window
     * @return the 'first' {@link ConnectionFigure} at the given coordinates
     */
    protected ConnectionFigure findFirstConnectionAt(final int x, final int y, final int tolerance) {
        final List<?> children = parentFigure.getChildren();
        // grow the 'tolerance window' from '0' to the provided value
        // as soon as connections lie within the tolerance window search is over, thus only the
        // 'closest' connections are selected
        for (int toleranceIndex = 0; toleranceIndex <= tolerance; ++toleranceIndex) {
            final Rectangle hitarea = new Rectangle(x - toleranceIndex, y - toleranceIndex, 1 + 2 * toleranceIndex, 1 + 2 * toleranceIndex);
            for (Object child : children) {
                if (child instanceof ConnectionFigure) {
                    ConnectionFigure childFigure = (ConnectionFigure) child;
                    if (childFigure.intersects(hitarea)) {
                        return childFigure;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the 'closest' {@link ConnectionFigure}s at the given coordinates, using the tolerance
     * as a 'growing tolerance window'.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param tolerance the maximum value for the tolerance window
     * @return the 'closest' {@link ConnectionFigure}s at the given coordinates
     */
    protected Set<ConnectionFigure> findConnectionsAt(final int x, final int y, final int tolerance) {
        Set<ConnectionFigure> result = new HashSet<ConnectionFigure>();
        List<?> children = parentFigure.getChildren();
        // grow the 'tolerance window' from '0' to the provided value
        // as soon as connections lie within the tolerance window search is over, thus only the
        // 'closest' connections are selected
        for (int toleranceIndex = 0; toleranceIndex <= tolerance; ++toleranceIndex) {
            final Rectangle hitarea = new Rectangle(x - toleranceIndex, y - toleranceIndex, 1 + 2 * toleranceIndex, 1 + 2 * toleranceIndex);
            for (Object child : children) {
                if (child instanceof ConnectionFigure) {
                    ConnectionFigure childFigure = (ConnectionFigure) child;
                    if (childFigure.intersects(hitarea)) {
                        result.add(childFigure);
                    }
                }
            }
            // return as soon as a set of connections with the minimum distance is found
            if (!result.isEmpty()) {
                break;
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Must be called to initialize this view.
     * 
     * @param desc The associated WorkflowDescription.
     * @param source The source tree viewer.
     * @param target The target tree viewer.
     */
    public void initialize(WorkflowDescription desc, EndpointTreeViewer source, EndpointTreeViewer target) {
        this.description = desc;
        this.sourceTreeViewer = source;
        this.targetTreeViewer = target;
    }
    
    /**
     * Updates the workflow description and repaints.
     * 
     * @param desc The new workflow description
     */
    public void updateCanvas(WorkflowDescription desc){
        this.description = desc;
        repaint();
    }
    
    /**
     * Repaints the connections on the canvas.
     */
    public void repaint() {
        // clear the parent figure
        parentFigure.removeAll();
        // for each connection create a connection figure and add it to the parent figure
        for (Connection c : description.getConnections()) {
            TreeItem outputItem = sourceTreeViewer.findEndpoint(c.getSourceNode(), c.getOutput().getName());
            TreeItem inputItem = targetTreeViewer.findEndpoint(c.getTargetNode(), c.getInput().getName());
            // calculate the coordinates of the connection figure (the line) on the canvas
            if (inputItem != null && outputItem != null) {
                int outputY = outputItem.getBounds().y + outputItem.getBounds().height / 2;
                int outputX = 0;
                int inputY = inputItem.getBounds().y + inputItem.getBounds().height / 2;
                int inputX = parentFigure.getBounds().width;
                // create the connection figure (the connection line)
                ConnectionFigure line = new ConnectionFigure(c, new Point(outputX, outputY), new Point(inputX, inputY));
                // add the connection figure to the parent figure
                line.setAntialias(SWT.ON);
                parentFigure.add(line);
            }
        }
    }

    /**
     * PolylineFigure that represents a connection (line).
     * 
     * @author Heinrich Wendel
     * @author Christian Weiss
     */
    private class ConnectionFigure extends PolylineConnection {

        /** The represented {@link Connection} instance. */
        private final Connection connection;

        /**
         * Constructor.
         * 
         * @param connection The connection.
         */
        ConnectionFigure(Connection connection, Point start, Point end) {
            this.connection = connection;
            setStart(start);
            setEnd(end);
            setTargetDecoration(new PolylineDecoration());
        }

        /**
         * Returns the represented {@link Connection} instance.
         * 
         * @return The represented {@link Connection} instance.
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Sets the selection state of the connection figure.
         * 
         * @param selected The new selection state.
         */
        public void setSelected(boolean selected) {
            if (selected) {
                setForegroundColor(ColorConstants.blue);
            } else {
                setForegroundColor(ColorConstants.black);
            }
        }

        @Override
        public boolean intersects(Rectangle rect) {
            if (!super.intersects(rect)) {
                return false;
            }
            int lineWidth = getLineWidth();
            int lineWidthAdjustment = lineWidth - 1;
            Point start = getStart();
            Point end = getEnd();
            Line2D line = new Line2D.Float(start.x, start.y, end.x, end.y);
            Rectangle2D rectangle =
                new Rectangle2D.Double(rect.x - lineWidthAdjustment, rect.y - lineWidthAdjustment, rect.width + 2 * lineWidthAdjustment,
                    rect.height + 2 * lineWidthAdjustment);
            return line.intersects(rectangle);
        }

    }

}
