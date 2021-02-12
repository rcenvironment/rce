/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Default {@link UserComponentIdMappingService} implementation.
 *
 * @author Robert Mischke
 * @author Brigitte Boden (added parsing for SSH tools)
 */
@Component
public final class UserComponentIdMappingServiceImpl implements UserComponentIdMappingService {

    private static final String SINGLE_QUOTE = "'";

    private static final String DOLLAR = ")$";

    private static final String BUILTIN_COMPONENTS_TYPE_PREFIX = "rce";

    private static final String RA_COMPONENTS_TYPE_PREFIX = "ssh";

    // very lenient filter; unproblematic as non-sensical rules simply do nothing, as they never match
    private static final String VALID_INTEGRATED_TOOL_ID_REGEXP = "[^/]+";

    private static final Pattern BUILTIN_COMPONENTS_EXTERNAL = Pattern.compile("^" + BUILTIN_COMPONENTS_TYPE_PREFIX + "/([\\w ]+)$");

    private static final Pattern RA_COMPONENTS_EXTERNAL = Pattern.compile("^" + RA_COMPONENTS_TYPE_PREFIX + "/([\\w ]+)$");

    // note: this expects the "built-in component" pattern to be checked first, otherwise this will over-match
    private static final Pattern INTEGRATED_TOOLS_EXTERNAL = Pattern.compile("^(\\w+)/(" + VALID_INTEGRATED_TOOL_ID_REGEXP + DOLLAR);

    // note: special nested pattern needed to match "doe.v2", too
    // note: this expects the "remote access component" pattern to be checked first, otherwise this will over-match
    private static final Pattern BUILTIN_COMPONENTS_INTERNAL = Pattern.compile("^de\\.rcenvironment\\.(\\w+(?:\\.\\w+)?)$");

    private static final Pattern RA_COMPONENTS_INTERNAL =
        Pattern.compile("^de\\.rcenvironment\\.remoteaccess\\.(" + VALID_INTEGRATED_TOOL_ID_REGEXP + DOLLAR);

    private static final Pattern INTEGRATED_TOOLS_INTERNAL =
        Pattern.compile("^de\\.rcenvironment\\.integration\\.(\\w+)\\.(" + VALID_INTEGRATED_TOOL_ID_REGEXP + DOLLAR);

    private final Map<String, String> builtinComponentNamesByInternalId = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, String> builtinComponentInternalIdsByName = Collections.synchronizedMap(new HashMap<>());

    @Override
    public String fromExternalToInternalId(String input) throws OperationFailureException {

        Matcher matcher; // intentionally reusing the same variable to prevent accidental spill-overs -- misc_ro

        matcher = RA_COMPONENTS_EXTERNAL.matcher(input);
        if (matcher.matches()) {
            final String name = matcher.group(1); // the name without the static prefix
            return StringUtils.format("de.rcenvironment.remoteaccess.%s", name);
        }

        matcher = BUILTIN_COMPONENTS_EXTERNAL.matcher(input);
        if (matcher.matches()) {
            final String extId = matcher.group(1); // the name without the static prefix
            final String intId = builtinComponentInternalIdsByName.get(extId);
            if (intId == null) {
                throw new OperationFailureException("Found no built-in component matching '" + input + SINGLE_QUOTE);
            }
            return intId; // the map already contains the full id, so no prefixing required
        }

        matcher = INTEGRATED_TOOLS_EXTERNAL.matcher(input);
        if (matcher.matches()) {
            final String type = matcher.group(1);
            final String name = matcher.group(2);
            return StringUtils.format("de.rcenvironment.integration.%s.%s", type, name);
        }

        throw new OperationFailureException("Unrecognized component/tool id format '" + input + SINGLE_QUOTE);
    }

    @Override
    public String fromInternalToExternalId(String input) throws OperationFailureException {

        Matcher matcher; // intentionally reusing the same variable to prevent accidental spill-overs -- misc_ro

        matcher = RA_COMPONENTS_INTERNAL.matcher(input);
        if (matcher.matches()) {
            final String name = matcher.group(1);
            return StringUtils.format(RA_COMPONENTS_TYPE_PREFIX + "/%s", name);
        }

        matcher = BUILTIN_COMPONENTS_INTERNAL.matcher(input);
        if (matcher.matches()) {
            final String name = builtinComponentNamesByInternalId.get(input); // note: full id, not just the regexp part
            if (name == null) {
                throw new OperationFailureException("Found no built-in component matching '" + input + SINGLE_QUOTE);
            }
            return StringUtils.format(BUILTIN_COMPONENTS_TYPE_PREFIX + "/%s", name);
        }

        matcher = INTEGRATED_TOOLS_INTERNAL.matcher(input);
        if (matcher.matches()) {
            final String type = matcher.group(1);
            final String name = matcher.group(2);
            return StringUtils.format("%s/%s", type, name);
        }

        throw new OperationFailureException("Unrecognized internal component/tool id format '" + input + SINGLE_QUOTE);
    }

    @Override
    public void registerBuiltinComponentMapping(String intId, String name) {
        LogFactory.getLog(getClass())
            .debug(StringUtils.format("Registered built-in mapping from component id '%s' to name '%s'", intId, name));
        builtinComponentInternalIdsByName.put(name, intId);
        builtinComponentNamesByInternalId.put(intId, name);
    }

}
