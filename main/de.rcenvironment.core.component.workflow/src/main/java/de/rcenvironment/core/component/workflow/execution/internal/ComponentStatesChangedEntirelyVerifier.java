/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Default implementation of {@link ComponentStatesChangedEntirelyVerifier}.
 * 
 * @author Doreen Seider
 * 
 * Note: See note in {@link ComponentStatesChangedEntirelyListener}. --seid_do
 */
public class ComponentStatesChangedEntirelyVerifier {

    private Set<ComponentStatesChangedEntirelyListener> compStatesEntirelyChangedListeners = new HashSet<>();

    private final ComponentStatesChangedEntirelyNotifier preparedComponentStateNotifier;

    private final ComponentStatesChangedEntirelyNotifier pausedComponentStateNotifier;

    private final ComponentStatesChangedEntirelyNotifier resumedComponentStateNotifier;

    private final ComponentStatesChangedEntirelyNotifier finishedComponentStateNotifier;

    private final ComponentStatesChangedEntirelyNotifier finalComponentStateNotifier;
    
    private final ComponentStatesChangedEntirelyNotifier disposedComponentStateNotifier;

    private final Set<String> lostComponents;
    
    private final ComponentStatesChangedEntirelyNotifier lastConsoleRowNotifier;

    protected ComponentStatesChangedEntirelyVerifier(int componentCount) {
        preparedComponentStateNotifier = new PreparedComponentStateNotifier(componentCount);
        pausedComponentStateNotifier = new PausedComponentStateNotifier(componentCount);
        resumedComponentStateNotifier = new ResumedComponentStateNotifier(componentCount);
        finishedComponentStateNotifier = new FinishedComponentStateNotifier(componentCount);
        finalComponentStateNotifier = new FinalComponentStateNotifier(componentCount);
        disposedComponentStateNotifier = new DisposedComponentStateNotifier(componentCount);
        lostComponents = new HashSet<>();
        lastConsoleRowNotifier = new LastConsoleRowNotifier(componentCount);
    }
    
    /**
     * Adds a {@link ComponentStatesChangedEntirelyListener}.
     * 
     * @param listener {@link ComponentStatesChangedEntirelyListener} to notify
     */
    public void addListener(ComponentStatesChangedEntirelyListener listener) {
        compStatesEntirelyChangedListeners.add(listener);
    }

    /**
     * Announces a {@link ComponentState} that should be considered by the underlying notifiers that are responsible to callback the
     * appropriate {@link ComponentStatesChangedEntirelyListener}.
     * 
     * @param compExecutionId execution identifier of component affected
     * @param compState {@link ComponentState} to consider
     */
    public void announceComponentState(String compExecutionId, ComponentState compState) {
        switch (compState) {
        case PREPARED:
            preparedComponentStateNotifier.addComponentInDesiredState(compExecutionId);
            break;
        case PAUSED:
            if (pausedComponentStateNotifier.isEnabled()) {
                pausedComponentStateNotifier.addComponentInDesiredState(compExecutionId);
            }
            break;
        case FINISHED:
        case FINISHED_WITHOUT_EXECUTION:
            finishedComponentStateNotifier.addComponentInDesiredState(compExecutionId);
        case CANCELED:
        case FAILED:
        case RESULTS_REJECTED:
            finalComponentStateNotifier.addComponentInDesiredState(compExecutionId);
            if (pausedComponentStateNotifier.isEnabled()) {
                pausedComponentStateNotifier.addComponentInDesiredState(compExecutionId);
            }
            break;
        case DISPOSED:
            disposedComponentStateNotifier.addComponentInDesiredState(compExecutionId);
            break;
        default:
            break;
        }

        if (resumedComponentStateNotifier.isEnabled() && !ComponentState.RESUMING.equals(compState)) {
            resumedComponentStateNotifier.addComponentInDesiredState(compExecutionId);
        }
    }

    /**
     * Announces a final {@link ComponentState} that should be considered by the underlying notifiers that are responsible to callback the
     * appropriate {@link ComponentStatesChangedEntirelyListener}.
     * 
     * @param compExecutionId execution identifier of component affected
     */
    public void accounceComponentInAnyFinalState(String compExecutionId) {
        finalComponentStateNotifier.addComponentInDesiredState(compExecutionId);
    }

