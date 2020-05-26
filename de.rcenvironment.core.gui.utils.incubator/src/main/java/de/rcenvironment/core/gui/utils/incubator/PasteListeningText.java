/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import java.util.ArrayList;
import java.util.List;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Paste Listening Text.
 * 
 * @author Hendrik Abbenhaus
 */
public class PasteListeningText extends Text implements ModifyListener {

    private List<PasteListener> li = new ArrayList<PasteListener>();

    private boolean listenerActive = true;

    public PasteListeningText(Composite parent, int style) {
        super(parent, style);
        this.addModifyListener(this);
    }

    /**
     * Adds a paste-Listener.
     * 
     * @param listener the listener
     */
    public void addPasteListener(PasteListener listener) {
        li.add(listener);
    }

    private void notifyPasteListener(String text) {
        for (PasteListener l : li) {
            l.paste(text);
        }
    }

    @Override
    public void setText(String text) {
        listenerActive = false;
        super.setText(text);
        listenerActive = true;
    }

    @Override
    protected void checkSubclass() {
        // super.checkSubclass();
    }

    /**
     * Paste Listener for {@link PasteListeningText}.
     * 
     * @author Hendrik Abbenhaus
     * @author Oliver Seebach
     * @author Dominik Schneider
     */
    public interface PasteListener {

        /**
         * Paste-Method.
         * 
         * @param text the pasted text
         */
        void paste(String text);
    }

    @Override
    public void modifyText(ModifyEvent arg0) {
        if (listenerActive) {
            // Necessary to distinguish between modification and pasting
            // Pasting will trigger an automatic host+port detection, a modification won't

            if (isTextPasted()) {
                notifyPasteListener(getText().trim());
            }

        }

    }

    private boolean isTextPasted() {
        // Compares last clipboard entry with actual text in the textfield

        String clipboardText = "";

        clipboardText = ClipboardHelper.getContentAsStringOrNull();

        if (clipboardText != null) {
            String text = getText().trim();
            clipboardText = clipboardText.trim();
            return (!clipboardText.isEmpty() && clipboardText.equals(text));
        } else {
            return false;
        }
    }

}
