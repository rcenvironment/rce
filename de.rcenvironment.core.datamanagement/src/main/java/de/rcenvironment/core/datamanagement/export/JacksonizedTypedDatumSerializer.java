/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.datamanagement.export;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.testutils.TypedDatumSerializerDefaultStub;

/**
 * Wrapper to enable the usage of the TypedDatumSerializer in the serialization process of workflow data.
 *
 * @author Tobias Brieden
 */
public class JacksonizedTypedDatumSerializer extends JsonSerializer<TypedDatum> {

    private TypedDatumSerializer tds;

    public JacksonizedTypedDatumSerializer() {
        tds = new TypedDatumSerializerDefaultStub();
    }

    @Override
    public void serialize(TypedDatum datum, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException, JsonProcessingException {
        
        jsonGenerator.writeObject(tds.serialize(datum));
    }

}
