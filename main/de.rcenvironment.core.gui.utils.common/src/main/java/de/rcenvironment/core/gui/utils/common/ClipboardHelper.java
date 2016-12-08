/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;

/**
 * Helper for accessing the system clipboard.
 * 
 * This helper currently only allows the handling of {@link String}s.
 * 
 * @author Tobias Rodehutskors
 */
public final class ClipboardHelper {

    private static final Log LOG = LogFactory.getLog(ClipboardHelper.class);

    /**
     * This class has only static methods.
     */
    private ClipboardHelper() {
        // only static methods
    }

    /**
     * Helper to copy {@link String}s onto the system clipboard.
     * 
     * @param content The content to copy onto the clipboard.
     */
    public static void setContent(String content) {
        // This method should be overloaded to allow handling of different types.
        // This function should be refactored if more transfer data types should be supported to avoid code duplication
        // TODO Maybe we should store an instance of the clipboard for reuse?
        Clipboard cb = new Clipboard(Display.getDefault());
        TextTransfer textTransfer = TextTransfer.getInstance();
        try {
            cb.setContents(new Object[] { content }, new Transfer[] { textTransfer });
        } catch (IllegalArgumentException e) { // content is null
            // Empty content cannot be copied onto the clipboard
        } // TODO there are more possible exceptions
        cb.dispose();
    }

    /**
     * Helper to get a {@link String} from the system clipboard. Returns
     * 
     * @return a {@link String} if the clipboard contains content of the type TextTransfer, otherwise it returns null.
     */
    public static String getContentAsStringOrNull() {
        // This function should be refactored if more transfer data types should be supported to avoid code duplication

        Clipboard cb = new Clipboard(Display.getDefault());

        // Check if the requested type is currently available on the system clipboard. This is faster and more reliable than directly
        // accessing the clipboard, according to https://eclipse.org/articles/Article-SWT-DND/DND-in-SWT.html.
        TransferData[] availableTypes = cb.getAvailableTypes();
        boolean textTransferAvailable = false;
        for (TransferData availableType : availableTypes) {
            if (TextTransfer.getInstance().isSupportedType(availableType)) {
                textTransferAvailable = true;
                break;
            }
        }

        if (!textTransferAvailable) {
            cb.dispose();
            return null;
        } else {
            TextTransfer textTransfer = TextTransfer.getInstance();
            String clipboardText = (String) cb.getContents(textTransfer);
            cb.dispose();
            return clipboardText;
        }
    }
}
