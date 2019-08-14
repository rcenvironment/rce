/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Class to store outputLocation information which are then converted to json. The key in the map corresponds to the filename of the
 * OutpuLocation.
 *
 * @author Brigitte Boden
 */
public class OutputLocationList {

    private List<OutputLocation> outputs = new ArrayList<OutputLocation>();

    /**
     * 
     * Default constructor; required by JSON.
     *
     */
    public OutputLocationList() {}

    /**
     * Adds a new location to the list or replaces an existing location, if one with the given filename exists.
     * 
     * @param o Object containing OutputLocation Information
     */
    public void addOrReplaceOutputLocation(OutputLocation o) {
        if (getOutputLocationById(o.getGroupId()) != null) {
            outputs.remove(getOutputLocationById(o.getGroupId()));
        } 
        outputs.add(o);
    }

    /**
     * Return the Output Location with the given identifier.
     * 
     * @param id The identifier of the required output location
     * @return Output Location with the given identifier
     */
    public OutputLocation getOutputLocationById(String id) {
        for (OutputLocation o : outputs) {
            if (o.getGroupId().equals(id)) {
                return o;
            }
        }
        return null;
    }

    @JsonIgnore
    public List<OutputLocation> getOutputLocations() {
        return Collections.unmodifiableList(outputs);
    }

    /**
     * Remove an output location from the list.
     * 
     * @param id identifier of the list to remove.
     */
    public void removeLocation(String id) {
        outputs.remove(getOutputLocationById(id));
    }
}
