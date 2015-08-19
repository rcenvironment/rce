/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

import java.security.cert.X509Certificate;



/**
 * X509Certificate based extension of {@link User}.
 * (needs to be exported because it needs to be visible for communication bundle in order
 * to be able to deserialize objects of this class)
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * 
 * @see java.security.cert.X509Certificate
 */
public class CertificateUser extends User {

    private static final long serialVersionUID = 6949816833219681401L;
    
    private static final Type TYPE = Type.certificate;

    private final String userId;
    
    private final String domain;
    
    public CertificateUser(X509Certificate certificate, int validityInDays) {
        super(validityInDays);
        userId = certificate.getSubjectX500Principal().toString().replace(", ", ",");
        domain = certificate.getIssuerX500Principal().toString().replace(", ", ",");
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public Type getType() {
        return TYPE;
    }

}
