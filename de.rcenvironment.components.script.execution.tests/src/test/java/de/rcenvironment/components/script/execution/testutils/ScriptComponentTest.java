/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.testutils;

import java.io.IOException;

import org.junit.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactoryRegistry;
import de.rcenvironment.components.script.execution.ScriptComponent;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.testutils.ScriptingServiceStubFactory;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * Integration test for {@link ScriptComponent}.
 * 
 * @author Sascha Zur
 */
public class ScriptComponentTest {

    private static final String PRINT_TEST = "print test";

    private static final String FALSE = "false";

    private static final String TRUE = "true";

    /**
     * Expected exception if script/validation fails.
     */
    @Rule
    public ExpectedException scriptException = ExpectedException.none();

    private ComponentTestWrapper component;

    private ComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    private ScriptComponent componentInstance;

    /**
     * Common setup.
     * 
     * @throws IOException e
     */
    @Before
    public void setUp() throws IOException {
        context = new ComponentContextMock();
        componentInstance = new ScriptComponent();
        component = new ComponentTestWrapper(componentInstance, context);
        TempFileServiceAccess.setupUnitTestEnvironment();
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();

        ComponentDataManagementService componentDataManagementServiceMock = EasyMock.createMock(ComponentDataManagementService.class);

        context.addService(ScriptingService.class, ScriptingServiceStubFactory.createDefaultInstance());
        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);

    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();
    }

    /**
     * Tests behavior of script component lifecycle.
     * 
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testScriptComponentLifecycle() throws ComponentException {
        ScriptExecutorFactoryRegistry scriptExecutorService = EasyMock.createMock(ScriptExecutorFactoryRegistry.class);
        ScriptExecutor executor = EasyMock.createMock(ScriptExecutor.class);
        EasyMock.expect(scriptExecutorService.requestScriptExecutor(ScriptLanguage.Jython)).andReturn(executor);
        EasyMock.replay(scriptExecutorService);
        context.addService(ScriptExecutorFactoryRegistry.class, scriptExecutorService);

        EasyMock.expect(executor.prepareExecutor(context)).andReturn(true);
        try {
            executor.prepareNewRun(ScriptLanguage.Jython, PRINT_TEST, null);
        } catch (ComponentException e) {
            Assert.fail();
        }
        executor.runScript();
        EasyMock.expect(executor.postRun()).andReturn(true);
        executor.deleteTempFiles();
        executor.reset();
        executor.deleteTempFiles();
        executor.tearDown();
        EasyMock.replay(executor);

        context.addSimulatedInput("", "", DataType.Float, false, null);
        context.setInputValue("", typedDatumFactory.createFloat(3.0));
        context.setConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, PRINT_TEST);
        context.setConfigurationValue(ScriptComponentConstants.SCRIPT_LANGUAGE, ScriptLanguage.Jython.name());

        component.start();
        component.processInputs();
        component.reset();
    }

    /**
     * Tests behavior of script component history data item.
     * 
     * @throws ComponentException on unexpected component failures.
     * @throws IOException if data management fails
     */
    @Test
    public void testScriptComponentHistoryDataItemTrue() throws ComponentException, IOException {
        testScriptComponentHistoryDataItem(TRUE);
    }

    /**
     * Tests behavior of script component history data item.
     * 
     * @throws ComponentException on unexpected component failures.
     * @throws IOException if data management fails
     */
    @Test
    public void testScriptComponentHistoryDataItemFalse() throws ComponentException, IOException {
        testScriptComponentHistoryDataItem(FALSE);
    }

    private void testScriptComponentHistoryDataItem(String dataItemActive) throws ComponentException, IOException {
        ScriptExecutorFactoryRegistry scriptExecutorService = EasyMock.createMock(ScriptExecutorFactoryRegistry.class);
        ScriptExecutor executor = EasyMock.createNiceMock(ScriptExecutor.class);
        EasyMock.expect(scriptExecutorService.requestScriptExecutor(EasyMock.anyObject(ScriptLanguage.class))).andReturn(executor)
            .anyTimes();
        EasyMock.replay(scriptExecutorService);

        EasyMock.expect(executor.prepareExecutor(EasyMock.anyObject(ComponentContext.class))).andReturn(true).anyTimes();

        EasyMock.expect(executor.postRun()).andReturn(true).anyTimes();
        EasyMock.replay(executor);
        context.addService(ScriptExecutorFactoryRegistry.class, scriptExecutorService);
        context.addSimulatedInput("", "", DataType.Float, false, null);
        context.setInputValue("", typedDatumFactory.createFloat(3.0));
        context.setConfigurationValue(ScriptComponentConstants.SCRIPT_LANGUAGE, ScriptLanguage.Jython.name());
        context.setConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM, dataItemActive);
        context.setConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, PRINT_TEST);
        if (dataItemActive.equals(TRUE)) {
            ComponentDataManagementService componentDataManagementServiceMock = context.getService(ComponentDataManagementService.class);
            EasyMock.expect(componentDataManagementServiceMock.createTaggedReferenceFromString(context, PRINT_TEST)).andReturn("A");
            EasyMock.replay(componentDataManagementServiceMock);
        }
        component.start();
        component.processInputs();

        if (dataItemActive.equals(TRUE)) {
            Assert.assertNotNull(context.getHistoryDataItem());
        } else {
            Assert.assertNull(context.getHistoryDataItem());
        }
    }
}
