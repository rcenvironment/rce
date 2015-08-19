/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;



/**
 * SelectionProvider which works with multiple view elements.
 *
 * @author Markus Kunde
 */
public class SelectionProviderIntermediate implements IPostSelectionProvider {

    /**
     * Selection provider, makes it possible to change selection providers during lifecycle.
     */
    private ISelectionProvider delegate = null;
    
    /**
     * All selection listeners.
     */
    private final ListenerList selectionListeners = new ListenerList();
    
    /**
     * All post selection listeners.
     */
    private final ListenerList postSelectionListeners = new ListenerList();
    
    /**
     * Selection listener with selection changed event listener.
     */
    private ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            if (event.getSelectionProvider() == delegate) {
                fireSelectionChanged(event.getSelection());
            }
        }
    };

    /**
     * Post selection listener with selection changed event listener.
     */
    private ISelectionChangedListener postSelectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            if (event.getSelectionProvider() == delegate) {
                firePostSelectionChanged(event.getSelection());
            }
        }
    };
    
    /**
     * Sets a new selection provider to delegate to. Selection listeners
     * registered with the previous delegate are removed before. 
     * 
     * @param newDelegate new selection provider
     */
    public void setSelectionProviderDelegate(ISelectionProvider newDelegate) {
        if (delegate == newDelegate) {
            return;
        }
        if (delegate != null) {
            delegate.removeSelectionChangedListener(selectionListener);
            if (delegate instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) delegate).removePostSelectionChangedListener(postSelectionListener);
            }
        }
        delegate = newDelegate;
        if (newDelegate != null) {
            newDelegate.addSelectionChangedListener(selectionListener);
            if (newDelegate instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) newDelegate).addPostSelectionChangedListener(postSelectionListener);
            }
            fireSelectionChanged(newDelegate.getSelection());
            firePostSelectionChanged(newDelegate.getSelection());
        }
    }
    
    /**
     * Facade between multiple selection listeners and single interface.
     * 
     * @param selection current selection
     */
    protected void fireSelectionChanged(ISelection selection) {
        fireSelectionChanged(selectionListeners, selection);
    }

    /**
     * Facade between multiple post selection listeners and single interface.
     * 
     * @param selection current post selection
     */
    protected void firePostSelectionChanged(ISelection selection) {
        fireSelectionChanged(postSelectionListeners, selection);
    }

    /**
     * Fire selection changed event to all selection listeners.
     * 
     * @param list list of all listeners
     * @param selection current selection
     */
    private void fireSelectionChanged(ListenerList list, ISelection selection) {
        SelectionChangedEvent event = new SelectionChangedEvent(delegate, selection);
        Object[] listeners = list.getListeners();
        for (Object listener2 : listeners) {
            ISelectionChangedListener listener = (ISelectionChangedListener) listener2;
            listener.selectionChanged(event);
        }
    }
    
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionListeners.add(listener);
    }

    @Override
    public ISelection getSelection() {
        if (delegate == null) {
            return null;
        }
        return delegate.getSelection();
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionListeners.remove(listener);
    }

    @Override
    public void setSelection(ISelection selection) {
        if (delegate != null) {
            delegate.setSelection(selection);
        }

    }

    @Override
    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
        postSelectionListeners.add(listener);
    }

    @Override
    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
        postSelectionListeners.remove(listener);
    }

}
