/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

/**
 * Announcement of workflow events of different type (see {@link WorkflowEventType}).
 *
 * @author Doreen Seider
 */
public class ComponentEventAnnouncement {

    private String subject;

    private String body;

    private WorkflowEventType type;

    /**
     * Supported workflow event types that can be announced.
     * 
     * @author Doreen Seider
     */
    public enum WorkflowEventType {

        /**
         * A component in the workflow requests a manual output verification before sending them further.
         */
        REQUEST_FOR_OUTPUT_APPROVAL("Request for tool result approval");
        
        private String displayName;
        
        WorkflowEventType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * @return subject of this {@link ComponentEventAnnouncement} or <code>null</code> if there is none
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @return <code>true</code> if this {@link ComponentEventAnnouncement} provides a subject otherwise <code>false</code>
     */
    public boolean hasSubject() {
        return subject != null;
    }

    /**
     * @return body of this {@link ComponentEventAnnouncement}
     */
    public String getBody() {
        return body;
    }

    public WorkflowEventType getWorkflowEventType() {
        return type;
    }

    /**
     * Create new {@link ComponentEventAnnouncement} instances.
     * 
     * @param type {@link WorkflowEventType} of the {@link ComponentEventAnnouncement}
     * @param subject subject related to the {@link ComponentEventAnnouncement}, may be <code>null</code>
     * @param body subject related to the {@link ComponentEventAnnouncement}
     * @return {@link ComponentEventAnnouncement} instance newly created
     */
    public static ComponentEventAnnouncement createAnnouncement(WorkflowEventType type, String subject, String body) {
        ComponentEventAnnouncement announcement = new ComponentEventAnnouncement();
        announcement.type = type;
        announcement.subject = subject;
        announcement.body = body;
        return announcement;
    }

}
