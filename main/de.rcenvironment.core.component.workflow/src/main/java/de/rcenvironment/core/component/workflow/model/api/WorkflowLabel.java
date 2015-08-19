/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;
import java.util.UUID;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;

import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A Label within a {@link WorkflowDescription}.
 * 
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author Doreen Seider
 */
public class WorkflowLabel extends PropertiesChangeSupport implements Serializable {

    /** Constant for the palette name entry. */
    public static final String PALETTE_ENTRY_NAME = "Add Label";

    /** Constant for the default fonz size. */
    public static final int DEFAULT_FONT_SIZE = 9;
    
    /** Constant for label initial text. */
    public static final String INITIAL_TEXT = "New label";

    /** Constant for label alpha. */
    public static final int STANDARD_ALPHA = 128;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_BLUE = 0x22;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_GREEN = 0x92;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_RED = 0x39;

    /** Constant for label color. */
    public static final int STANDARD_COLOR_BLACK = 0;

    /** Property that is fired when a label property changes. */
    public static final String PROPERTY_CHANGE = "de.rcenvironment.rce.component.workflow.WorkflowLabelProperty";

    /** Property that is fired when a label command property changes. */
    public static final String COMMAND_CHANGE = "de.rcenvironment.rce.component.workflow.WorkflowLabelCommand";

    private static final int INT_255 = 255;

    private static final int CENTER_LABEL_ALIGNMENT = PositionConstants.CENTER;

    private static final int LEFT_LABEL_ALIGNMENT = PositionConstants.LEFT;

    private static final int RIGHT_LABEL_ALIGNMENT = PositionConstants.RIGHT;

    private static final int CENTER_TEXT_ALIGNMENT = PositionConstants.CENTER;

    private static final int TOP_TEXT_ALIGNMENT = PositionConstants.TOP;

    private static final int BOTTOM_TEXT_ALIGNMENT = PositionConstants.BOTTOM;

    private static final long serialVersionUID = 3420597804308218542L;

    private String text;

    private int x;

    private int y;

    private Dimension size;

    private int alpha = STANDARD_ALPHA;

    private String identifier;

    private int[] colorBackground;

    private int[] colorText;

    private boolean hasBorder;

    private AlignmentType alignmentType;
    
    private int textSize;

    public WorkflowLabel(String text) {
        this.text = text;
        identifier = UUID.randomUUID().toString();
        setColorBackground(new int[] { STANDARD_COLOR_RED, STANDARD_COLOR_GREEN, STANDARD_COLOR_BLUE });
        setColorText(new int[] { STANDARD_COLOR_BLACK, STANDARD_COLOR_BLACK, STANDARD_COLOR_BLACK });
        setAlignmentType(AlignmentType.CENTER);
        setHasBorder(false);
        setTextSize(DEFAULT_FONT_SIZE);
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

    /**
     * @return <code>true</code> if {@link WorkflowLabel} has border, otherwise <code>false</code>
     */
    public boolean hasBorder() {
        return hasBorder;
    }

    public void setHasBorder(boolean hasBorder) {
        this.hasBorder = hasBorder;
    }

    public AlignmentType getAlignmentType() {
        return alignmentType;
    }

    public void setAlignmentType(AlignmentType alignmentType) {
        this.alignmentType = alignmentType;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }
    
    /**
     * Fires property change event.
     */
    public void firePropertChangeEvent() {
        firePropertyChange(PROPERTY_CHANGE);
    }

    @Override
    public String toString() {
        return StringUtils.format("'%s' (x=%d, y=%d, size=%s, alpha=%d, color=%d %d %d, background=%d %d %d, border=%b, font size=%d)", 
            text, x, y, size.toString(), alpha, colorText[0], colorText[1], colorText[2], colorBackground[0], colorBackground[1],
            colorBackground[2], hasBorder, textSize);
    }

    /**
     * Contains all label alignment types.
     * 
     * @author Marc Stammerjohann
     */
    public enum AlignmentType {

        /** top left. */
        TOPLEFT(LEFT_LABEL_ALIGNMENT, TOP_TEXT_ALIGNMENT),
        /** top center. */
        TOPCENTER(CENTER_LABEL_ALIGNMENT, TOP_TEXT_ALIGNMENT),
        /** top right. */
        TOPRIGHT(RIGHT_LABEL_ALIGNMENT, TOP_TEXT_ALIGNMENT),
        /** center left. */
        CENTERLEFT(LEFT_LABEL_ALIGNMENT, CENTER_TEXT_ALIGNMENT),
        /** center. */
        CENTER(CENTER_LABEL_ALIGNMENT, CENTER_TEXT_ALIGNMENT),
        /** center right. */
        CENTERRIGHT(RIGHT_LABEL_ALIGNMENT, CENTER_TEXT_ALIGNMENT),
        /** bottom left. */
        BOTTOMLEFT(LEFT_LABEL_ALIGNMENT, BOTTOM_TEXT_ALIGNMENT),
        /** bottom center. */
        BOTTOMCENTER(CENTER_LABEL_ALIGNMENT, BOTTOM_TEXT_ALIGNMENT),
        /** bottom right. */
        BOTTOMRIGHT(RIGHT_LABEL_ALIGNMENT, BOTTOM_TEXT_ALIGNMENT);

        private int labelAlignment;

        private int textAlignment;

        AlignmentType(int labelAlignment, int textAlignment) {
            this.labelAlignment = labelAlignment;
            this.textAlignment = textAlignment;
        }

        public int getLabelAlignment() {
            return labelAlignment;
        }

        public int getTextAlignment() {
            return textAlignment;
        }

    }

}
