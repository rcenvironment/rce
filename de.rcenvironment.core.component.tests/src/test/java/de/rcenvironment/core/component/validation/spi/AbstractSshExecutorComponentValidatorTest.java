/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreator;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;

/**
 * 
 * Test for the AbstractSshExecutorComponentValidator.
 *
 * @author Jascha Riedel
 */
public class AbstractSshExecutorComponentValidatorTest {

    private ComponentDescriptionMockCreator componentDescriptionMockCreater;

    private AbstractSshExecutorComponentValidator abstractSshExecutorComponentValidator;

    private List<ComponentValidationMessage> messages;

    /** Basic Setup. */
    @Before
    public void setUp() {
        componentDescriptionMockCreater = new ComponentDescriptionMockCreator();

        abstractSshExecutorComponentValidator = new AbstractSshExecutorComponentValidator() {

            @Override
            public String getIdentifier() {
                return null;
            }

            @Override
            protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(ComponentDescription componentDescription) {
                return null;
            }

            @Override
            protected List<ComponentValidationMessage> validateSshComponentSpecific(ComponentDescription componentDescription) {
                return null;
            }
        };
    }

    /** Test CONFIG_KEY_HOST not set. */
    @Test
    public void testConfigKeyHostNotSet() {
        setAllNecessaryValues();
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_HOST, null);
        validate();

        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());

    }

    /** Test CONFIG_KEY_PORT not set. */
    @Test
    public void testConfigKeyPortNotSet() {
        setAllNecessaryValues();
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_PORT, null);
        validate();

        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());

    }

    /** Test CONFIG_KEY_SANDBOXROOT not set. */
    @Test
    public void testConfigKeySandBoxRootNotSet() {
        setAllNecessaryValues();
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SANDBOXROOT, null);
        validate();

        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
        assertTrue(messages.get(0).getAbsoluteMessage().contains(SshExecutorConstants.CONFIG_KEY_SANDBOXROOT));

    }

    /** Test CONFIG_KEY_SCRIPT. */
    @Test
    public void testConfigKeyScriptNotSet() {
        setAllNecessaryValues();
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, null);
        validate();

        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());

    }

    private void setAllNecessaryValues() {
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_HOST, "a");
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_PORT, "24");
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SANDBOXROOT, "b");
        componentDescriptionMockCreater.addConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, "asfsf");
    }

    private void validate() {
        messages = abstractSshExecutorComponentValidator
            .validateComponentSpecific(componentDescriptionMockCreater.createComponentDescriptionMock());
    }
}
