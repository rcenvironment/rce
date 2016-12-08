/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores Validation Messages.
 * 
 * @author Jascha Riedel
 *
 */
public final class ComponentValidationMessageStore {

    private static ComponentValidationMessageStore instance = null;

    private Map<String, List<ComponentValidationMessage>> messageMap = new HashMap<>();

    private ComponentValidationMessageStore() {};

    /**
     * Returns the instance of the store.
     * 
     * @return {@link ComponentValidationMessageStore} instance
     */
    public static ComponentValidationMessageStore getInstance() {
        if (instance == null) {
            instance = new ComponentValidationMessageStore();
        }
        return instance;
    }

    /**
     * 
     * @param componentId that one wants the messages of.
     * @return List<{@link ComponentValidationMessage}> linked to the componentId.
     */
    public List<ComponentValidationMessage> getMessagesByComponentId(String componentId) {

        if (!messageMap.containsKey(componentId)) {
            messageMap.put(componentId, new ArrayList<ComponentValidationMessage>());
        }
        return Collections.unmodifiableList(messageMap.get(componentId));
    }

    /**
     * 
     * @param componentId that the messages belong to.
     * @param messageList that should put to the map (Note that this is a replacing operation).
     */
    public void addValidationMessagesByComponentId(String componentId, List<ComponentValidationMessage> messageList) {
        synchronized (messageMap) {
            messageMap.put(componentId, messageList);
        }
    }

    public Map<String, List<ComponentValidationMessage>> getMessageMap() {
        return Collections.unmodifiableMap(messageMap);
    }

    /**
     * @return whether their are any error or warning messages in the store.
     */
    public boolean isErrorAndWarningsFree() {
        boolean isEmpty = true;
        for (String componentId : messageMap.keySet()) {
            isEmpty = messageMap.get(componentId).isEmpty();
            if (!isEmpty) {
                break;
            }
        }
        return isEmpty;
    }

    /**
     * 
     * Deletes all messages stored in the message store.
     *
     */
    public void emptyMessageStore() {
        messageMap.clear();
    }

}
