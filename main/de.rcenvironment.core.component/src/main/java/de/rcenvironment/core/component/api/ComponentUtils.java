/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.type.TypeReference;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointGroupDefinitionImpl;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class providing utility methods used for handling component properties.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public final class ComponentUtils {

    /** Logger. */
    public static final Log LOGGER = LogFactory.getLog(ComponentUtils.class);

    /** Component version on other node is lower. */
    public static final int LOWER_COMPONENT_VERSION = -1;

    /** Component version on other node is equal. */
    public static final int EQUAL_COMPONENT_VERSION = 0;

    /** Component version on other node is greater. */
    public static final int GREATER_COMPONENT_VERSION = 1;

    /** Regex expression for all placeholder. */
    public static final String PLACEHOLDER_REGEX = "\\$\\{((\\w*)(\\.))?((\\*)(\\.))?(.*)\\}";

    /** Variable for current working directory. */
    public static final String VARIABLE_CWD = "${cwd}";

    /** Variable for current working directory. */
    public static final String VARIABLE = "${%s}";

    /** Tag used to wrap output variable names. */
    public static final String OUTPUT_TAG = "_rce_output_";

    /**
     * Suffix for identifier of missong component. Introduced to cut the link to the configuration GUI of the missing component.
     */
    public static final String MISSING_COMPONENT_PREFIX = "20ca0171b5e24e10a284af7c1d6d94e9missing_";

    /*
     * Note for the Regex: A placeholder has the form ${ATTRIBUTE1.ATTRIBUTE2.NAME} where the attributes are optional. For getting the
     * groups of a regex match, you have: group 2 : ATTRIBUTE1 group 5 : ATTRIBUTE2 group 7 : NAME where group 2 and 5 can be null if there
     * is no attribute.
     */
    /** Constant. */
    public static final int ATTRIBUTE1 = 2;

    /** Constant. */
    public static final int ATTRIBUTE2 = 5;

    /** Constant. */
    public static final int PLACEHOLDERNAME = 7;

    /** Constant. */
    public static final String PLACEHOLDER_PASSWORD_STORAGE_NODE = "placeholder";

    /** Constant. */
    public static final String GLOBALATTRIBUTE = "global";

    /** Constant. */
    public static final String ENCODEDATTRIBUTE = "*";

    /** Constant. */
    public static final String PLACEHOLDER_PASSWORD_SYMBOL = "*";

    private static final String FILE_SEPERATOR = "/";

    private ComponentUtils() {}

    /**
     * @param compDesc {@link ComponentDescription} of the component.
     * @param compInstallations given list of available {@link ComponentInstallation}s
     * @return List of nodes the component is installed on.
     */
    public static Map<NodeIdentifier, Integer> getNodesForComponent(Collection<ComponentInstallation> compInstallations,
        ComponentDescription compDesc) {

        ComponentInterface compInterface = compDesc.getComponentInstallation().getComponentRevision().getComponentInterface();
        String temp = getComponentInterfaceIdentifierWithoutVersion(compInterface.getIdentifier());

        String compInterfaceIdWithoutVersion;

        if (temp.contains(MISSING_COMPONENT_PREFIX)) {
            compInterfaceIdWithoutVersion = temp.replace(MISSING_COMPONENT_PREFIX, "");
        } else {
            compInterfaceIdWithoutVersion = temp;
        }

        Map<NodeIdentifier, Integer> identifiers = new HashMap<NodeIdentifier, Integer>();
        for (ComponentInstallation compInstallation : compInstallations) {

            ComponentInterface compInterfaceToCheck = compInstallation.getComponentRevision().getComponentInterface();
            String compInterfaceIdToCheckWithoutVersion = getComponentInterfaceIdentifierWithoutVersion(
                compInterfaceToCheck.getIdentifier());

            if (compInterfaceIdWithoutVersion.equals(compInterfaceIdToCheckWithoutVersion)) {

                NodeIdentifier node = NodeIdentifierFactory.fromNodeId(compInstallation.getNodeId());
                int compared;
                try {
                    Float versionToCheck = Float.valueOf(compInterfaceToCheck.getVersion());
                    Float version = Float.valueOf(compInterface.getVersion());
                    compared = versionToCheck.compareTo(version);

                } catch (NumberFormatException e) {
                    compared = compInterfaceToCheck.getVersion().compareTo(compInterface.getVersion());
                }
                if (compared < 0) {
                    identifiers.put(node, LOWER_COMPONENT_VERSION);
                } else if (compared > 0) {
                    identifiers.put(node, GREATER_COMPONENT_VERSION);
                } else {
                    identifiers.put(node, EQUAL_COMPONENT_VERSION);
                }
            }
        }
        return identifiers;
    }

    /**
     * Returns whether the given component is available on the given platform.
     * 
     * @param componentId Identifier of the component.
     * @param node identifier of node to check
     * @param installations given list of available {@link ComponentDescription}s
     * @return Whether the given component is available on the given platform.
     */
    public static boolean hasComponent(Collection<ComponentInstallation> installations, String componentId, NodeIdentifier node) {
        for (ComponentInstallation installation : installations) {

            if (installation.getComponentRevision().getComponentInterface().getIdentifier().equals(componentId)
                && installation.getNodeId().equals(node.getIdString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Eliminates duplicate {@link ComponentInterface}s in given list. It prefers to keep the local {@link ComponentInstallation}.
     * 
     * @param compInstallations given list of available {@link ComponentDescription}s
     * @param localNode loca node
     * @return Whether the given component is available on the given platform.
     */
    public static List<ComponentInstallation> eliminateComponentInterfaceDuplicates(Collection<ComponentInstallation> compInstallations,
        NodeIdentifier localNode) {
        List<ComponentInstallation> filteredInstallations = new ArrayList<ComponentInstallation>();

        // eliminate duplicates
        for (ComponentInstallation compInstallation : compInstallations) {

            String compInterfaceId = compInstallation.getComponentRevision().getComponentInterface().getIdentifier();
            String compInterfaceIdWithoutVersion = getComponentInterfaceIdentifierWithoutVersion(compInterfaceId);

            boolean contained = false;
            Iterator<ComponentInstallation> iterator = filteredInstallations.iterator();

            while (iterator.hasNext()) {
                String filteredCompInterfaceId = iterator.next().getComponentRevision().getComponentInterface().getIdentifier();
                String filteredCompInterfaceIdWithoutVersion = getComponentInterfaceIdentifierWithoutVersion(filteredCompInterfaceId);

                if (compInterfaceIdWithoutVersion.equals(filteredCompInterfaceIdWithoutVersion)) {
                    if (compInstallation.getNodeId().equals(localNode.getIdString())) {
                        iterator.remove();
                    } else {
                        contained = true;
                    }
                    continue;
                }
            }
            if (!contained) {
                filteredInstallations.add(compInstallation);
            }
        }

        return filteredInstallations;
    }

    /**
     * @param compInterfaceId component interface identifier
     * @return component interface identifier without version.
     */
    public static String getComponentInterfaceIdentifierWithoutVersion(String compInterfaceId) {
        return compInterfaceId.substring(0, compInterfaceId.indexOf(ComponentConstants.ID_SEPARATOR));
    }

    /**
     * @param compInterfaceId Identifier of {@link ComponentInterface} to search for
     * @param installations {@link ComponentInstallation}s to search
     * @param node the {@link NodeIdentifier} the component must be installed on
     * @return {@link ComponentInstallation} referring to {@link ComponentInstallation} identifier or <code>null</code>
     */
    public static ComponentInstallation getExactMatchingComponentInstallationForNode(String compInterfaceId,
        Collection<ComponentInstallation> installations, NodeIdentifier node) {
        for (ComponentInstallation installation : installations) {
            if (installation.getComponentRevision().getComponentInterface().getIdentifier().equals(compInterfaceId)
                && installation.getNodeId().equals(node.getIdString())) {
                return installation;
            }
        }
        return null;
    }

    /**
     * @param compInterfaceId Identifier of {@link ComponentInterface} to search for
     * @param installations {@link ComponentInstallation}s to search
     * @param node the {@link NodeIdentifier} the component must be installed on
     * @return {@link ComponentInstallation} referring to {@link ComponentInstallation} identifier or <code>null</code>
     */
    public static ComponentInstallation getComponentInstallationForNode(String compInterfaceId,
        Collection<ComponentInstallation> installations, NodeIdentifier node) {
        for (ComponentInstallation installation : installations) {
            if (getComponentInterfaceIdentifierWithoutVersion(installation.getComponentRevision().getComponentInterface().getIdentifier())
                .equals(getComponentInterfaceIdentifierWithoutVersion(compInterfaceId))
                && installation.getNodeId().equals(node.getIdString())) {
                return installation;
            }
        }
        return null;
    }

    /**
     * @param componentInterfaceId component interface id to match
     * @param componentInstallations {@link ComponentInstallation}s to consider
     * @return {@link ComponentInstallation}, which matches the given component interface id first
     */
    public static ComponentInstallation getComponentInstallation(String componentInterfaceId,
        Collection<ComponentInstallation> componentInstallations) {
        for (ComponentInstallation currentComponentInstallation : componentInstallations) {
            String currentComponentInterfaceId = currentComponentInstallation.getComponentRevision()
                .getComponentInterface().getIdentifier();
            if (currentComponentInterfaceId.equals(componentInterfaceId)) {
                return currentComponentInstallation;
            }
        }
        return null;
    }

    /**
     * Returns a placeholder {@link ComponentDescription} used if actual component of .wf file is not available.
     * 
     * @param identifier identifier of the missing component
     * @param version version of the missing component
     * @param name name of the missing component
     * @param nodeId node id of the missing component
     * @return placeholder {@link ComponentDescription}
     */
    public static ComponentInstallation createPlaceholderComponentInstallation(String identifier, String version, String name,
        String nodeId) {

        if (name == null) {
            name = "N/A";
        }

        ComponentInterfaceImpl componentInterface = new ComponentInterfaceImpl();
        componentInterface.setIdentifier(MISSING_COMPONENT_PREFIX + identifier);
        componentInterface.setDisplayName(name);
        componentInterface.setGroupName(ComponentConstants.COMPONENT_GROUP_UNKNOWN);
        componentInterface.setVersion(version);
        componentInterface.setIcon16(null);
        componentInterface.setIcon24(null);
        componentInterface.setIcon32(null);
        componentInterface.setLocalExecutionOnly(false);
        componentInterface.setPerformLazyDisposal(false);
        componentInterface.setInputDefinitionsProvider(new EndpointDefinitionsProviderImpl());
        componentInterface.setOutputDefinitionsProvider(new EndpointDefinitionsProviderImpl());
        componentInterface.setConfigurationDefinition(new ConfigurationDefinitionImpl());
        componentInterface.setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinitionImpl>());

        ComponentRevisionImpl componentRevision = new ComponentRevisionImpl();
        componentRevision.setComponentInterface(componentInterface);

        ComponentInstallationImpl componentInstallation = new ComponentInstallationImpl();
        componentInstallation.setInstallationId(componentInterface.getIdentifier());
        componentInstallation.setNodeId(nodeId);
        componentInstallation.setComponentRevision(componentRevision);

        return componentInstallation;
    }

    /**
     * Checks whether the given placeholder is a global placeholder.
     * 
     * @param placeholder :
     * @return true if it is
     */
    public static boolean isGlobalPlaceholder(String placeholder) {
        Matcher matcherOfPlaceholder = getMatcherForPlaceholder(placeholder);
        return (matcherOfPlaceholder.group(ATTRIBUTE1) != null && (matcherOfPlaceholder.group(ATTRIBUTE1)
            .equals(GLOBALATTRIBUTE) | (matcherOfPlaceholder
                .group(ATTRIBUTE2) != null && matcherOfPlaceholder.group(ATTRIBUTE2).equals(GLOBALATTRIBUTE))));
    }

    /**
     * Creates a @link {@link Matcher} for the given placeholder.
     * 
     * @param placeholder :
     * @return :
     */
    public static Matcher getMatcherForPlaceholder(String placeholder) {
        Pattern pattern = Pattern.compile(ComponentUtils.PLACEHOLDER_REGEX);
        Matcher matcher = pattern.matcher(placeholder);
        matcher.find();
        return matcher;
    }

    /**
     * Replaces output variables.
     * 
     * @param text text to replace
     * @param outputValues actual values
     * @param placeholderFormat the format the placeholders are to look for. Uses %s for name.
     * @return text with actual values replaced
     */
    public static String replaceOutputVariables(String text, Set<String> outputValues, String placeholderFormat) {
        for (String outputName : outputValues) {
            text = text.replace(StringUtils.format(placeholderFormat, outputName), OUTPUT_TAG + outputName + OUTPUT_TAG);
        }
        return text;
    }

    /**
     * Replaces property variables with actual values and consumes them.
     * 
     * @param script text to replace
     * @param properties actual values
     * @param placeholderFormat the format the placeholders are to look for. Uses %s for name.
     * @return text with actual values replaced
     */
    public static String replacePropertyVariables(String script, Map<String, String> properties, String placeholderFormat) {
        for (String propName : properties.keySet()) {
            script = script.replace(StringUtils.format(placeholderFormat, propName), "" + properties.get(propName));
        }
        return script;
    }

    /**
     * Replaces cwd variable with actual values.
     * 
     * @param text text to replace
     * @param workDir actual values
     * @return text with actual values replaced
     */
    public static String replaceCWDVariable(String text, String workDir) {
        String cwd = workDir;
        cwd = cwd.replace("\\\\", FILE_SEPERATOR);
        cwd = cwd.replace("\\", FILE_SEPERATOR);
        text = text.replace(VARIABLE_CWD, cwd);
        return text;
    }

    /**
     * Replaces variable with actual values.
     * 
     * @param text text to replace
     * @param value actual value
     * @param placeholderName name of the placeholder to replace
     * @param placeholderFormat structure of the placeholder
     * @return text with actual values replaced
     */
    public static String replaceVariable(String text, String value, String placeholderName, String placeholderFormat) {
        value = value.replace("\\\\", FILE_SEPERATOR);
        value = value.replace("\\", FILE_SEPERATOR);
        text = text.replace(StringUtils.format(placeholderFormat, placeholderName), value);
        return text;
    }

    /**
     * Wraps text with output tag.
     * 
     * @param taggedOutputName text to tag
     * @return tagged text
     */
    public static String extractOutputName(String taggedOutputName) {
        return taggedOutputName.replaceAll(OUTPUT_TAG, "");
    }

    /**
     * Checks whether the given placeholder is encrypted.
     * 
     * @param placeholder : to check.
     * @param encryptedPlaceholder : list of all encrypted placeholder.
     * @return true if it is
     */
    public static boolean isEncryptedPlaceholder(String placeholder, List<String> encryptedPlaceholder) {
        if (encryptedPlaceholder != null) {
            return encryptedPlaceholder.contains(placeholder);
        }
        return false;
    }

    /**
     * @param jsonInputStream raw description in json
     * @return list of {@link EndpointDefinition}s
     * @throws IOException if something went wrong
     */
    public static Set<EndpointGroupDefinitionImpl> extractDynamicInputGroupDefinitions(InputStream jsonInputStream) throws IOException {
        return extractInputGroupDefinitions(jsonInputStream, EndpointDefinitionConstants.JSON_KEY_DYNAMIC_INPUT_GROUPS);
    }

    /**
     * @param jsonInputStream raw description in json
     * @return list of {@link EndpointDefinition}s
     * @throws IOException if something went wrong
     */
    public static Set<EndpointGroupDefinitionImpl> extractStaticInputGroupDefinitions(InputStream jsonInputStream)
        throws IOException {
        return extractInputGroupDefinitions(jsonInputStream, EndpointDefinitionConstants.JSON_KEY_STATIC_INPUT_GROUPS);
    }

    /**
     * @param jsonInputStream raw description in json
     * @return list of {@link EndpointDefinition}s
     * @throws IOException if something went wrong
     */
    private static Set<EndpointGroupDefinitionImpl> extractInputGroupDefinitions(InputStream jsonInputStream, String key)
        throws IOException {

        Set<EndpointGroupDefinitionImpl> endpointGroups = new HashSet<EndpointGroupDefinitionImpl>();

        for (Object definition : extractDefinitionAsList(jsonInputStream, key)) {
            try {
                EndpointGroupDefinitionImpl endpointGroup = new EndpointGroupDefinitionImpl();
                endpointGroup.setRawEndpointGroupDefinition((Map<String, Object>) definition);
                endpointGroups.add(endpointGroup);
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        }

        return endpointGroups;
    }

    /**
     * @param jsonInputStream raw description in json
     * @param extendedJsonInputStream raw definition of meta data extension in json
     * @param type input or output?
     * @return list of {@link EndpointDefinition}s
     * @throws IOException if something went wrong
     */
    public static Set<EndpointDefinitionImpl> extractStaticEndpointDefinition(InputStream jsonInputStream,
        List<InputStream> extendedJsonInputStream, EndpointType type) throws IOException {
        return extractEndpointDefinition(jsonInputStream, extendedJsonInputStream, type, true);
    }

    /**
     * @param jsonInputStream raw definition in json
     * @param extendedJsonInputStream raw definition of meta data extension in json
     * @param direction input or output?
     * @return list of {@link EndpointDefinition}s
     * @throws IOException if something went wrong
     */
    public static Set<EndpointDefinitionImpl> extractDynamicEndpointDefinition(InputStream jsonInputStream,
        List<InputStream> extendedJsonInputStream, EndpointType direction) throws IOException {
        return extractEndpointDefinition(jsonInputStream, extendedJsonInputStream, direction, false);
    }

    private static Set<EndpointDefinitionImpl> extractEndpointDefinition(InputStream jsonInputStream,
        List<InputStream> extendedJsonInputStream, EndpointType type, boolean isStatic) throws IOException {

        Set<EndpointDefinitionImpl> enpointDefinitions = new HashSet<EndpointDefinitionImpl>();
        String key;
        if (isStatic) {
            if (type == EndpointType.INPUT) {
                key = EndpointDefinitionConstants.JSON_KEY_STATIC_INPUTS;
            } else {
                key = EndpointDefinitionConstants.JSON_KEY_STATIC_OUTPUTS;
            }
        } else {
            if (type == EndpointType.INPUT) {
                key = EndpointDefinitionConstants.JSON_KEY_DYNAMIC_INPUTS;
            } else {
                key = EndpointDefinitionConstants.JSON_KEY_DYNAMIC_OUTPUTS;
            }
        }
        for (Object definition : extractDefinitionAsList(jsonInputStream, key)) {
            try {
                EndpointDefinitionImpl endpointDef = new EndpointDefinitionImpl();
                endpointDef.setRawEndpointDefinition((Map<String, Object>) definition);
                endpointDef.setEndpointType(type);
                enpointDefinitions.add(endpointDef);
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        }

        if (isStatic) {
            if (type == EndpointType.INPUT) {
                key = EndpointDefinitionConstants.JSON_KEY_STATIC_INPUTS_META_DATA;
            } else {
                key = EndpointDefinitionConstants.JSON_KEY_STATIC_OUTPUTS_META_DATA;
            }
        } else {
            if (type == EndpointType.INPUT) {
                key = EndpointDefinitionConstants.JSON_KEY_DYNAMIC_INPUTS_META_DATA;
            } else {
                key = EndpointDefinitionConstants.JSON_KEY_DYNAMIC_OUTPUTS_META_DATA;
            }
        }
        for (InputStream inputStream : extendedJsonInputStream) {
            for (Object definition : extractDefinitionAsList(inputStream, key)) {
                try {
                    if (isStatic) {
                        String endpointName = (String) ((Map<String, Object>) definition).get(EndpointDefinitionConstants.KEY_NAME);
                        for (EndpointDefinitionImpl endpointDefinition : enpointDefinitions) {
                            if (endpointDefinition.getName().equals(endpointName)) {
                                endpointDefinition.setRawEndpointDefinitionExtension((Map<String, Object>) definition);
                            }
                        }
                    } else {
                        String endpointIdentifier =
                            (String) ((Map<String, Object>) definition).get(EndpointDefinitionConstants.KEY_IDENTIFIER);
                        for (EndpointDefinitionImpl endpointDefinition : enpointDefinitions) {
                            if (endpointDefinition.getIdentifier().equals(endpointIdentifier)) {
                                endpointDefinition.setRawEndpointDefinitionExtension((Map<String, Object>) definition);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new IOException(e);
                }
            }
        }

        return enpointDefinitions;
    }

    /**
     * @param configurationJson raw configuration description in json
     * @param placeholdersJson raw placeholders description in json
     * @param activationFilterJson raw activation filter description in json
     * @return {@link ConfigurationDefinitionImpl}
     * @throws IOException if something went wrong
     */
    public static ConfigurationDefinitionImpl extractConfigurationDescription(InputStream configurationJson,
        InputStream placeholdersJson, InputStream activationFilterJson) throws IOException {

        ConfigurationDefinitionImpl configDef = new ConfigurationDefinitionImpl();
        configDef.setRawConfigurationDefinition(extractDefinitionAsList(configurationJson,
            ConfigurationDefinitionConstants.JSON_KEY_CONFIGURATION));
        configDef.setRawPlaceholderMetaDataDefinition(extractDefinitionAsList(placeholdersJson,
            ConfigurationDefinitionConstants.JSON_KEY_PLACEHOLDERS));
        configDef.setRawActivationFilters(extractDefinitionAsMap(activationFilterJson,
            ConfigurationDefinitionConstants.JSON_KEY_ACTIVATION_FILTER));
        return configDef;
    }

    /**
     * @param configurationJson raw configuration description in json
     * @param placeholdersJson raw placeholders description in json
     * @param activationFilterJson raw activation filter description in json
     * @return {@link ConfigurationExtensionDefinitionImpl}
     * @throws IOException if something went wrong
     */
    public static ConfigurationExtensionDefinitionImpl extractConfigurationExtensionDescription(
        InputStream configurationJson, InputStream placeholdersJson, InputStream activationFilterJson) throws IOException {

        ConfigurationExtensionDefinitionImpl extConfigDef = new ConfigurationExtensionDefinitionImpl();
        extConfigDef.setRawConfigurationDefinition(extractDefinitionAsList(configurationJson,
            ConfigurationDefinitionConstants.JSON_KEY_CONFIGURATION));
        extConfigDef.setRawPlaceholderMetaDataDefinition(extractDefinitionAsList(placeholdersJson,
            ConfigurationDefinitionConstants.JSON_KEY_PLACEHOLDERS));
        extConfigDef.setActivationFilter(extractDefinitionAsMap(activationFilterJson,
            ConfigurationDefinitionConstants.JSON_KEY_ACTIVATION_FILTER));
        return extConfigDef;
    }

    /**
     * Creates log message for an exception and its cause if it exists.
     * 
     * @param t throwable to consider
     * @return formatted log message considering given throwable
     */
    public static String createErrorLogMessage(Throwable t) {
        return addCauseToExceptionErrorLogMessage(t, "");
    }

    /**
     * Creates log message for an exception and its cause if it exists.
     * 
     * @param errorMessage error message to consider
     * @param errorId unique id that serves a reference to the full-stack trace in the log file - it must be generated by the
     *        {@link LogUtils} class
     * @return formatted log message considering given error message and error id
     */
    public static String createErrorLogMessage(String errorMessage, String errorId) {
        return StringUtils.format("%s (%s)", errorMessage, errorId);
    }

    /**
     * Creates log message for an exception and its cause if it exists.
     * 
     * @param t throwable to consider
     * @param errorId unique id that serves a reference to the full-stack trace in the log file - it must be generated by the
     *        {@link LogUtils} class
     * @return formatted log message considering given throwable and error id
     */
    public static String createErrorLogMessage(Throwable t, String errorId) {
        return StringUtils.format("%s (%s)", addCauseToExceptionErrorLogMessage(t, ""), errorId);
    }

    private static String addCauseToExceptionErrorLogMessage(Throwable cause, String errorMessage) {
        if (!errorMessage.isEmpty()) {
            errorMessage = errorMessage + "; cause: ";
        }
        if (cause.getMessage() == null || cause.getMessage().isEmpty()
            || (cause.getCause() != null && cause.getMessage().startsWith(cause.getCause().getClass().getCanonicalName()))) {
            errorMessage = errorMessage + "Unexpected error: " + cause.getClass().getSimpleName();
        } else {
            errorMessage = errorMessage + cause.getMessage();
        }
        if (cause.getCause() != null) {
            errorMessage = addCauseToExceptionErrorLogMessage(cause.getCause(), errorMessage);
        }
        return errorMessage;
    }

    /**
     * @param jsonInputStream raw description in json
     * @param key of relevant json node
     * @return list of {@link Object}s
     * @throws IOException if something went wrong
     */
    private static List<Object> extractDefinitionAsList(InputStream jsonInputStream, String key) throws IOException {
        Object extractedObj = extractDefinitionAsObject(jsonInputStream, key);
        if (extractedObj == null) {
            return new ArrayList<Object>();
        } else {
            return (List<Object>) extractedObj;
        }
    }

    /**
     * @param jsonInputStream raw description in json
     * @param key of relevant json node
     * @return list of {@link Object}s
     * @throws IOException if something went wrong
     */
    private static Map<String, Object> extractDefinitionAsMap(InputStream jsonInputStream, String key) throws IOException {
        Object extractedObj = extractDefinitionAsObject(jsonInputStream, key);
        if (extractedObj == null) {
            return new HashMap<String, Object>();
        } else {
            return (Map<String, Object>) extractedObj;
        }
    }

    /**
     * @param jsonInputStream raw description in json
     * @param key of relevant json node
     * @return list of {@link Object}s
     * @throws IOException if something went wrong
     */
    private static Object extractDefinitionAsObject(InputStream jsonInputStream, String key) throws IOException {

        String errorMessage = "parsing JSON file failed";

        Map<String, Object> descriptions;
        try {
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            descriptions = JsonUtils.getDefaultObjectMapper().readValue(jsonInputStream, typeRef);
        } catch (JsonParseException e) {
            throw new IOException(errorMessage, e);
        } catch (JsonMappingException e) {
            throw new IOException(errorMessage, e);
        }
        jsonInputStream.close();

        return descriptions.get(key);
    }

    /**
     * Reads the URL for the icon, correcting it if it does not fit.
     *
     * @param bundleName name of the bundle to look
     * @param iconName name of the icon
     * @return correct url, or null, if icon can't be found
     */
    public static URL readIconURL(String bundleName, String iconName) {
        URL url = null;
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle != null) {
            Enumeration<URL> result = Platform.getBundle(bundleName).findEntries("/", iconName, true);
            if (result != null) {
                if (result.hasMoreElements()) {
                    url = result.nextElement();
                } else {
                    url = null;
                }
            }
        } else {
            LOGGER.warn("Bundle for loading icons could not be found: " + bundleName);
        }
        return url;
    }

}
