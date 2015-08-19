/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.validators.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.service.component.ComponentContext;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.start.common.validation.PlatformValidator;

/**
 * Validator for all .json files in the configuration folder.
 * 
 * @author Sascha Zur
 */
public class ConfigurationValidator implements PlatformValidator {

    private ConfigurationService configService;

    /** Reusable JSON mapper object. */
    private ObjectMapper mapper = new ObjectMapper();

    protected void activate(final ComponentContext context) {
        // do nothing
    }

    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

    @Override
    public Collection<PlatformMessage> validatePlatform() {
        final Collection<PlatformMessage> result = new LinkedList<PlatformMessage>();

        // TODO review >= 6.0.0; the current validator doesn't really make sense anymore; rework or delete?
        if (true) {
            return result;
        }

        File configArea = configService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_CONFIGURATION_ROOT);
        for (File f : configArea.listFiles()) {
            if (f.getName().endsWith(".json")) {
                String fileContent;
                try {
                    fileContent = FileUtils.readFileToString(f);
                    mapper.configure(Feature.ALLOW_COMMENTS, true);
                    mapper.readTree(fileContent);
                } catch (IOException e) {
                    result.add(new PlatformMessage(PlatformMessage.Type.WARNING,
                        ValidatorsBundleActivator.bundleSymbolicName,
                        String.format(Messages.couldNotValidateJsonFile, f.getAbsolutePath())));
                }
            }
        }
        return result;
    }
}
