/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Test constants for the {@link Mail} bundle.
 * 
 * @author Tobias Menden
 */
public final class MailTestConstants {

    /**
     * Test smtp server.
     */
    public static final String SMTP_SERVER = "test.server.de";

    /**
     * Test mail address map.
     */
    public static final Map<String, String> MAIL_ADDRESS_MAP = new HashMap<String, String>();

    /**
     * Test user password.
     */
    public static final String USER_PASS = "mail@address.de:user:pass";;

    /**
     * Test flag for SSL usage.
     */
    public static final boolean USE_SSL = true;

    /**
     * Test sender.
     */
    public static final String FROM = "testSender";

    /**
     * Test sender.
     */
    public static final int SSL_PORT = 666;

    /**
     * Test mail subject.
     */
    public static final String SUBJECT = "testSubject";

    /**
     * Test mail body.
     */
    public static final String TEXT = "testText";

    /**
     * Test reply address.
     */
    public static final String REPLY_TO = "testReplyAddress";

    /**
     * Test recipients.
     */
    public static final List<String> TO = new Vector<String>();

    static {
        TO.add("address1@smtp.de");
        TO.add("address2@smtp.de");
        MAIL_ADDRESS_MAP.put("list1", "listRecepient1@smtp.de; listRecepient2@smtp.de");
        MAIL_ADDRESS_MAP.put("list2", "listRecepient1@smtp.de; listRecepient3@smtp.de");
    }

    /**
     * Test recipient.
     */
    public static final String TEST_ADDRESS = "test@smtp.de";

    /**
     * Test recipients.
     */
    public static final String RECEPIENTS_LIST2 = TEST_ADDRESS + "; list2; another@address.de; wrong input; wrongAdress";

    private MailTestConstants() {}
}