    /**
     * Announces the last {@link ConsoleRow} of a component. That event is considered by the underlying notifiers that are responsible to
     * callback the appropriate {@link ComponentStatesChangedEntirelyListener}.
     * 
     * @param compExecutionId execution identifier of component affected
     */
    public void announceLastConsoleRow(String compExecutionId) {
        lastConsoleRowNotifier.addComponentInDesiredState(compExecutionId);
    }
    
    /**
     * Announces lost component(s).
     * 
     * @param compExecutionIds execution identifiers of lost components
     */
    public void announceLostComponents(Set<String> compExecutionIds) {
        for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
            compStatesEntirelyChangedListener.onComponentsLost(compExecutionIds);
        }
        lostComponents.addAll(compExecutionIds);
    }
    
    /**
     * Declares components as being in final component state and disposed that are currently known as lost.
     */
    public void declareLostComponentsAsBeingInFinalStateAndDisposed() {
        finalComponentStateNotifier.addComponentsInDesiredState(lostComponents);
        disposedComponentStateNotifier.addComponentsInDesiredState(lostComponents);
        lastConsoleRowNotifier.addComponentsInDesiredState(lostComponents);
    }

    /**
     * @param compExecutionId execution identifier of component affected
     * @return <code>true</code> if component is or was in a final component state
     */
    public boolean isComponentInFinalState(String compExecutionId) {
        return finalComponentStateNotifier.isComponentInDesiredState(compExecutionId);
    }

    /**
     * @param compExecutionId execution identifier of component affected
     * @return <code>true</code> if component is {@link ComponentState#DISPOSED}
     */
    public boolean isComponentDisposed(String compExecutionId) {
        return disposedComponentStateNotifier.isComponentInDesiredState(compExecutionId);
    }
    
    public Set<String> getLostComponents() {
        return Collections.unmodifiableSet(lostComponents);
    }
    
    public Set<String> getComponentsInFinalState() {
        return finalComponentStateNotifier.getComponentsInDesiredState();
    }
    
    public Set<String> getDisposedComponents() {
        return disposedComponentStateNotifier.getComponentsInDesiredState();
    }

    /**
     * Enables verification of {@link ComponentState#PAUSED}.
     */
    public void enablePausedComponentStateVerification() {
        pausedComponentStateNotifier.clearComponentsInDesiredState();
        pausedComponentStateNotifier.setEnabled(true);
        pausedComponentStateNotifier.addComponentsInDesiredState(finalComponentStateNotifier.getComponentsInDesiredState());
    }
    
    /**
     * Enables verification of resumed {@link ComponentState}s.
     */
    public void enableResumedComponentStateVerification() {
        resumedComponentStateNotifier.clearComponentsInDesiredState();
        resumedComponentStateNotifier.setEnabled(true);
        resumedComponentStateNotifier.addComponentsInDesiredState(finalComponentStateNotifier.getComponentsInDesiredState());
    }
    
    /**
     * Abstract implementation of specific component state change notifiers. It accepts component execution identifiers and adds them to an
     * underlying set. If the size of the set is equal to the component count of the workflow the
     * {@link ComponentStatesChangedEntirelyNotifier#onComponentStatesChangedEntirely()} is called.
     * 
     * @author Doreen Seider
     */
    private abstract class ComponentStatesChangedEntirelyNotifier {

        private final int compCount;
        
        private boolean enabled = false;

        private Set<String> componentsInDesiredState = Collections.synchronizedSet(new HashSet<String>() {

            private static final long serialVersionUID = 6431615134151724870L;

            @Override
            public boolean add(String e) {
                boolean rv = super.add(e);
                if (size() == compCount) {
                    onComponentStatesChangedEntirely();
                }
                return rv;
            };
        });

        private ComponentStatesChangedEntirelyNotifier(int componentCount, boolean enabled) {
            this.compCount = componentCount;
            this.enabled = enabled;
        }

        private synchronized void clearComponentsInDesiredState() {
            componentsInDesiredState.clear();
        }

        private synchronized void addComponentInDesiredState(String executionIdentifier) {
            if (enabled) {
                componentsInDesiredState.add(executionIdentifier);
            }
        }

        private synchronized void addComponentsInDesiredState(Set<String> executionIdentifiers) {
            if (enabled) {
                componentsInDesiredState.addAll(executionIdentifiers);
            }
        }

        private synchronized Set<String> getComponentsInDesiredState() {
            return Collections.unmodifiableSet(componentsInDesiredState);
        }

        private synchronized boolean isComponentInDesiredState(String executionIdentifier) {
            return componentsInDesiredState.contains(executionIdentifier);
        }
        
        protected synchronized void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        private synchronized boolean isEnabled() {
            return enabled;
        }

        abstract void onComponentStatesChangedEntirely();

    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} for {@link ComponentState#PREPARED}.
     *
     * @author Doreen Seider
     */
    private final class PreparedComponentStateNotifier extends ComponentStatesChangedEntirelyNotifier {

        private PreparedComponentStateNotifier(int componentCount) {
            super(componentCount, true);
        }

        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onComponentStatesChangedCompletelyToPrepared();
            }
        }

    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} for {@link ComponentState#PAUSED}.
     *
     * @author Doreen Seider
     */
    private final class PausedComponentStateNotifier extends ComponentStatesChangedEntirelyNotifier {

        private PausedComponentStateNotifier(int componentCount) {
            super(componentCount, false);
        }

        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onComponentStatesChangedCompletelyToPaused();
            }
            setEnabled(false);
        }

    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} to all {@link ComponentState}s a component can be in after it was
     * paused.
     *
     * @author Doreen Seider
     */
    private final class ResumedComponentStateNotifier extends ComponentStatesChangedEntirelyNotifier {

        private ResumedComponentStateNotifier(int componentCount) {
            super(componentCount, false);
        }
        
        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onComponentStatesChangedCompletelyToResumed();
            }
            setEnabled(false);
        }
        
    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} for {@link ComponentState#FINISHED} and
     * {@link ComponentState#FINISHED_WITHOUT_EXECUTION}.
     *
     * @author Doreen Seider
     */
    private final class FinishedComponentStateNotifier extends ComponentStatesChangedEntirelyNotifier {

        private FinishedComponentStateNotifier(int componentCount) {
            super(componentCount, true);
        }

        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onComponentStatesChangedCompletelyToFinished();
            }
        }

    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} for any final {@link ComponentState} (
     * {@link ComponentState#FINISHED}, {@link ComponentState#FINISHED_WITHOUT_EXECUTION}, {@link ComponentState#CANCELED},
     * {@link ComponentState#FAILED}).
     *
     * @author Doreen Seider
     */
    private final class FinalComponentStateNotifier extends ComponentStatesChangedEntirelyNotifier {

        private FinalComponentStateNotifier(int componentCount) {
            super(componentCount, true);
        }

        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onComponentStatesChangedCompletelyToAnyFinalState();
            }
        }

    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} for {@link ComponentState#DISPOSED}.
     *
     * @author Doreen Seider
     */
    private final class DisposedComponentStateNotifier extends ComponentStatesChangedEntirelyNotifier {

        private DisposedComponentStateNotifier(int componentCount) {
            super(componentCount, true);
        }

        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onComponentStatesChangedCompletelyToDisposed();
            }
        }

    }

    /**
     * Implementation of {@link ComponentStatesChangedEntirelyNotifier} for {@link ComponentState#DISPOSED}.
     *
     * @author Doreen Seider
     */
    private final class LastConsoleRowNotifier extends ComponentStatesChangedEntirelyNotifier {

        private LastConsoleRowNotifier(int componentCount) {
            super(componentCount, true);
        }

        @Override
        protected void onComponentStatesChangedEntirely() {
            for (ComponentStatesChangedEntirelyListener compStatesEntirelyChangedListener : compStatesEntirelyChangedListeners) {
                compStatesEntirelyChangedListener.onLastConsoleRowsReceived();
            }
        }

    }

}
