/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;
import java.util.UUID;

import org.eclipse.draw2d.geometry.Dimension;

import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;

/**
 * A Label within a {@link WorkflowDescription}.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabel extends PropertiesChangeSupport implements Serializable {

    /** Constant for the palette name entry. */
    public static final String PALETTE_ENTRY_NAME = "Add Label";

    /** Constant for label initial text. */
    public static final String INITIAL_TEXT = "New label";

    /** Constant for label alpha. */
    public static final int STANDARD_ALPHA = 128;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_BLUE = 173;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_GREEN = 176;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_RED = 155;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_BLACK = 0;

    /** Property that is fired when a label property changes. */
    public static final String PROPERTY_CHANGE = "de.rcenvironment.rce.component.workflow.WorkflowLabelProperty";

    private static final int INT_255 = 255;

    /**
     * Constant.
     */
    private static final long serialVersionUID = 3420597804308218542L;

    private String text;

    private int x;

    private int y;

    private Dimension size;

    private int alpha = STANDARD_ALPHA;

    private String identifier;

    private int[] colorBackground;

    private int[] colorText;

    public WorkflowLabel(String text) {
        this.text = text;
        identifier = UUID.randomUUID().toString();
        setColorBackground(new int[] { STANDARD_COLOR_RED, STANDARD_COLOR_GREEN, STANDARD_COLOR_BLUE });
        setColorText(new int[] { STANDARD_COLOR_BLACK, STANDARD_COLOR_BLACK, STANDARD_COLOR_BLACK });
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * @param newX The new X location.
     * @param newY The new Y location.
     */
    public void setLocation(int newX, int newY) {
        setX(newX);
        setY(newY);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Dimension getSize() {
        return size;
    }

    /**
     * Resizes the label and throws a property change event.
     * 
     * @param size new size
     */
    public void setSize(Dimension size) {
        this.size = size;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getAlphaDisplay() {
        return -alpha + INT_255;
    }

    /**
     * Enhanced setter.
     * 
     * @param alpha new value
     */
    public void setAlpha(int alpha) {
        this.alpha = INT_255 - alpha;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int[] getColorBackground() {
        return colorBackground;
    }

    /**
     * Enhanced setter.
     * 
     * @param color new value
     */
    public void setColorBackground(int[] color) {
        this.colorBackground = color;
    }

    public int[] getColorText() {
        return colorText;
    }

    /**
     * Enhanced setter.
     * 
     * @param colorText new value
     */
    public void setColorText(int[] colorText) {
        this.colorText = colorText;
    }

}
