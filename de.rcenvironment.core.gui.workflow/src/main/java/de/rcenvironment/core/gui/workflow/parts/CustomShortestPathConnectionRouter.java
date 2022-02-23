/*
 * Copyright (c) 2000, 2005 IBM Corporation and others
 * Copyright 2015-2022 DLR, Germany
 *  
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

/*******************************************************************************
 * Code adapted from Tatiana (Trace Analysis Tool for Interaction Analysts)
 * (see: https://code.google.com/p/tatiana/source/browse/trunk/fr.emse.tatiana.scoresheetvisualisation/
 * src/fr/emse/tatiana/scoresheetvisualisation/ShortestPathConnectionRouter.java)
 * Downloaded: October, 2015
 * 
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutListener;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.Path;
import org.eclipse.draw2d.graph.ShortestPathRouter;

import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart.TransparentLabel;

/**
 * Routes multiple connections around the children of a given container figure.
 * 
 * @author Whitney Sorenson
 * @author Randy Hudson
 * @author Oliver Seebach
 */
public final class CustomShortestPathConnectionRouter extends AbstractRouter {

    /** Custom Layout listener. */
    private class LayoutTracker extends LayoutListener.Stub {

        public void postLayout(IFigure containerFigure) {
            processLayout();
        }

        public void remove(IFigure child) {
            // Skip labels as routing obstacles
            if (!(child instanceof TransparentLabel)) {
                removeChild(child);
            }
        }

        public void setConstraint(IFigure child, Object constraint) {
            addChild(child);
        }
    }

    private Map<Connection, Object> constraintMap = new HashMap<Connection, Object>();

    private Map<IFigure, Rectangle> figuresToBounds;

    private Map<Connection, Path> connectionToPaths;

    private boolean isDirty;

    private ShortestPathRouter algorithm = new ShortestPathRouter();

    private IFigure container;

    private Set<Connection> staleConnections = new HashSet<Connection>();

    private LayoutListener listener = new LayoutTracker();

    private FigureListener figureListener = new FigureListener() {

        public void figureMoved(IFigure source) {

            Rectangle newBounds = source.getBounds().getCopy();

            if (algorithm.updateObstacle((Rectangle) figuresToBounds.get(source), newBounds)) {
                queueSomeRouting();
                isDirty = true;
            }

            // Skip labels as routing obstacles
            if (!(source instanceof TransparentLabel)) {
                figuresToBounds.put(source, newBounds);
            }
        }
    };

    private boolean ignoreInvalidate;

    /**
     * Creates a new shortest path router with the given container. The container contains all the figure's which will be treated as
     * obstacles for the connections to avoid. Any time a child of the container moves, one or more connections will be revalidated to
     * process the new obstacle locations. The connections being routed must not be contained within the container.
     * 
     * @param container the container
     */
    public CustomShortestPathConnectionRouter(IFigure container) {
        isDirty = false;
        algorithm = new ShortestPathRouter();
        this.container = container;
    }

    void addChild(IFigure child) {
        if (connectionToPaths == null) {
            return;
        }
        if (figuresToBounds.containsKey(child)) {
            return;
        }
        Rectangle bounds = child.getBounds().getCopy();

        // Skip labels as routing obstacles
        if (!(child instanceof TransparentLabel)) {
            algorithm.addObstacle(bounds);
            figuresToBounds.put(child, bounds);
            child.addFigureListener(figureListener);
            isDirty = true;
        }
    }

    private void hookAll() {
        figuresToBounds = new HashMap<IFigure, Rectangle>();
        for (int i = 0; i < container.getChildren().size(); i++) {
            addChild((IFigure) container.getChildren().get(i));
        }
        container.addLayoutListener(listener);
    }

    private void unhookAll() {
        container.removeLayoutListener(listener);
        if (figuresToBounds != null) {
            Iterator<IFigure> figureItr = figuresToBounds.keySet().iterator();
            while (figureItr.hasNext()) {
                // Must use iterator's remove to avoid concurrent modification
                IFigure child = (IFigure) figureItr.next();
                // Skip labels as routing obstacles
                if (!(child instanceof TransparentLabel)) {
                    figureItr.remove();
                    removeChild(child);
                }
            }
            figuresToBounds = null;
        }
    }

    /**
     * Gets the constraint for the given {@link Connection}. The constraint is the paths list of bend points for this connection.
     *
     * @param connection The connection whose constraint we are retrieving
     * @return The constraint
     */
    public Object getConstraint(Connection connection) {
        return constraintMap.get(connection);
    }

    /**
     * Returns the default spacing maintained on either side of a connection. The default value is 4.
     * 
     * @return the connection spacing
     * @since 3.2
     */
    public int getSpacing() {
        return algorithm.getSpacing();
    }

