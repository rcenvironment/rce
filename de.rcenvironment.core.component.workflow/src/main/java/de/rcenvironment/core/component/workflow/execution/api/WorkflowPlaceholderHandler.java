/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.configuration.SecureStorageSection;
import de.rcenvironment.core.configuration.SecureStorageService;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * 
 * Class for placeholder management and parsing. Here, all information about the placeholders are stored.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (secure storage adaptation)
 */
public class WorkflowPlaceholderHandler implements Serializable {

    /**
     * Key for the preferences key store.
     */
    public static final String PLACEHOLDER_PREFERENCES_KEY = "placeholder_UUID";

    private static final String WORKFLOW_PLACEHOLDER_PATH = "placeholderHistory" + File.separator
        + "Workflow_Placeholder_";

    private static final String COULD_NOT_LOAD_PASSWORD_FROM_STORAGE = "Could not load password from secure storage!";

    private static final Log LOGGER = LogFactory.getLog(WorkflowPlaceholderHandler.class);

    private static final long serialVersionUID = 1L;

    private static final String PLACEHOLDERCOMPONENT_HISTORYFILE = "placeholderComponentHistory.json";

    private static final String PLACEHOLDERINSTANCE_HISTORYFILE = "placeholderInstanceHistory.json";

    private static final String DOT = ".";

    private static PersistentSettingsService persistentSettingsService;

    private static SecureStorageSection secureStorageSection;

    private static List<String> encryptedPlaceholder;

    private static String placeholderPersistentSettingsUUID = null;

    private Map<String, List<String>> componentTypeHistory;

    private Map<String, List<String>> componentInstanceHistory;

    private Map<String, Map<String, String>> componentInstancePlaceholders;

    private Map<String, Map<String, String>> componentTypePlaceholders;

    private Map<String, String> placeholdersDataType;

    // history of a placeholder is saved in componentID.phname -> list of history way with the last
    // entry being the last used

    private Map<String, List<String>> componentInstancesOfType;

    private WorkflowDescription workflowDescription;

    @Deprecated
    /**
     * Because of OSGi.
     */
    public WorkflowPlaceholderHandler() {}

    /**
     * Creates a {@link WorkflowPlaceholderHandler} from the given {@link WorkflowDescription}.
     * 
     * @param wd : {@link WorkflowDescription} as source
     * @param preferencesUUID : UUID for getting the workspace's placeholder
     * @return new {@link WorkflowPlaceholderHandler}
     */
    public static WorkflowPlaceholderHandler createPlaceholderDescriptionFromWorkflowDescription(WorkflowDescription wd,
        String preferencesUUID) {
        WorkflowPlaceholderHandler weph = new WorkflowPlaceholderHandler();
        placeholderPersistentSettingsUUID = preferencesUUID;

        initializePlaceholders(wd, weph);
        initializeHistory(weph);

        weph.workflowDescription = wd;

        return weph;
    }

    protected static void initializePlaceholders(WorkflowDescription wd, WorkflowPlaceholderHandler weph) {
        weph.setComponentInstancePlaceholders(new HashMap<String, Map<String, String>>());
        weph.setComponentTypePlaceholders(new HashMap<String, Map<String, String>>());
        weph.setComponentInstancesOfType(new HashMap<String, List<String>>());
        weph.setPlaceholdersDataType(new HashMap<String, String>());

        encryptedPlaceholder = new LinkedList<>();
        for (WorkflowNode node : wd.getWorkflowNodes()) {
            ConfigurationDescription configDesc = node.getConfigurationDescription();
            for (String key : configDesc.getConfiguration().keySet()) {
                if (configDesc.isPlaceholderSet(key)) {
                    weph.addPlaceholder(configDesc.getActualConfigurationValue(key), node.getComponentDescription().getIdentifier(),
                        node.getIdentifierAsObject().toString());
                    String placeholder = getNameOfPlaceholder(configDesc.getActualConfigurationValue(key));
                    String dataType = configDesc.getComponentConfigurationDefinition().getPlaceholderMetaDataDefinition()
                        .getDataType(placeholder);
                    if (dataType != null) {
                        weph.placeholdersDataType.put(node.getName() + DOT + placeholder, dataType);
                    }
                } else if (key.contains(PlaceholdersMetaDataConstants.DATA_TYPE)) {
                    String dataType = configDesc.getActualConfigurationValue(key);
                    weph.placeholdersDataType.put(node.getName() + DOT + key.replace(PlaceholdersMetaDataConstants.DATA_TYPE, ""),
                        dataType);
                }
            }
        }
    }

