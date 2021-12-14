/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;

/**
 * common superclass for test step definitions, providing common interfaces, subclasses, methods and teststeps for instance management.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke (based on previous code of)
 */
public abstract class InstanceManagementStepDefinitionBase extends AbstractStepDefinitionBase {

    // TODO randomize and check for collisions before starting instance
    protected static final AtomicInteger PORT_NUMBER_GENERATOR = new AtomicInteger(52100);

    protected static final InstanceManagementService INSTANCE_MANAGEMENT_SERVICE = ExternalServiceHolder.getInstanceManagementService();

    public InstanceManagementStepDefinitionBase(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Interface passing method to be performed on instances.
     * 
     * @author Marlon Schroeter
     */
    protected interface InstanceAction {

        void performActionOnInstance(ManagedInstance instance, long timeout) throws IOException;
    }

    /**
     * Interface passing method to be performed on a subset of all instances.
     * 
     * @author Marlon Schroeter
     */
    protected interface InstanceIterator {

        void iterateActionOverInstance(ManagedInstance instance) throws Exception;
    }

    /**
     * Enum listing all possible execution modes, when performing actions on instances.
     */
    protected enum InstanceActionExecutionType {
        ORDERED,
        CONCURRENT,
        RANDOM
    }

    /**
     * @param isMainAction true if executing this command is the actual test action; false if this it is incidental, e.g. for testing state
     *        or performing cleanup
     */
    protected final String executeCommandOnInstance(final ManagedInstance instance, String commandString, boolean isMainAction) {
        final String instanceId = instance.getId();
        final String startInfoText = StringUtils.format("Executing command \"%s\" on instance \"%s\"", commandString, instanceId);
        if (isMainAction) {
            printToCommandConsole(startInfoText);
        }
        log.debug(startInfoText);
        CapturingTextOutReceiver commandOutputReceiver = new CapturingTextOutReceiver();
        try {
            final int maxAttempts = 3;
            int numAttempts = 0;
            while (numAttempts < maxAttempts) {
                try {
                    INSTANCE_MANAGEMENT_SERVICE.executeCommandOnInstance(instanceId, commandString, commandOutputReceiver);
                    break; // exit retry loop on success
                } catch (JSchException e) {
                    if (!e.toString().contains("Connection refused: connect")) {
                        throw e; // rethrow and fail on other errors
                    }
                }
                numAttempts++;
            }
            if (numAttempts > 1) {
                String retrySuffix = " after retrying the SSH connection for " + (numAttempts - 1) + " times)";
                printToCommandConsole(
                    StringUtils.format("  (Executed command \"%s\" on instance \"%s\"%s", commandString, instanceId, retrySuffix));
            }
            String commandOutput = commandOutputReceiver.getBufferedOutput();
            instance.setLastCommandOutput(commandOutput);
            log.debug(StringUtils.format("Finished execution of command \"%s\" on instance \"%s\"", commandString, instanceId));
            return commandOutput;
        } catch (JSchException | SshParameterException | IOException | InterruptedException e) {
            fail(StringUtils.format("Failed to execute command \"%s\" on instance \"%s\": %s", commandString, instanceId, e.toString()));
            return null; // dummy command; never reached
        }

    }

    protected void iterateInstances(InstanceIterator instanceIterator, String allFlag, String instanceIds) throws Exception {
        for (final ManagedInstance instance : resolveInstanceList(allFlag != null, instanceIds)) {
            instanceIterator.iterateActionOverInstance(instance);
        }
    }

    /**
     * Performs an action on the provided instances concurrently, sequentially or in a given order.
     * 
     * @param action object of type InstanceAction containing the method to be performed on the instances.
     * @param instances List of instances on which the same action is to be performed.
     * @param executionMode indicating in which kind of execution the actions are to be performed.
     */
    protected void performActionOnInstances(InstanceAction action, List<ManagedInstance> instances,
        InstanceActionExecutionType executionMode) {
        switch (executionMode) {
        case CONCURRENT:
            final RunnablesGroup runnablesGroup = ConcurrencyUtils.getFactory().createRunnablesGroup();
            for (final ManagedInstance instance : instances) {
                runnablesGroup.add(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // TODO does not work in parallel execution context. Make accessible to multiple threads at the same time.
                            action.performActionOnInstance(instance, StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS * instances.size());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                // TODO this does not seem to execute action.performActionOnInstance in parallel
                executeRunnablesGroupAndHandlePotentialErrors(runnablesGroup, "performing action on instance");
            }
            break;
        case ORDERED:
            for (ManagedInstance instance : instances) {
                try {
                    action.performActionOnInstance(instance, StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS * instances.size());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        case RANDOM:
            Collections.shuffle(instances);
            for (ManagedInstance instance : instances) {
                try {
                    action.performActionOnInstance(instance, StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS * instances.size());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        default:
            fail("unknown execution mode");
        }
    }

    protected InstanceActionExecutionType resolveExecutionMode(String executionDesc) throws IllegalArgumentException {
        if (executionDesc == null) {
            throw new IllegalArgumentException(
                "executionDesc was set to null, which is not supported. Provide a fallback execution mode to circumvent this.");
        }

        InstanceActionExecutionType executionMode;
        switch (executionDesc) {
        case ("in the given order"):
            executionMode = InstanceActionExecutionType.ORDERED;
            break;
        case ("concurrently"):
            executionMode = InstanceActionExecutionType.CONCURRENT;
            break;
        case ("in a random order"):
            executionMode = InstanceActionExecutionType.RANDOM;
            break;
        default:
            throw new IllegalArgumentException(
                "executionDesc was set to an unsupported value. Provide a fallback execution mode to circumvent this.");
        }
        return executionMode;
    }

    protected InstanceActionExecutionType resolveExecutionMode(String executionDesc, InstanceActionExecutionType fallback) {
        try {
            return resolveExecutionMode(executionDesc);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    protected final ManagedInstance resolveInstance(String instanceId) {
        return executionContext.getInstanceFromId(instanceId);
    }

    /**
     * Returns a list of ManagedInstances depending on the parameter inputs. All | List 0 0 : returns all enabled instances 0 1 : returns
     * given list of instances 1 0 : returns all enabled instances 1 1 : fails
     * 
     * @param allFlag value regarding if all instances should be effected
     * @param instanceIds list of instances that should be effected
     * 
     * @return List of ManagedInstances
     */
    protected List<ManagedInstance> resolveInstanceList(boolean allFlag, String instanceIds) {
        List<ManagedInstance> instances;
        if (instanceIds == null) {
            instances = new ArrayList<ManagedInstance>(executionContext.getEnabledInstances());
        } else if (!allFlag) {
            instances = new ArrayList<ManagedInstance>();
            for (String instanceId : parseCommaSeparatedList(instanceIds)) {
                instances.add(resolveInstance(instanceId));
            }
        } else {
            // allFlag being set and a list of instance being provided is ambiguous and therefore not supported
            fail("calling this operation for all instances and providing a list is not supported. Choose either one.");
            return null; // never reached
        }
        return instances;
    }

    private void executeRunnablesGroupAndHandlePotentialErrors(final RunnablesGroup runnablesGroup, String singleTaskDescription) {
        final List<RuntimeException> exceptions = runnablesGroup.executeParallel();
        boolean hasFailure = false;
        for (RuntimeException e : exceptions) {
            if (e != null) {
                log.warn("Exception while asynchronously " + singleTaskDescription, e);
                hasFailure = true;
            }
        }
        if (hasFailure) {
            // rethrow an arbitrary one
            for (RuntimeException e : exceptions) {
                if (e != null) {
                    throw e;
                }
            }
        }
    }
}
