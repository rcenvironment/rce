/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import java.util.List;

import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Class representing an e-mail.
 * 
 * @author Tobias Menden
 */
public class Mail {

    private static final String ASSERTION_DEFINED = "The parameter must not be null: ";
    
    private final List<String> recipients;

    private final String subject;

    private final String text;

    private final String replyTo;

    public Mail(List<String> recipients, String subject, String text, String replyTo) throws IllegalArgumentException {
        Assertions.isDefined(recipients, ASSERTION_DEFINED + "recepient");
        Assertions.isDefined(subject, ASSERTION_DEFINED + "subject");
        Assertions.isDefined(text, ASSERTION_DEFINED + "text");
        Assertions.isDefined(replyTo, ASSERTION_DEFINED + "replyTo");
        this.recipients = recipients;
        this.subject = subject;
        this.text = text;
        this.replyTo = replyTo;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public String getReplyTo() {
        return replyTo;
    }

}
