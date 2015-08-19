/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.script.execution;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ScriptPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String SCRIPT = "script";

    private static final String USAGEOFSCRIPT = "usageOfScript";

    private static final String V2_9 = "2.9";

    private static final String V3_1 = "3.1";

    private static final String V3_2 = "3.2";

    private static final String V3_3 = "3.3";
    
    private static final String V3_4 = "3.4";

    private static final String USAGE_OF_SCRIPT = "usage of script";

    private static final String CONFIGURATION = "configuration";

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return ScriptComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;
        if (!silent) {
            if (persistentComponentDescriptionVersion.compareTo(V2_9) >= 0
                && persistentComponentDescriptionVersion.compareTo(V3_1) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
            }
            if (persistentComponentDescriptionVersion.compareTo(V3_1) >= 0
                && persistentComponentDescriptionVersion.compareTo(V3_4) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
            }
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
        PersistentComponentDescription description, boolean silent) throws IOException {

        if (!silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                return secondUpdate(description);
            }
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                if (description.getComponentVersion().compareTo(V3_2) < 0) {
                    description = thirdUpdate(description);
                }
                if (description.getComponentVersion().compareTo(V3_3) < 0) {
                    description = fourthUpdate(description);
                }
                if (description.getComponentVersion().compareTo(V3_4) < 0) {
                    description = updateFromV33ToV34(description);
                }
            }
        }
        return description;
    }
    
    private PersistentComponentDescription updateFromV33ToV34(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        description = PersistentComponentDescriptionUpdaterUtils.updateSchedulingInformation(description);
        description.setComponentVersion(V3_4);
        return description;
    }

    /**
     * Updates the component from version 3.2 to 3.3.
     * */
    private PersistentComponentDescription fourthUpdate(PersistentComponentDescription description) throws JsonParseException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ObjectNode configNode = (ObjectNode) node.get(CONFIGURATION);
        if (configNode.get(USAGEOFSCRIPT) != null) {
            if (configNode.get(USAGEOFSCRIPT).getTextValue().equals("LOCAL")) {
                configNode.put(USAGEOFSCRIPT, TextNode.valueOf("NEW"));
                configNode.put("script", TextNode.valueOf(configNode.get("localScript").getTextValue()));
            }
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V3_3);
        return description;
    }
    
    /**
     * Updates the component from version 3.1 to 3.2.
     **/
    private PersistentComponentDescription thirdUpdate(PersistentComponentDescription description) throws JsonProcessingException,
        IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ObjectNode configNode = (ObjectNode) node.get(CONFIGURATION);
        ArrayNode dynOutputs = (ArrayNode) node.get("dynamicOutputs");
        List<String> outputNameList = new LinkedList<String>();
        if (dynOutputs != null) {
            for (JsonNode output : dynOutputs) {
                outputNameList.add(output.get("name").getTextValue());
            }
        }

        String script = configNode.get(SCRIPT).getTextValue();

        Pattern p = Pattern.compile("(\\S*\\s*= [\"|\']FINISHED[\"|\'])");
        Matcher m = p.matcher(script);

        while (m.find()) {
            String group = m.group(1);
            String name = group.substring(0, group.indexOf("=")).trim();
            if (outputNameList.contains(name)) {
                script =
                    script.substring(0, script.indexOf(group)) + "RCE.close_output(\"" + name + "\")"
                        + script.substring(script.indexOf(group) + group.length());
            }
        }
        configNode.put(SCRIPT, TextNode.valueOf(script));
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V3_2);
        return description;
    }

    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription secondUpdate(PersistentComponentDescription description) throws JsonParseException, IOException {

        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs", "default", description);
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicInputs", "default", description);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ObjectNode configNode = (ObjectNode) node.get(CONFIGURATION);
        if (node.get(CONFIGURATION).get(USAGE_OF_SCRIPT) != null) {
            configNode.put(USAGEOFSCRIPT, node.get(CONFIGURATION).get(USAGE_OF_SCRIPT));
            configNode.remove(USAGE_OF_SCRIPT);
        }
        if (node.get(CONFIGURATION).get(USAGEOFSCRIPT) == null) {
            configNode.put(USAGEOFSCRIPT, TextNode.valueOf("NEW"));
        }

        configNode.put(SCRIPT, TextNode.valueOf(configNode.get(SCRIPT).getTextValue().replaceAll("_dm_.clear\\(\\)", "")));
        configNode.remove("remote path of existing script");
        configNode.remove("debug");
        configNode.remove("remote upload path of new script");
        configNode.remove(USAGE_OF_SCRIPT);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V3_1);
        return description;
    }

}