    protected static void initializeHistory(WorkflowPlaceholderHandler weph) {
        weph.setComponentInstanceHistory(new HashMap<String, List<String>>());
        weph.setComponentTypeHistory(new HashMap<String, List<String>>());

        if (!placeholderPersistentSettingsUUID.isEmpty()) {
            String id = WORKFLOW_PLACEHOLDER_PATH + placeholderPersistentSettingsUUID;
            Map<String, List<String>> compHist =
                persistentSettingsService.readMapWithStringList(id + File.separator + PLACEHOLDERINSTANCE_HISTORYFILE);
            if (compHist == null) {
                compHist = persistentSettingsService.readMapWithStringList(PLACEHOLDERINSTANCE_HISTORYFILE);
            }
            weph.setComponentInstanceHistory(compHist); // load

            Map<String, List<String>> compTypeHist =
                persistentSettingsService.readMapWithStringList(id + File.separator + PLACEHOLDERCOMPONENT_HISTORYFILE);
            if (compTypeHist == null) {
                compTypeHist = persistentSettingsService.readMapWithStringList(PLACEHOLDERINSTANCE_HISTORYFILE);
            }
            weph.setComponentTypeHistory(compTypeHist); // load
        }
    }

    /**
     * Restores the passwords in the eclipse secure storage to the current placeholder. This will ask the user for the eclipse storage
     * master passphrase.
     * 
     * @param componentHistory the map with password placeholder
     */
    public static void restorePasswords(Map<String, List<String>> componentHistory) {
        for (final Map.Entry<String, List<String>> entry : componentHistory.entrySet()) {
            final String key = entry.getKey();
            List<String> currentList = entry.getValue();
            if (!currentList.isEmpty() && currentList.get(0).equals(ComponentUtils.PLACEHOLDER_PASSWORD_SYMBOL)) {
                try {
                    String path = key;
                    String value = secureStorageSection.read(path, "");
                    List<String> list = new LinkedList<>();
                    list.add(value);
                    componentHistory.put(key, list);
                } catch (OperationFailureException e) {
                    LOGGER.error(COULD_NOT_LOAD_PASSWORD_FROM_STORAGE, e);
                }
            }
        }
    }

