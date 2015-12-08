/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;

/**
 * Class to store x and y location independent from the graphical framework.
 * 
 * @author Oliver Seebach
 * 
 * TODO make immutable to allow passing instances from gui to model and vice versa without side effects and without creating new
 * instances - seid_do, April 2015
 */
public class Location implements Serializable, Comparable<Location> {

    private static final long serialVersionUID = 3311137685734060246L;

    /** X coordinate. */
    public final int x;

    /** Y coordinate. */
    public final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public String toString() {
        return (this.x + ":" + this.y);
    }

    @Override
    public int compareTo(Location o) {
        int result = Integer.valueOf(x).compareTo(Integer.valueOf(o.x));
        if (result == 0) {
            result = Integer.valueOf(y).compareTo(Integer.valueOf(o.y));
        }
        return result;
    }

}
