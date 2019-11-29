/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.common.spi;

import java.util.Collection;
import java.util.List;

/**
 * An interface for services that offer monitoring information in form of text lines. Each contributor may offer multiple "topics", which
 * can be enabled or disabled by the user.
 * 
 * @author Robert Mischke
 */
public interface PeriodicMonitoringDataContributor {

    /**
     * @return the available topic IDs (as map keys), with a short description (as map values)
     */
    Collection<String> getTopicIds();

    /**
     * @param topicId a topic id
     * @return a short description for the given monitoring topic
     */
    String getTopicDescription(String topicId);

    /**
     * Generates the monitoring output for the given topic, and appends it to the provided list. A contributor MUST NOT edit or remove any
     * existing entries in the {@link List}.
     * 
     * @param topicId the topic to generate output for
     * @param collection the list to append the output lines to
     */
    void generateOutput(String topicId, List<String> collection);

}
