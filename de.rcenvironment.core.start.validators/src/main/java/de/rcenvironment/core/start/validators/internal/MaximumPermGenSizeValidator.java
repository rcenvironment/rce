/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;

/**
 * Ensures that RCE is started with a PermGen size >= 256 MB.
 * 
 * @author Sascha Zur
 */
// TODO review: is this validator robust enough? especially the unit handling seems to be broken
public class MaximumPermGenSizeValidator extends DefaultInstanceValidator {

    private static final String VALIDATION_DISPLAY_NAME = "Maximum permanent generation heap (MaxPermSize)";

    private static final int DEFAULT_MIN_PERM_SIZE = 256;
    
    private static final String DEFAULT_MIN_PERM_SIZE_UNIT = "m";

    @Override
    public InstanceValidationResult validate() {
        RuntimeMXBean runtimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimemxBean.getInputArguments();

        String maxPermSizeDefined = null;
        for (String str : arguments) {
            if (str.startsWith("-XX:MaxPermSize")) {
                maxPermSizeDefined = str.substring(str.lastIndexOf('=') + 1);
            }
        }
        if (maxPermSizeDefined != null) {
            int maxPermSizeValue = Integer.parseInt(maxPermSizeDefined.substring(0, maxPermSizeDefined.length() - 1));
            String maxPermSizeUnit = String.valueOf(maxPermSizeDefined.charAt(maxPermSizeDefined.length() - 1));
            if (maxPermSizeUnit.toUpperCase().equals(DEFAULT_MIN_PERM_SIZE_UNIT.toUpperCase())) {
                if (maxPermSizeValue < DEFAULT_MIN_PERM_SIZE) {
                    return createInstanceValidationResult(DEFAULT_MIN_PERM_SIZE, DEFAULT_MIN_PERM_SIZE_UNIT);
                }
            }
        }
        long maxPermgen = 0;
        for (MemoryPoolMXBean mx : ManagementFactory.getMemoryPoolMXBeans()) {
            if (mx.getName().endsWith("Perm Gen")) {
                maxPermgen = mx.getUsage().getMax() / (long) Math.pow(2, 2 * 10);
            }
        }
        if (maxPermgen != 0 && maxPermgen < DEFAULT_MIN_PERM_SIZE) {
            return createInstanceValidationResult(DEFAULT_MIN_PERM_SIZE, DEFAULT_MIN_PERM_SIZE_UNIT);
        }
        
        return InstanceValidationResultFactory.createResultForPassed(VALIDATION_DISPLAY_NAME);
    }
    
    private InstanceValidationResult createInstanceValidationResult(int minPermSizeValue, String minPermSizeUnit) {
        String errorMessage = Messages.permGenSizeTooLow + minPermSizeValue + minPermSizeUnit + ".";
        return InstanceValidationResultFactory.createResultForFailureWhichAllowsToProceed(
            VALIDATION_DISPLAY_NAME, errorMessage);
    }
    
}
