/*
 * Copyright 2002-2016 University of Oslo, Norway
 *
 * This file is part of Cerebrum.
 *
 * Cerebrum is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Cerebrum is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerebrum; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

/*
 * BofdConnection.java
 *
 * Created on November 19, 2002, 11:48 AM
 */

package no.uio.jbofh;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.X509TrustManager;


/**
 * Specialized TrustManager called by the SSLSocket framework when
 * validating server certificate.
 */
class InternalTrustManager implements X509TrustManager {
    static X509Certificate serverCert = null;

    InternalTrustManager(String cafile) throws IOException, CertificateException {
        readServerCert(cafile);
    }

    private void readServerCert(String cafile) throws IOException,
            CertificateException {
        X509Certificate cert;
        CertificateFactory cf;
        cf = CertificateFactory.getInstance("X.509");
        if (cafile == null) {
            try (   
                InputStream inStream = 
                ResourceLocator.getResource(this, "/cacert.pem").openStream()) {
                cert = (X509Certificate)cf.generateCertificate(inStream);
            }
        } else { 
        try (    
            FileInputStream file = new FileInputStream(cafile)) {
            cert = (X509Certificate)cf.generateCertificate(file);
            }
            
        }
        serverCert = cert;
    }

    public void checkClientTrusted( X509Certificate[] cert, String str) {
        // Not implemented (not called by framework for this client)
    }
    
    public void checkServerTrusted( X509Certificate[] cert, String str) 
        throws CertificateException {
        Date date = new Date();
        if(cert == null || cert.length == 0)
            throw new IllegalArgumentException("null or zero-length certificate chain");
        if(str == null || str.length() == 0)
            throw new IllegalArgumentException("null or zero-length authentication type");
        for(int i = 0; i < cert.length; i++) {
            X509Certificate parent;
            if(i + 1 >= cert.length) {
                parent = cert[i];
            } else {
                parent = cert[i+1];
            }

            if(! parent.getSubjectDN().equals(cert[i].getIssuerDN())) {
                throw new CertificateException("Incorrect issuer for server cert");
            }
            cert[i].checkValidity(date);
            parent.checkValidity(date);
            try {
                if (cert[i] != parent)
                    cert[i].verify(parent.getPublicKey());
                } catch (CertificateException | NoSuchAlgorithmException |
                            InvalidKeyException | NoSuchProviderException |
                            SignatureException e) {
                   throw new CertificateException("Bad server certificate: "+e);
            }
            if(cert[i].getIssuerDN().equals(serverCert.getSubjectDN())) {
                // Issuer is trusted
                try {
                    cert[i].verify(serverCert.getPublicKey());
                    serverCert.checkValidity(date);
                    } catch (CertificateException | NoSuchAlgorithmException |
                            InvalidKeyException | NoSuchProviderException |
                            SignatureException e) {
                    System.out.println("bas");
                    throw new CertificateException("Bad server certificate: "+e);
                }
                return;
            }
        }
        throw new CertificateException("Bad server certificate: No CA match.");
    }
    
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] ret = new X509Certificate[1];
        ret[0] = serverCert;
        return ret;
    }
}

// arch-tag: e689905d-cdab-4978-9ea4-28e1647b512e
