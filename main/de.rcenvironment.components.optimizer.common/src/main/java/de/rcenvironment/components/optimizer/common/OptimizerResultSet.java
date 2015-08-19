/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Class holding one set of values for an optimizer output.
 * 
 * @author Sascha Zur
 */
public class OptimizerResultSet implements Serializable {

    private static final long serialVersionUID = -5549958046464158432L;

    private static TypedDatumSerializer typedDatumSerializer;

    private final Map<String, String> values = new HashMap<String, String>();

    private String component;

    @Deprecated
    public OptimizerResultSet() {

    }

    public OptimizerResultSet(final Map<String, TypedDatum> values2, String component) {
        for (String key : values2.keySet()) {
            values.put(key, typedDatumSerializer.serialize(values2.get(key)));
        }
        this.setComponent(component);
    }

    /**
     * @param key the key of the value to get.
     * @return the value.
     */
    public TypedDatum getValue(final String key) {
        return typedDatumSerializer.deserialize(values.get(key));
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * Getter for all values.
     * 
     * @return map with values.
     */
    public Map<String, TypedDatum> getValues() {
        Map<String, TypedDatum> result = new HashMap<String, TypedDatum>();
        for (String key : values.keySet()) {
            result.put(key, typedDatumSerializer.deserialize(values.get(key)));
        }
        return result;
    }

    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumSerializer = newTypedDatumService.getSerializer();
    }

    protected void unbindTypedDatumService(TypedDatumService oldTypedDatumService) {}
}
