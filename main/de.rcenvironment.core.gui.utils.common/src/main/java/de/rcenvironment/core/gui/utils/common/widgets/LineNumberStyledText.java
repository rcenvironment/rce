/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * SWT {@link StyledText} with line numbers.
 *
 * @author Hendrik Abbenhaus
 */
public class LineNumberStyledText extends StyledText implements LineStyleListener {

    private int lineCount = 0;

    private StyleRange styleRange = new StyleRange();

    public LineNumberStyledText(Composite parent, int style) {
        super(parent, style);
        styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        addLineStyleListener(this);

        addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (lineCount != getLineCount()) {
                    updateLineNumbers();
                }
            }
        });

        updateLineNumbers();
    }

    @Override
    public void lineGetStyle(LineStyleEvent arg0) {
        int bulletLength = (Integer.toString(lineCount).length());
        int bulletWidth = (bulletLength + 1) * getLineHeight() / 2;
        styleRange.metrics = new GlyphMetrics(0, 0, bulletWidth);
        if (arg0.bullet == null) {
            arg0.bullet = new Bullet(ST.BULLET_TEXT, styleRange);
        } else {
            arg0.bullet.style = styleRange;
        }
        int bulletLine = getLineAtOffset(arg0.lineOffset) + 1;
        arg0.bullet.text = StringUtils.format("%" + bulletLength + "s", bulletLine);
        arg0.tabStops = new int[] { bulletWidth, 3 * bulletWidth };
    }

    /**
     * Update line-numbers by setting the text again.
     * 
     */
    private void updateLineNumbers() {
        int sel = getCaretOffset();
        int scrollbarSel = getTopIndex();
        lineCount = getLineCount();
        super.setText(getText());
        setCaretOffset(sel);
        setTopIndex(scrollbarSel);
    }

    @Override
    public void setEnabled(boolean enabled) {
        setSelection(0);
        setBackgroundEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        updateLineNumbers();
    }

    /**
     * <code>true</code> sets the background-color to white;<br>
     * <code>false</code> sets the background-color to gray.
     * 
     * @param enabled parameter
     */
    public void setBackgroundEnabled(boolean enabled) {
        if (enabled) {
            setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        } else {
            setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
    }

}
