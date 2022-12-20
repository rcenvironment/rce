/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Weinert
 */
public final class EndpointAdapters implements Iterable<EndpointAdapter> {

    private final List<EndpointAdapter> definitions = new LinkedList<>();

    // We make the Constructor private in order to enforce use of the builder class
    private EndpointAdapters() {}

    /**
     * Endpoint adapter builder.
     *
     * @author Alexander Weinert
     * @author Jan Flink
     */
    public static class Builder {

        private final EndpointAdapters product = new EndpointAdapters();

        public Builder addEndpointAdapter(EndpointAdapter def) {
            this.product.definitions.add(def);
            return this;
        }

        public EndpointAdapters build() {
            final Map<String, List<EndpointAdapter>> externalInputNameCount = this.product.definitions.stream()
                .filter(EndpointAdapter::isInputAdapter)
                .collect(Collectors.groupingBy(EndpointAdapter::getExternalName));
            final Map<String, List<EndpointAdapter>> externalOutputNameCount = this.product.definitions.stream()
                .filter(EndpointAdapter::isOutputAdapter)
                .collect(Collectors.groupingBy(EndpointAdapter::getExternalName));

            StringBuilder offendingEndpointAdapterDescriptions = new StringBuilder();
            for (Map.Entry<String, List<EndpointAdapter>> countEntry : externalInputNameCount.entrySet()) {
                if (countEntry.getValue().size() > 1) {
                    final String endpointAdapterDescriptions = countEntry.getValue().stream()
                        .map(EndpointAdapter::toString)
                        .collect(Collectors.joining(", ", "[", "]"));

                    offendingEndpointAdapterDescriptions.append(endpointAdapterDescriptions);
                }
            }
            for (Map.Entry<String, List<EndpointAdapter>> countEntry : externalOutputNameCount.entrySet()) {
                if (countEntry.getValue().size() > 1) {
                    final String endpointAdapterDescriptions = countEntry.getValue().stream()
                        .map(EndpointAdapter::toString)
                        .collect(Collectors.joining(", ", "[", "]"));

                    offendingEndpointAdapterDescriptions.append(endpointAdapterDescriptions);
                }
            }

            if (!offendingEndpointAdapterDescriptions.toString().equals("")) {
                final String errorMessage =
                    "The following EndpointAdapterDefinitions do not have disjoint external names: " + offendingEndpointAdapterDescriptions;
                throw new IllegalArgumentException(errorMessage);
            }

            return product;
        }
    }

    @Override
    public Iterator<EndpointAdapter> iterator() {
        return this.definitions.iterator();
    }

    public Stream<EndpointAdapter> stream() {
        return this.definitions.stream();
    }

    public boolean containsAdapterWithInternalEndpointName(final String internalEndpoint) {
        return this.stream()
            .anyMatch(definition -> definition.getInternalName().equals(internalEndpoint));
    }

    /**
     * @throws NoSuchElementException If no endpoint adapter adapts an endpoint of the given name
     * @param internalEndpointName The name of some adapted endpoint
     * @return Some EndpointAdapter that adapts an endpoint of the given name
     */
    public EndpointAdapter getByInternalEndpointName(final String internalEndpointName) {
        return this.stream()
            .filter(definition -> definition.getInternalName().equals(internalEndpointName))
            .findAny().get();
    }

    /**
     * @throws NoSuchElementException If no endpoint adapter exposes an endpoint with the given name
     * @param externalEndpointName The name of some exposed endpoint
     * @return Some EndpointAdapter that exposes an endpoint with the given name. If multiple such adapters exist, an arbitrary one is
     *         returned.
     */
    public EndpointAdapter getByExternalEndpointName(final String externalEndpointName) {
        return this.stream()
            .filter(definition -> definition.getExternalName().equals(externalEndpointName))
            .findAny().get();
    }

    public boolean containsOutputAdapters() {
        return this.stream()
            .anyMatch(definition -> definition.isOutputAdapter());
    }

    public boolean isEmpty() {
        return this.definitions.isEmpty();
    }
}