    /**
     * Automatically add the given placeholder to the right map and adds an instance to the componentype if necessary.
     * 
     * @param placeholder : the placeholder
     * @param componentID : the component type id
     * @param componentUUID : the instance ID of the component
     */
    public void addPlaceholder(String placeholder, String componentID, String componentUUID) {
        Matcher matcher = ComponentUtils.getMatcherForPlaceholder(placeholder);
        if (matcher.matches()) {
            // Add component instance to list for component group
            List<String> componentInstanceList = componentInstancesOfType.get(componentID);
            if (componentInstanceList != null) {
                if (!componentInstanceList.contains(componentUUID)) {
                    componentInstanceList.add(componentUUID);
                }
            } else {
                componentInstanceList = new LinkedList<>();
                componentInstanceList.add(componentUUID);
                componentInstancesOfType.put(componentID, componentInstanceList);
            }
            // Add placeholder key to right map.
            if (isGlobalPlaceholder(matcher)) {
                addPlaceholderKeyToMap(componentTypePlaceholders, componentID, matcher.group(ComponentUtils.PLACEHOLDERNAME));
            } else {
                addPlaceholderKeyToMap(componentInstancePlaceholders, componentUUID, matcher.group(ComponentUtils.PLACEHOLDERNAME));
            }
            if ((matcher.group(ComponentUtils.ATTRIBUTE1) != null
                && matcher.group(ComponentUtils.ATTRIBUTE1).equals(ComponentUtils.ENCODEDATTRIBUTE))
                || (matcher.group(ComponentUtils.ATTRIBUTE2) != null
                    && matcher.group(ComponentUtils.ATTRIBUTE2).equals(ComponentUtils.ENCODEDATTRIBUTE))) {
                encryptedPlaceholder.add(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME));
            }
        }

    }

    private boolean isGlobalPlaceholder(Matcher matcherOfPlaceholder) {
        return (matcherOfPlaceholder.group(ComponentUtils.ATTRIBUTE1) != null && (matcherOfPlaceholder.group(ComponentUtils.ATTRIBUTE1)
            .equals(ComponentUtils.GLOBALATTRIBUTE)
            || (matcherOfPlaceholder
                .group(ComponentUtils.ATTRIBUTE2) != null && matcherOfPlaceholder
                    .group(ComponentUtils.ATTRIBUTE2).equals(ComponentUtils.GLOBALATTRIBUTE))));
    }

    private void addPlaceholderKeyToMap(Map<String, Map<String, String>> map, String key, String placeholderName) {
        Map<String, String> placeholderMap = map.computeIfAbsent(key, absentKey -> new HashMap<>());
        if (!placeholderMap.containsKey(placeholderName)) {
            placeholderMap.put(placeholderName, null);
        }
    }

    /**
     * Adds a value to the given placeholder in the right map.
     * 
     * @param placeholder : the placeholder with the new value.
     * @param componentID : the component type id
     * @param componentUUID : if placeholder not global, UUID is necessary
     * @param value : the new value
     * @param wfID : ID
     * @param addToHistory : True, if it should be added to history
     */
    public void setPlaceholderValue(String placeholder, String componentID, String componentUUID, String value, String wfID,
        boolean addToHistory) {
        if (placeholder.matches(ComponentUtils.PLACEHOLDER_REGEX)) {
            Matcher matcher = ComponentUtils.getMatcherForPlaceholder(placeholder);
            if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME),
                encryptedPlaceholder)) {
                value = new String(new Base64().encode(value.getBytes(StandardCharsets.UTF_8)));
            }
            if (isGlobalPlaceholder(matcher)) {
                if (componentTypePlaceholders.get(componentID) != null) {
                    componentTypePlaceholders.get(componentID).put(matcher.group(ComponentUtils.PLACEHOLDERNAME), value);
                }
            } else {
                if (componentInstancePlaceholders.get(componentUUID) != null) {
                    componentInstancePlaceholders.get(componentUUID).put(matcher.group(ComponentUtils.PLACEHOLDERNAME), value);
                }
            }

            String tail = DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME);
            if (addToHistory) {
                // do save -> add to history

                // placeholder component history
                String placeholderCompHistory = wfID + DOT + componentID + tail;
                List<String> placeholderCompHistoryList = null;
                if (componentTypeHistory != null && componentTypeHistory.get(placeholderCompHistory) != null) {
                    placeholderCompHistoryList = componentTypeHistory.get(placeholderCompHistory);
                    if (placeholderCompHistoryList.contains(value)) {
                        placeholderCompHistoryList.remove(value);
                    }
                } else {
                    placeholderCompHistoryList = new LinkedList<>();
                }
                if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME),
                    encryptedPlaceholder)) {
                    storePassword(value, componentID, matcher.group(ComponentUtils.PLACEHOLDERNAME), placeholderCompHistoryList);
                } else {
                    placeholderCompHistoryList.add(value);
                }
                if (componentTypeHistory != null) {
                    componentTypeHistory.put(placeholderCompHistory, placeholderCompHistoryList);
                }

                // placeholder instance history
                String placeholderInstanceHistory = componentUUID + tail;
                List<String> placeholderHistoryList = null;
                if (componentInstanceHistory != null && componentInstanceHistory.get(placeholderInstanceHistory) != null) {
                    placeholderHistoryList = componentInstanceHistory.get(placeholderInstanceHistory);
                    if (placeholderHistoryList.contains(value)) {
                        placeholderHistoryList.remove(value);
                    }
                } else {
                    placeholderHistoryList = new LinkedList<>();
                }
                if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME),
                    encryptedPlaceholder)) {
                    storePassword(value, componentUUID, matcher.group(ComponentUtils.PLACEHOLDERNAME), placeholderHistoryList);
                } else {
                    placeholderHistoryList.add(value);
                }
                if (componentInstanceHistory != null) {
                    componentInstanceHistory.put(placeholderInstanceHistory, placeholderHistoryList);
                }
            } else {
                // don't save === remove from history

                // placeholder component history
                String placeholderCompHistory = wfID + DOT + componentID + tail;
                List<String> placeholderCompHistoryList = null;
                if (componentTypeHistory != null && componentTypeHistory.get(placeholderCompHistory) != null) {
                    placeholderCompHistoryList = componentTypeHistory.get(placeholderCompHistory);
                    if (placeholderCompHistoryList.contains(value)) {
                        placeholderCompHistoryList.remove(value);
                    }
                } else {
                    placeholderCompHistoryList = new LinkedList<>();
                }
                if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME),
                    encryptedPlaceholder)) {
                    clearPassword(componentID, matcher.group(ComponentUtils.PLACEHOLDERNAME), placeholderCompHistoryList);
                }

                // placeholder instance history
                String placeholderInstanceHistory = componentUUID + tail;
                List<String> placeholderHistoryList = null;
                if (componentInstanceHistory != null && componentInstanceHistory.get(placeholderInstanceHistory) != null) {
                    placeholderHistoryList = componentInstanceHistory.get(placeholderInstanceHistory);
                    if (placeholderHistoryList.contains(value)) {
                        placeholderHistoryList.remove(value);
                    }
                } else {
                    placeholderHistoryList = new LinkedList<>();
                }
                if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME),
                    encryptedPlaceholder)) {
                    clearPassword(componentUUID, matcher.group(ComponentUtils.PLACEHOLDERNAME), placeholderHistoryList);
                }
            }
        }
    }

    private void clearPassword(String componentID, String placeholderName, List<String> placeholderList) {
        placeholderList.clear();
        try {
            String path = componentID + DOT + placeholderName;
            secureStorageSection.delete(path);
        } catch (OperationFailureException e) {
            LOGGER.warn("Could not remove password", e);
        }
    }

    private void storePassword(Serializable value, String componentID, String placeholderName, List<String> placeholderList) {
        placeholderList.clear();
        try {
            String path = componentID + DOT + placeholderName;
            secureStorageSection.store(path, value.toString());
            placeholderList.add(ComponentUtils.PLACEHOLDER_PASSWORD_SYMBOL);
        } catch (OperationFailureException e) {
            LOGGER.warn("Could not store password", e);
        }
    }

    /**
     * Adds a value to the given placeholder in the right map. Use only if you are sure the placeholder is global.
     * 
     * @param placeholder : the placeholder with the new value.
     * @param componentID : the component type id
     * @param value : the new value
     * @param wfID : ID
     * @param addToHistory : True, if it should be added to history
     */
    public void setGlobalPlaceholderValue(String placeholder, String componentID, String value, String wfID, boolean addToHistory) {
        if (placeholder.matches(ComponentUtils.PLACEHOLDER_REGEX)) {
            Matcher matcher = ComponentUtils.getMatcherForPlaceholder(placeholder);
            if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME),
                encryptedPlaceholder)) {
                value = new String(new Base64().encode(value.getBytes(StandardCharsets.UTF_8)));
            }
            if (isGlobalPlaceholder(matcher)) {
                if (componentTypePlaceholders.get(componentID) != null) {
                    componentTypePlaceholders.get(componentID).put(matcher.group(ComponentUtils.PLACEHOLDERNAME), value);
                }
            }
            if (addToHistory) {
                String placeholderCompHistory = wfID + DOT + componentID + DOT + matcher.group(ComponentUtils.PLACEHOLDERNAME);
                List<String> placeholderCompHistoryList = null;
                if (componentTypeHistory != null && componentTypeHistory.get(placeholderCompHistory) != null) {
                    placeholderCompHistoryList = componentTypeHistory.get(placeholderCompHistory);
                    if (placeholderCompHistoryList.contains(value)) {
                        placeholderCompHistoryList.remove(value);
                    }
                } else {
                    placeholderCompHistoryList = new LinkedList<>();
                }
                placeholderCompHistoryList.add(value);
                if (componentTypeHistory != null) {
                    componentTypeHistory.put(placeholderCompHistory, placeholderCompHistoryList);
                }
            }

        }
    }

    /**
     * 
     * Returns the value for the given placeholder. If it is not found, the value is null.
     * 
     * @param placeholderName : the placeholder to look for.
     * @param componentUUID : The instance id from the component if it is not a global placeholder.
     * @return the value of the placeholder, if found, else null
     */
    public Serializable getValueByPlaceholderName(String placeholderName, String componentUUID) {
        Serializable result = null;
        if (componentInstancePlaceholders.containsKey(componentUUID)) {
            result = componentInstancePlaceholders.get(componentUUID).get(placeholderName);
        }
        return result;
    }

    /**
     * 
     * Returns the value for the given placeholder. If it is not found, the value is null. Use this method if you are sure the placeholder
     * is global.
     * 
     * 
     * @param placeholderName : the placeholder to look for.
     * @param componentId : id of the component type to look for.
     * @return the value of the placeholder, if found, else null
     */
    public Serializable getGlobalValueByPlaceholderName(String placeholderName, String componentId) {
        Serializable result = null;
        if (componentTypePlaceholders.get(componentId) != null) {
            result = componentTypePlaceholders.get(componentId).get(placeholderName);
        }
        return result;
    }

    /**
     * 
     * Returns the value for the given placeholder. If it is not found, the value is null.
     * 
     * @param placeholder : the placeholder to look for.
     * @param componentID : the component type id
     * @param componentUUID : The instance id from the component if it is not a global placeholder.
     * @return the value of the placeholder, if found, else null
     */
    public Serializable getValueByPlaceholder(String placeholder, String componentID, String componentUUID) {
        Serializable result = null;

        if (placeholder.matches(ComponentUtils.PLACEHOLDER_REGEX)) {
            Matcher matcher = ComponentUtils.getMatcherForPlaceholder(placeholder);
            if (isGlobalPlaceholder(matcher)) {
                if (componentTypePlaceholders.get(componentID) != null) {
                    result = componentTypePlaceholders.get(componentID).get(matcher.group(ComponentUtils.PLACEHOLDERNAME));
                }
            } else {
                if (componentInstancePlaceholders.get(componentUUID) != null) {
                    result = componentInstancePlaceholders.get(componentUUID).get(matcher.group(ComponentUtils.PLACEHOLDERNAME));
                }
            }
        }
        return result;
    }

    /**
     * 
     * Returns the value for the given placeholder. If it is not found, the value is null. Use this method if you are sure the placeholder
     * is global.
     * 
     * 
     * @param placeholder : the placeholder to look for.
     * @param componentID : the component type id
     * @return the value of the placeholder, if found, else null
     */
    public Serializable getValueByPlaceholder(String placeholder, String componentID) {
        if (placeholder.matches(ComponentUtils.PLACEHOLDER_REGEX)) {
            Matcher matcher = ComponentUtils.getMatcherForPlaceholder(placeholder);
            if (isGlobalPlaceholder(matcher) && componentTypePlaceholders.get(componentID) != null) {
                return componentTypePlaceholders.get(componentID).get(matcher.group(ComponentUtils.PLACEHOLDERNAME));
            }
        }
        return null;
    }

    /**
     * Returns all instances with placeholder of one component type.
     * 
     * @param componentID : type of the component
     * @return a list of all registered component instances, null if there are none
     */
    public List<String> getComponentInstances(String componentID) {
        if (componentInstancesOfType != null) {
            return componentInstancesOfType.get(componentID);
        }
        return null;
    }

    /**
     * Returns a keyset with all placeholdernames of one registered instance.
     * 
     * @param componentUUID : the instance id
     * @return set of placeholders
     */
    public Set<String> getPlaceholderNameSetOfComponentInstance(String componentUUID) {
        if (componentInstancePlaceholders != null) {
            return componentInstancePlaceholders.get(componentUUID).keySet();
        }
        return null;
    }

    /**
     * Getter.
     * 
     * @param componentID :
     * @return :
     */
    public Map<String, String> getPlaceholdersOfComponentType(String componentID) {
        return componentTypePlaceholders.get(componentID);
    }

    /**
     * Getter.
     * 
     * @param componentUUID :
     * @return :
     */
    public Map<String, String> getPlaceholdersOfComponentInstance(String componentUUID) {
        return componentInstancePlaceholders.get(componentUUID);
    }

    /**
     * Returns a keyset with all global placeholdernames of one component type.
     * 
     * @param componentID : the compoent id
     * @return set of placeholders
     */
    public Set<String> getGlobalPlaceholdersForComponentID(String componentID) {
        if (componentTypePlaceholders.get(componentID) != null) {
            return componentTypePlaceholders.get(componentID).keySet();
        }
        return null;
    }

    /**
     * Returns a set with all placeholder of one registered instance.
     * 
     * @param componentUUID : the instance id
     * @return set of placeholders
     */
    public Set<String> getPlaceholderNamesOfComponentInstance(String componentUUID) {
        Set<String> result = new HashSet<>();
        if (componentInstancePlaceholders != null) {
            String compKey = "";
            for (Map.Entry<String, List<String>> entry : componentInstancesOfType.entrySet()) {
                if (entry.getValue().contains(componentUUID)) {
                    compKey = entry.getKey();
                }
            }
            for (String placeholderName : componentInstancePlaceholders.get(componentUUID).keySet()) {
                final String closeBracket = "}";
                if (ComponentUtils.isEncryptedPlaceholder(compKey + DOT + placeholderName, encryptedPlaceholder)) {
                    result.add("${*." + placeholderName + closeBracket);
                } else {
                    result.add("${" + placeholderName + closeBracket);
                }
            }
        }
        return result;
    }

    /**
     * Returns a set with all placeholder of one component type.
     * 
     * @param componentID : the component type id
     * @return set of placeholders
     */
    public Set<String> getPlaceholderOfComponent(String componentID) {
        Set<String> result = new HashSet<>();
        if (componentTypePlaceholders != null && componentTypePlaceholders.get(componentID) != null) {
            for (String placeholderName : componentTypePlaceholders.get(componentID).keySet()) {
                final String closeBracket = "}";
                if (ComponentUtils.isEncryptedPlaceholder(componentID + DOT + placeholderName, encryptedPlaceholder)) {
                    result.add("${global.*." + placeholderName + closeBracket);
                } else {
                    result.add("${global." + placeholderName + closeBracket);

                }
            }
        }
        return result;
    }

    /**
     * Returns all types of components with placeholders.
     * 
     * @return Set of component IDs
     */
    public Set<String> getIdentifiersOfPlaceholderContainingComponents() {
        if (componentInstancesOfType != null) {
            return componentInstancesOfType.keySet();
        }
        return null;
    }

    /**
     * Returns the regex name value.
     * 
     * @param fullPlaceholder : to check
     * @return the name
     */
    public static String getNameOfPlaceholder(String fullPlaceholder) {
        return ComponentUtils.getMatcherForPlaceholder(fullPlaceholder).group(ComponentUtils.PLACEHOLDERNAME);
    }

    protected void bindPersistentSettingsService(PersistentSettingsService newPersistentSettingsService) {
        persistentSettingsService = newPersistentSettingsService;
    }

    protected void bindSecureStorageService(SecureStorageService secureStorageService) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // skip implicit bind actions if is was spawned as part of a default test environment;
            // note that this may cause side effects if tests explicitly rely on this behavior
            return;
        }
        try {
            secureStorageSection = secureStorageService.getSecureStorageSection(ComponentUtils.PLACEHOLDER_PASSWORD_STORAGE_NODE);
        } catch (IOException e) {
            LOGGER.error("Failed to initialize the secure storage for placeholder data: " + e.toString());
            // TODO specify further handling
        }
    }

    public static List<String> getEncryptedPlaceholder() {
        return encryptedPlaceholder;
    }

    /**
     * Loads the history for the given placeholder.
     * 
     * @param placeholder : to look for
     * @param componentUUID : the UUID
     * @return list of history
     */
    public String[] getInstancePlaceholderHistory(String placeholder, String componentUUID) {
        String[] result = new String[0];
        if (componentInstanceHistory.containsKey(componentUUID + DOT + placeholder)) {
            List<String> history = componentInstanceHistory.get(componentUUID + DOT + placeholder);
            result = history.toArray(result);
        }

        return result;
    }

    /**
     * Loads the history for the given placeholder.
     * 
     * @param placeholder : to look for
     * @param componentID : the ID
     * @param wfID : ID of the current workflow
     * @return list of history
     */
    public String[] getComponentPlaceholderHistory(String placeholder, String componentID, String wfID) {
        String[] result = new String[0];
        if (componentTypeHistory.containsKey(wfID + DOT + componentID + DOT + placeholder)) {
            List<String> history = componentTypeHistory.get(wfID + DOT + componentID + DOT + placeholder);
            result = history.toArray(result);
        }
        return result;
    }

    /**
     * Saves the new placeholders history.
     */
    public void saveHistory() {
        if (!placeholderPersistentSettingsUUID.isEmpty()) {
            String id = WORKFLOW_PLACEHOLDER_PATH + placeholderPersistentSettingsUUID;
            persistentSettingsService.saveMapWithStringList(componentTypeHistory, id + File.separator + PLACEHOLDERCOMPONENT_HISTORYFILE);
            persistentSettingsService
                .saveMapWithStringList(componentInstanceHistory, id + File.separator + PLACEHOLDERINSTANCE_HISTORYFILE);
        }
    }

    /**
     * Deletes the history of the given placeholder.
     * 
     * @param componentDescriptionIdentifier : the component identifier of the placeholder
     * @param placeholderName : name of the placeholder to delete history from
     */
    public void deletePlaceholderHistory(String componentDescriptionIdentifier, String placeholderName) {
        Set<String> setToDelete = new HashSet<>();
        for (String key : componentInstanceHistory.keySet()) {
            if (key != null && placeholderName != null && key.endsWith(placeholderName)) {
                setToDelete.add(key);
            }
        }
        for (String key : setToDelete) {
            if (!componentInstanceHistory.get(key).isEmpty()
                && !componentInstanceHistory.get(key).get(0).equals(ComponentUtils.PLACEHOLDER_PASSWORD_SYMBOL)) {
                try {
                    secureStorageSection.delete(key);
                } catch (OperationFailureException e) {
                    LOGGER.warn("Could not remove from storage", e);
                }
            }
            componentInstanceHistory.remove(key);
        }
        setToDelete = new HashSet<>();
        for (String key : componentTypeHistory.keySet()) {
            if (key.endsWith(componentDescriptionIdentifier + DOT + placeholderName)) {
                setToDelete.add(key);
            }
        }
        for (String key : setToDelete) {
            if (!componentTypeHistory.get(key).isEmpty()
                && !componentTypeHistory.get(key).get(0).equals(ComponentUtils.PLACEHOLDER_PASSWORD_SYMBOL)) {
                try {
                    secureStorageSection.delete(key);
                } catch (OperationFailureException e) {
                    LOGGER.warn("Could not remove from storage", e);
                }
            }
            componentTypeHistory.remove(key);
        }
        saveHistory();
    }

    /**
     * Returns a list with all proposals the given placeholder has in its history.
     * 
     * @param placeholderName : the placeholder to look for.
     * @return proposals of this placeholder
     */
    public String[] getOtherPlaceholderHistoryValues(String placeholderName) {
        List<String> proposals = new LinkedList<>();
        for (Map.Entry<String, List<String>> entry : componentTypeHistory.entrySet()) {
            if (entry.getKey().endsWith(placeholderName)) {
                for (String newValue : entry.getValue()) {
                    if (!proposals.contains(newValue)) {
                        proposals.add(newValue);
                    }
                }
            }
        }
        return proposals.toArray(new String[proposals.size()]);
    }

    /**
     * Searches for another value for the given placeholder in same workflow file.
     * 
     * @param placeholderName : placeholder to look for
     * @param identifier : workflow identifier
     * @return a value, if exists, else null
     */
    public String getValueFromOtherComponentInWorkflow(String placeholderName, String identifier) {
        String resultValue = null;
        for (String key : componentTypeHistory.keySet()) {
            if (key.endsWith(placeholderName) && key.contains(identifier)) {
                List<String> allValues = componentTypeHistory.get(key);
                resultValue = allValues.get(allValues.size() - 1);
            }
        }
        return resultValue;
    }

    /**
     * Deletes the history for ALL placeholder in the secure storage.
     * 
     */
    public void deleteAllPasswordHistories() {
        try {
            for (String key : secureStorageSection.listKeys()) {
                secureStorageSection.delete(key);
            }
        } catch (OperationFailureException e) {
            LOGGER.error(COULD_NOT_LOAD_PASSWORD_FROM_STORAGE, e);
        }
    }

    protected static void setPersistentSettingsService(PersistentSettingsService incPersistentSettingsService) {
        persistentSettingsService = incPersistentSettingsService;
    }

    public Map<String, List<String>> getComponentTypeHistory() {
        return componentTypeHistory;
    }

    public void setComponentTypeHistory(Map<String, List<String>> componentTypeHistory) {
        this.componentTypeHistory = componentTypeHistory;
    }

    public Map<String, List<String>> getComponentInstanceHistory() {
        return componentInstanceHistory;
    }

    public void setComponentInstanceHistory(Map<String, List<String>> componentInstanceHistory) {
        this.componentInstanceHistory = componentInstanceHistory;
    }

    public Map<String, Map<String, String>> getComponentInstancePlaceholders() {
        return componentInstancePlaceholders;
    }

    public void setComponentInstancePlaceholders(Map<String, Map<String, String>> componentInstancePlaceholders) {
        this.componentInstancePlaceholders = componentInstancePlaceholders;
    }

    public Map<String, Map<String, String>> getComponentTypePlaceholders() {
        return componentTypePlaceholders;
    }

    public void setComponentTypePlaceholders(Map<String, Map<String, String>> componentTypePlaceholders) {
        this.componentTypePlaceholders = componentTypePlaceholders;
    }

    public Map<String, List<String>> getComponentInstancesOfType() {
        return componentInstancesOfType;
    }

    public void setComponentInstancesOfType(Map<String, List<String>> componentInstancesOfType) {
        this.componentInstancesOfType = componentInstancesOfType;
    }

    public static void setPlaceholderPersistentSettingsUUID(String placeholderPersistentSettingsUUID) {
        WorkflowPlaceholderHandler.placeholderPersistentSettingsUUID = placeholderPersistentSettingsUUID;
    }

    public static String getPlaceholderPersistentSettingsUUID() {
        return WorkflowPlaceholderHandler.placeholderPersistentSettingsUUID;
    }

    public void setPlaceholdersDataType(Map<String, String> componentDataType) {
        this.placeholdersDataType = componentDataType;
    }

    public Map<String, String> getPlaceholdersDataType() {
        return placeholdersDataType;
    }

    /**
     * @return True if there are any active placeholders, i.e., placeholders that have to be filled by the user prior to execution
     */
    public boolean hasActivePlaceholders() {
        Set<String> componentTypesWithPlaceholder = this.getIdentifiersOfPlaceholderContainingComponents();

        for (String componentID : componentTypesWithPlaceholder) {
            List<WorkflowNodeIdentifier> instancesWithPlaceholder = this.getComponentInstances(componentID).stream()
                .map(WorkflowNodeIdentifier::new)
                .collect(Collectors.toList());

            for (WorkflowNodeIdentifier componentInstanceID : instancesWithPlaceholder) {
                final ConfigurationDescription configDesc =
                    workflowDescription.getWorkflowNode(componentInstanceID).getComponentDescription().getConfigurationDescription();

                final Collection<String> instancePlaceholders =
                    this.getPlaceholderNameSetOfComponentInstance(componentInstanceID.toString());

                final boolean containsActivePlaceholder = instancePlaceholders.stream()
                    .anyMatch(instancePlaceholder -> isActivePlaceholder(instancePlaceholder, configDesc));

                if (containsActivePlaceholder) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the placeholder is active, i.e. it has to be filled before running the workflow.
     * 
     * @param instancePlaceholder the placeholder to check
     * @param configDesc configuration description of the component.
     * @return true, iff the placeholder is active.
     */
    public static boolean isActivePlaceholder(String instancePlaceholder,
        ConfigurationDescription configDesc) {
        Set<String> configKeys = configDesc.getComponentConfigurationDefinition().getConfigurationKeys();
        Set<String> activeConfigKeys = configDesc.getActiveConfigurationDefinition().getConfigurationKeys();
        boolean isActivePlaceholder = false;
        for (String configKey : configDesc.getConfiguration().keySet()) {
            if ((activeConfigKeys.contains(configKey) || !configKeys.contains(configKey))
                && configDesc.isPlaceholderSet(configKey)) {
                String configValue = configDesc.getActualConfigurationValue(configKey);
                if (configValue.contains(instancePlaceholder)) {
                    isActivePlaceholder = true;
                    break;
                }
            }
        }
        return isActivePlaceholder;
    }

}
