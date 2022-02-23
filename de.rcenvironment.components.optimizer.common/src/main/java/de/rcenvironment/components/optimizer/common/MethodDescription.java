/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Configuration class for an optimizer method.
 *
 * @author Sascha Zur
 */
public class MethodDescription implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2629717845269373743L;
    private String methodName;
    private String methodCode;
    private String optimizerPackage;
    private String followingMethods;
    private Map<String, Map <String, String>> commonSettings;
    private Map<String, Map <String, String>> specificSettings;
    private Map<String, Map <String, String>> responsesSettings;
    private Map<String, String> configMap;

    @Override
    public String toString(){
        String resultString = "";
        resultString += "MethodName = " + methodName + "\n";
        resultString += "MethodCode = " + methodCode + "\n";
        resultString += "OptPackage = " + optimizerPackage + "\n";
        resultString += "followMethods = " + followingMethods + "\n";
        resultString += "CommonSettings = " + commonSettings + "\n";
        resultString += "SpecificSettings = " + specificSettings + "\n";
        resultString += "ResponsesSettings = " + responsesSettings + "\n";
        resultString += "Configuration = " + configMap + "\n";
        return resultString;
    }
    /**
     * Adds a new value to the methods config name.  
     * @param key 
     * @param value 
     */
    public void setConfigValue(String key, String value){
        if (configMap == null){
            configMap = new HashMap<String, String>();
        }
        configMap.put(key, value);
    }
    /**
     * Returns the configuration value to the given key, or null if there is no value to the key. 
     * @param key 
     * @return value, if there is one, else null.
     */
    public String getConfigValue(String key){
        if (configMap != null){
            return configMap.get(key);
        }
        return null;
    }
    
    public Map<String, String> getConfigMap() {
        return configMap;
    }
    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }
    public String getMethodName() {
        return methodName;
    }
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    public String getMethodCode() {
        return methodCode;
    }
    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }
    public String getOptimizerPackage() {
        return optimizerPackage;
    }
    public void setOptimizerPackage(String optimizerPackage) {
        this.optimizerPackage = optimizerPackage;
    }
    public String getFollowingMethods() {
        return followingMethods;
    }
    public void setFollowingMethods(String followingMethods) {
        this.followingMethods = followingMethods;
    }
    public Map<String, Map<String, String>> getCommonSettings() {
        return commonSettings;
    }
    public void setCommonSettings(Map<String, Map<String, String>> commonSettings) {
        this.commonSettings = commonSettings;
    }
    public Map<String, Map<String, String>> getSpecificSettings() {
        return specificSettings;
    }
    public void setSpecificSettings(
            Map<String, Map<String, String>> specificSettings) {
        this.specificSettings = specificSettings;
    }

    public Map<String, Map <String, String>> getResponsesSettings() {
        return responsesSettings;
    }

    public void setResponsesSettings(Map<String, Map <String, String>> responsesSettings) {
        this.responsesSettings = responsesSettings;
    }
    
    
}
