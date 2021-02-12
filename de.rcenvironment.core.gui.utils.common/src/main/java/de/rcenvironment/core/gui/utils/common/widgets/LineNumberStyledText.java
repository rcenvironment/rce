/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.StandardFonts;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * SWT {@link StyledText} with line numbers.
 *
 * @author Hendrik Abbenhaus
 */
public class LineNumberStyledText extends StyledText implements LineStyleListener, ModifyListener, KeyListener {
    
    private static final int KEYCODE_A = 97;

    private int lineCount = 0;

    private StyleRange styleRange = new StyleRange();
    
    private MenuItem cutItem = null;
    
    private MenuItem pasteItem = null;

    public LineNumberStyledText(Composite parent, int style) {
        super(parent, style);
        addContextMenu();
        
        styleRange.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        addLineStyleListener(this);
        addModifyListener(this);
        addKeyListener(this);

        this.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
        
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
    
    private void addContextMenu(){
        Menu menu = new Menu(this);
        cutItem = new MenuItem(menu, SWT.PUSH);
        cutItem.setText("Cut\tCtrl+X");
        cutItem.addListener(SWT.Selection, new Listener(){
            @Override
            public void handleEvent(Event event){
                invokeAction(ST.CUT);
            }
        });
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy\tCtrl+C");
        item.addListener(SWT.Selection, new Listener(){
            @Override
            public void handleEvent(Event event){
                invokeAction(ST.COPY);
            }
        });
        pasteItem = new MenuItem(menu, SWT.PUSH);
        pasteItem.setText("Paste\tCtrl+V");
        pasteItem.addListener(SWT.Selection, new Listener(){
            @Override
            public void handleEvent(Event event){
                invokeAction(ST.PASTE);
            }
        });
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Select All\tCtrl+A");
        item.addListener(SWT.Selection, new Listener(){
            @Override
            public void handleEvent(Event event){
                selectAll();
            }
        });

        this.setMenu(menu); 
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
    
    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        if (cutItem == null || pasteItem == null){
            return;
        }
        cutItem.setEnabled(editable);
        pasteItem.setEnabled(editable);
    }

    /**
     * <code>true</code> sets the background-color to white;<br>
     * <code>false</code> sets the background-color to gray.
     * 
     * @param enabled parameter
     */
    public void setBackgroundEnabled(boolean enabled) {
        if (enabled) {
            getCaret().setVisible(false);
            setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        } else {
            setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            getCaret().setVisible(true);
        }
    }

    @Override
    public void modifyText(ModifyEvent arg0) {
        if (lineCount != getLineCount()) {
            updateLineNumbers();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.stateMask == SWT.CTRL && e.keyCode == KEYCODE_A) {
            this.selectAll();
        }
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        // TODO Auto-generated method stub
        
    }

}