    @Override
    public void invalidate(Connection connection) {
        if (ignoreInvalidate) {
            return;
        }
        staleConnections.add(connection);
        isDirty = true;
    }

    private void processLayout() {
        if (staleConnections.isEmpty()) {
            return;
        }
        ((Connection) staleConnections.iterator().next()).revalidate();
    }

    private void processStaleConnections() {
        Iterator<Connection> iter = staleConnections.iterator();
        if (iter.hasNext() && connectionToPaths == null) {
            connectionToPaths = new HashMap<Connection, Path>();
            hookAll();
        }

        while (iter.hasNext()) {
            Connection conn = (Connection) iter.next();

            Path path = (Path) connectionToPaths.get(conn);
            if (path == null) {
                path = new Path(conn);
                connectionToPaths.put(conn, path);
                algorithm.addPath(path);
            }

            List<?> constraint = (List<?>) getConstraint(conn);
            if (constraint == null) {
                constraint = Collections.EMPTY_LIST;
            }

            Point start = conn.getSourceAnchor().getReferencePoint().getCopy();
            Point end = conn.getTargetAnchor().getReferencePoint().getCopy();

            container.translateToRelative(start);
            container.translateToRelative(end);

            path.setStartPoint(start);
            path.setEndPoint(end);

            if (!constraint.isEmpty()) {
                PointList bends = new PointList(constraint.size());
                for (int i = 0; i < constraint.size(); i++) {
                    Bendpoint bp = (Bendpoint) constraint.get(i);
                    bends.addPoint(bp.getLocation());
                }
                path.setBendPoints(bends);
            } else {
                path.setBendPoints(null);
            }

            isDirty |= path.isDirty;
        }
        staleConnections.clear();
    }

    void queueSomeRouting() {
        if (connectionToPaths == null || connectionToPaths.isEmpty()) {
            return;
        }
        try {
            ignoreInvalidate = true;
            ((Connection) connectionToPaths.keySet().iterator().next())
                .revalidate();
        } finally {
            ignoreInvalidate = false;
        }
    }

    @Override
    public void remove(Connection connection) {
        staleConnections.remove(connection);
        constraintMap.remove(connection);
        if (connectionToPaths == null) {
            return;
        }
        Path path = (Path) connectionToPaths.remove(connection);
        algorithm.removePath(path);
        isDirty = true;
        if (connectionToPaths.isEmpty()) {
            unhookAll();
            connectionToPaths = null;
        } else {
            // Make sure one of the remaining is revalidated so that we can re-route again.
            queueSomeRouting();
        }
    }

    void removeChild(IFigure child) {
        if (connectionToPaths == null) {
            return;
        }
        Rectangle bounds = child.getBounds().getCopy();
        boolean change = false;
        algorithm.removeObstacle(bounds);
        figuresToBounds.remove(child);
        child.removeFigureListener(figureListener);
        if (change) {
            isDirty = true;
            queueSomeRouting();
        }
    }

    @Override
    public void route(Connection conn) {
        if (isDirty) {
            ignoreInvalidate = true;
            processStaleConnections();
            isDirty = false;
            List<?> updated = algorithm.solve();
            Connection current;
            for (int i = 0; i < updated.size(); i++) {
                Path path = (Path) updated.get(i);
                current = (Connection) path.data;
                current.revalidate();

                PointList points = path.getPoints().getCopy();
                Point ref1;
                Point ref2;
                Point start;
                Point end;
                ref1 = new PrecisionPoint(points.getPoint(1));
                ref2 = new PrecisionPoint(points.getPoint(points.size() - 2));
                current.translateToAbsolute(ref1);
                current.translateToAbsolute(ref2);

                start = current.getSourceAnchor().getLocation(ref1).getCopy();
                end = current.getTargetAnchor().getLocation(ref2).getCopy();

                current.translateToRelative(start);
                current.translateToRelative(end);
                points.setPoint(start, 0);
                points.setPoint(end, points.size() - 1);

                current.setPoints(points);
            }
            ignoreInvalidate = false;
        }
    }

    @Override
    public void setConstraint(Connection connection, Object constraint) {
        // Connection.setConstraint() already calls revalidate, so we know that a
        // route() call will follow.
        staleConnections.add(connection);
        constraintMap.put(connection, constraint);
        isDirty = true;
    }

    /**
     * Sets the default space that should be maintained on either side of a connection. This causes the connections to be separated from
     * each other and from the obstacles. The default value is 4.
     * 
     * @param spacing the connection spacing
     * @since 3.2
     */
    public void setSpacing(int spacing) {
        algorithm.setSpacing(spacing);
    }

}
