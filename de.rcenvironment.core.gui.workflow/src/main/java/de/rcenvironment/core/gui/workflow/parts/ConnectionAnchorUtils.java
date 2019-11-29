/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Util class that provides boxes which can be used as connection anchors.
 * 
 * @author Oliver Seebach
 *
 */
public abstract class ConnectionAnchorUtils {

    private static int componentSize = WorkflowNodePart.WORKFLOW_NODE_WIDTH;
    
    private static int widthOfAnchorBox = 0;

    private static int shiftFactorRectangular = 4;
    
    private static int shiftFactorDiagonal = 6;
    
    private ConnectionAnchorUtils() {
        // prevent instantiation
    }

    // Whole side

    /**
     * Returns a rectangle adjacent to the top edge of the reference rectangle.
     * 
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getTopBoxForRectangle(Rectangle rectangle) {
        Rectangle originalBox = rectangle;

        Point topLeftCorner = new Point(originalBox.getTopLeft().x, originalBox.getTopLeft().y - widthOfAnchorBox);
        Point topRightCornerMinusABit = new Point(originalBox.getTopRight().x, originalBox.getTopRight().y);

        Rectangle adaptedBox = new Rectangle(topLeftCorner, topRightCornerMinusABit);

        return adaptedBox;
    }

    /**
     * Returns a rectangle adjacent to the bottom edge of the reference rectangle.
     * 
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getBottomBoxForRectangle(Rectangle rectangle) {
        Rectangle originalBox = rectangle;

        Point bottomLeftCorner = new Point(originalBox.getBottomLeft().x, originalBox.getBottomLeft().y);
        Point bottomRightCornerPlusABit = new Point(originalBox.getBottomRight().x,
            originalBox.getBottomRight().y + widthOfAnchorBox);

        Rectangle adaptedBox = new Rectangle(bottomLeftCorner, bottomRightCornerPlusABit);

        return adaptedBox;
    }

    /**
     * Returns a rectangle adjacent to the right edge of the reference rectangle.
     * 
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getRightBoxForRectangle(Rectangle rectangle) {
        Rectangle originalBox = rectangle;

        Point topRightCorner = new Point(originalBox.getTopRight().x, originalBox.getTopRight().y);
        Point bottomRightCornerPlusABit = new Point(originalBox.getBottomRight().x + widthOfAnchorBox,
            originalBox.getBottomRight().y);

        Rectangle adaptedBox = new Rectangle(topRightCorner, bottomRightCornerPlusABit);

        return adaptedBox;
    }

    /**
     * Returns a rectangle adjacent to the left edge of the reference rectangle.
     * 
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getLeftBoxForRectangle(Rectangle rectangle) {
        Rectangle originalBox = rectangle;

        Point topLeftCornerMinusABit = new Point(originalBox.getTopLeft().x - widthOfAnchorBox, originalBox.getTopLeft().y);
        Point bottomLeftCorner = new Point(originalBox.getBottomLeft().x, originalBox.getBottomLeft().y);

        Rectangle adaptedBox = new Rectangle(topLeftCornerMinusABit, bottomLeftCorner);

        return adaptedBox;
    }
    
    // Third of an edge, in line with the grid
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getTopLeftRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getTopLeft().x + rectangle.width/4, rectangle.getTop().y);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getTopRightRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getTopLeft().x + rectangle.width*3/4, rectangle.getTop().y);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getBottomLeftRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getBottomLeft().x + rectangle.width/4, rectangle.getBottom().y);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getBottomRightRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getBottomLeft().x + rectangle.width*3/4, rectangle.getBottom().y);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getLeftUpperRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getLeft().x, rectangle.getTopLeft().y + rectangle.height/4);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getLeftLowerRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getLeft().x, rectangle.getTopLeft().y + rectangle.height*3/4);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getRightUpperRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getRight().x, rectangle.getTopRight().y + rectangle.height/4);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getRightLowerRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getRight().x, rectangle.getTopRight().y + rectangle.height*3/4);
        return new Rectangle(p1, p1);
    }
    
    
    // Center
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getBottomCenterRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getBottomLeft().x + rectangle.width/2, rectangle.getBottom().y);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getTopCenterRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getTopLeft().x + rectangle.width/2, rectangle.getTop().y);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getLeftCenterRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getLeft().x, rectangle.getTopLeft().y + rectangle.height/2);
        return new Rectangle(p1, p1);
    }
    
    /**
     * @param rectangle The reference rectangle
     * @return The adjacent rectangle
     */
    public static Rectangle getRightCenterRect(Rectangle rectangle) {
        Point p1 = new Point(rectangle.getRight().x, rectangle.getTopRight().y + rectangle.height/2);
        return new Rectangle(p1, p1);
    }
    
}
