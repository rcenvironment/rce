/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.utils.incubator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Paste Listening Text.
 * @author Hendrik Abbenhaus
 */
public class PasteListeningText extends Text implements ModifyListener{
    private List<PasteListener> li = new ArrayList<PasteListener>();
    private String content = "";
    private boolean listenerActive = true;
    
    public PasteListeningText(Composite parent, int style) {
        super(parent, style);
        this.addModifyListener(this);
    }
    
  
    /**
     * Adds a paste-Listener.
     * @param listener the listener 
     */
    public void addPasteListener(PasteListener listener) {
        li.add(listener);
    }
    
    
    private void notifyPasteListener(String text) {
        for (PasteListener l : li){
            l.paste(text);
        }
    }
    
    @Override
    public void setText(String string) {
        listenerActive = false;
        super.setText(string);
        listenerActive = true;
    }
    
    @Override
    protected void checkSubclass() {
        //super.checkSubclass();
    }
    
    /**
     * Paste Listener for {@link PasteListeningText}.
     * @author Hendrik Abbenhaus
     */
    public interface PasteListener{
        /**
         * Paste-Method. 
         * @param text the pasted text
         */
        void paste(String text);
    }

    @Override
    public void modifyText(ModifyEvent arg0) {
        if (!listenerActive){
            return;
        }
        int count = getText().length() - content.length();
        if (count >  1){
            String pasteString = getText().substring(getCaretPosition() - count, getCaretPosition());
            notifyPasteListener(pasteString);
        }
        content = getText();
    }
    
}
