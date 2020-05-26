/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 * @author Misiak Martin
 */
public class CopyFullpathHandler extends AbstractHandler {

    public CopyFullpathHandler() {}

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        String fullPath = "";
        ISelectionService selService = window.getSelectionService();
        IStructuredSelection selection = (IStructuredSelection) selService.getSelection();

        if (selection.getFirstElement() instanceof IResource) {
            IResource file = (IResource) selection.getFirstElement();
            IPath path = file.getLocation();
            
            fullPath = path.toOSString();
        }

        ClipboardHelper.setContent(fullPath);

        return null;
    }
}
