/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;


/**
 * Oval shaped border.
 *
 * @author Oliver Seebach
 */
public class RoundedLineBorder extends LineBorder {

    /** Width of the surrounding rectangle in logic points. */
    private int ovalWidth;

    /** Height of the surrounding rectangle in logic points. */ 
    private int ovalHeight;  
    
    public RoundedLineBorder(int ovalWidth, int ovalHeight) {
        super();
        this.ovalWidth = ovalWidth;
        this.ovalHeight = ovalHeight;     
    }   
    

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.draw2d.LineBorder#getInsets(org.eclipse.draw2d.IFigure)
     */
    @Override
    public Insets getInsets(IFigure figure) {
        return new Insets(getWidth());
    }
    
    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.draw2d.LineBorder#paint(org.eclipse.draw2d.IFigure, org.eclipse.draw2d.Graphics, org.eclipse.draw2d.geometry.Insets)
     */
    @Override
    public void paint(IFigure figure, Graphics graphics, Insets insets) {
        Rectangle rect = getPaintRectangle(figure, insets);
        rect.setSize(rect.width - 1, rect.height - 1); // -1 as oval doesn't fit otherwise
        tempRect.setBounds(rect);
        tempRect.shrink(getWidth() / 2, getWidth() / 2);

        graphics.setLineWidth(getWidth());
        graphics.setLineStyle(getStyle());
        if (getColor() != null) {
            graphics.setForegroundColor(getColor());
        } else {
            graphics.setForegroundColor(figure.getForegroundColor());
        }

        graphics.drawRoundRectangle(tempRect, ovalWidth, ovalHeight);
    }
    
}
