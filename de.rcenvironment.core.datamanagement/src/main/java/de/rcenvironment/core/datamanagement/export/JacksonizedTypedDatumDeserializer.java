/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.testutils.TypedDatumSerializerDefaultStub;

/**
 * Wrapper to enable the usage of the TypedDatumSerializer in the deserialization process of workflow data.
 *
 * @author Tobias Brieden
 */
public class JacksonizedTypedDatumDeserializer extends JsonDeserializer<TypedDatum> {

    private TypedDatumSerializer tds;

    public JacksonizedTypedDatumDeserializer() {
        tds = new TypedDatumSerializerDefaultStub();
    }

    @Override
    public TypedDatum deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException, JsonProcessingException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return tds.deserialize(node.asText());
    }

}
