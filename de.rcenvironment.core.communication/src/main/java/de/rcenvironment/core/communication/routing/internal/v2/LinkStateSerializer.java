/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Converter between {@link LinkState} objects and their string representation.
 * 
 * @author Robert Mischke
 */
public final class LinkStateSerializer {

    private LinkStateSerializer() {}

    /**
     * Local stub to simplify JSON generation and parsing.
     * 
     * @author Robert Mischke
     */
    private static class LinkStateJsonStub {

        public String id; // link id

        public String node; // NodeIdentifier string

        /**
         * Constructor for JSON deserialization.
         */
        @SuppressWarnings("unused")
        LinkStateJsonStub() {}

        LinkStateJsonStub(Link link) {
            id = link.getLinkId();
            node = link.getNodeIdString();
        }

        public Link toLink() {
            return new Link(id, node);
        }
    }

    /**
     * Converts a {@link LinkState} to its string representation.
     * 
     * @param state the LinkState
     * @return the string representation
     */
    public static String serialize(LinkState state) {
        return serialize(state.getLinks());
    }

    /**
     * Convenience method: Converts a list of {@link Link}s to an equivalent {@link LinkState}
     * string representation.
     * 
     * @param links the list of {@link Link}s
     * @return the string representation
     */
    public static String serialize(Collection<Link> links) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            List<LinkStateJsonStub> stubList = new ArrayList<LinkStateSerializer.LinkStateJsonStub>();
            for (Link link : links) {
                stubList.add(new LinkStateJsonStub(link));
            }
            return mapper.writeValueAsString(stubList);
        } catch (IOException e) {
            // should never happen
            throw new IllegalStateException("Error serializing local link state", e);
        }
    }

    /**
     * Restores a {@link LinkState} from its string representation.
     * 
     * @param serialized the string representation
     * @return the restored
     * @throws IOException on failure to parse the given string
     */
    public static LinkState deserialize(String serialized) throws IOException {

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            List<LinkStateJsonStub> stubList =
                mapper.readValue(serialized, new TypeReference<List<LinkStateJsonStub>>() {
                });
            List<Link> links = new ArrayList<Link>();
            for (LinkStateJsonStub stub : stubList) {
                links.add(stub.toLink());
            }
            return new LinkState(links);
        } catch (IOException e) {
            throw new IOException("Failed to parse link state data: linkStateData", e);
        }
    }
}
