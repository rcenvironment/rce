/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;
import java.util.UUID;

import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A Label within a {@link WorkflowDescription}.
 * 
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author Doreen Seider
 */
public class WorkflowLabel extends PropertiesChangeSupport implements Serializable, Comparable<WorkflowLabel> {

    /** Constant for the palate name entry. */
    public static final String PALETTE_ENTRY_NAME = "Add Label";

    /** Constant for default header font size. */
    public static final int DEFAULT_HEADER_FONT_SIZE = 12;

    /** Constant for the default font size. */
    public static final int DEFAULT_FONT_SIZE = 9;

    /** Constant for the default width. */
    public static final int DEFAULT_WIDTH = 121;

    /** Constant for the default height. */
    public static final int DEFAULT_HEIGHT = 61;

    /** Constant for label initial text. */
    public static final String INITIAL_TEXT = "New label";

    /** Constant for label alpha. */
    public static final int DEFAULT_ALPHA = 128;

    /** Constant for label color. */
    public static final int DEFAULT_COLOR_BLUE = 0x22;

    /** Constant for label color. */
    public static final int DEFAULT_COLOR_GREEN = 0x92;

    /** Constant for label color. */
    public static final int DEFAULT_COLOR_RED = 0x39;

    /** Constant for label color. */
    public static final int DEFAULT_COLOR_BLACK = 0;

    /** Property that is fired when a label property changes. */
    public static final String PROPERTY_CHANGE = "de.rcenvironment.rce.component.workflow.WorkflowLabelProperty";

    /** Property that is fired when a label command property changes. */
    public static final String COMMAND_CHANGE = "de.rcenvironment.rce.component.workflow.WorkflowLabelCommand";

    private static final int INT_255 = 255;

    private static final long serialVersionUID = 3420597804308218542L;

    private static final int INITIAL_ZINDEX = -1;

    private String headerText;

    private String text;

    private int x;

    private int y;

    private int width;

    private int height;

    private int alpha = DEFAULT_ALPHA;

    private String identifier;

    private int[] colorBackground;

    private int[] colorHeader;

    private int[] colorText;

    private boolean hasBorder;

    private LabelPosition labelPosition;

    private TextAlignmentType textAlignmentType;

    private TextAlignmentType headerAlignmentType;

    private int headerTextSize;

    private int textSize;

    private int zIndex;

    public WorkflowLabel(String text) {
        this.text = text;
        this.headerText = "";
        identifier = UUID.randomUUID().toString();
        setColorBackground(new int[] { DEFAULT_COLOR_RED, DEFAULT_COLOR_GREEN, DEFAULT_COLOR_BLUE });
        setColorHeader(new int[] { DEFAULT_COLOR_BLACK, DEFAULT_COLOR_BLACK, DEFAULT_COLOR_BLACK });
        setColorText(new int[] { DEFAULT_COLOR_BLACK, DEFAULT_COLOR_BLACK, DEFAULT_COLOR_BLACK });
        setLabelPosition(LabelPosition.CENTER);
        setTextAlignmentType(TextAlignmentType.LEFT);
        setHeaderAlignmentType(TextAlignmentType.CENTER);
        setHasBorder(false);
        setHeaderTextSize(DEFAULT_HEADER_FONT_SIZE);
        setTextSize(DEFAULT_FONT_SIZE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        zIndex = INITIAL_ZINDEX;
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
        firePropertyChange(PROPERTY_CHANGE);
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

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @param newHeight the new height
     * @param newWidth the new width
     */
    public void setSize(int newWidth, int newHeight) {
        setWidth(newWidth);
        setHeight(newHeight);
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

    public LabelPosition getLabelPosition() {
        return labelPosition;
    }

    public void setLabelPosition(LabelPosition labelPosition) {
        this.labelPosition = labelPosition;
    }

    public TextAlignmentType getTextAlignmentType() {
        return textAlignmentType;
    }

    public void setTextAlignmentType(TextAlignmentType textAlignmentType) {
        this.textAlignmentType = textAlignmentType;
    }

    public TextAlignmentType getHeaderAlignmentType() {
        return headerAlignmentType;
    }

    public void setHeaderAlignmentType(TextAlignmentType headerAlignmentType) {
        this.headerAlignmentType = headerAlignmentType;
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
        return StringUtils.format("'%s' (x=%d, y=%d, height=%d, width=%d alpha=%d, color=%d %d %d, background=%d %d %d, border=%b,"
            + " font size=%d)", text, x, y, height, width, alpha, colorText[0], colorText[1], colorText[2], colorBackground[0],
            colorBackground[1], colorBackground[2], hasBorder, textSize);
    }

    @Override
    public int compareTo(WorkflowLabel o) {
        return getIdentifier().compareTo(o.getIdentifier());
    }

    /**
     * Contains label alignment type.
     * 
     * @author Jascha Riedel
     */
    public enum LabelPosition {
        /** Position. */
        TOPLEFT,
        /** Position. */
        TOPCENTER,
        /** Position. */
        TOPRIGHT,
        /** Position. */
        CENTERLEFT,
        /** Position. */
        CENTER,
        /** Position. */
        CENTERRIGHT,
        /** Position. */
        BOTTOMLEFT,
        /** Position. */
        BOTTOMCENTER,
        /** Position. */
        BOTTOMRIGHT;
    }

    /**
     * 
     * Contains text alignment type for maintext and header.
     *
     * @author Jascha Riedel
     */
    public enum TextAlignmentType {
        /** Alignment. */
        LEFT,
        /** Alignment. */
        CENTER,
        /** Alignment. */
        RIGHT;
    }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public int[] getColorHeader() {
        return colorHeader;
    }

    public void setColorHeader(int[] colorHeader) {
        this.colorHeader = colorHeader;
    }

    public int getHeaderTextSize() {
        return headerTextSize;
    }

    public void setHeaderTextSize(int headerTextSize) {
        this.headerTextSize = headerTextSize;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zindex) {
        this.zIndex = zindex;
    }

}
