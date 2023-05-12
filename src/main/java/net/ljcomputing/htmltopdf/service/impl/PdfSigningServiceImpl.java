/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

James G Willmore - LJ Computing - (C) 2023
*/
package net.ljcomputing.htmltopdf.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.ljcomputing.htmltopdf.service.PdfSigningService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PdfSigningServiceImpl implements PdfSigningService {
    @Value("${app.keystoreAlias}")
    private String keystoreAlias;

    @Value("${app.password}")
    private String password;

    @Autowired private KeyStore keystore;
    private SigningCredentials credentials;

    @Override
    public PDDocument signPdf(Path pdf) {
        getSigningCredentials();
        return null;
    }

    private void getSigningCredentials() {
        SigningCredentials signingCredentials = new SigningCredentials();
        // grabs the first alias from the keystore and get the private key. An
        // alternative method or constructor could be used for setting a specific
        // alias that should be used.
        Enumeration<String> aliases = null;
        try {
            aliases = keystore.aliases();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String alias;
        Certificate cert = null;
        while (cert == null && aliases != null && aliases.hasMoreElements()) {
            alias = aliases.nextElement();
            try {
                signingCredentials.setPrivateKey(
                        (PrivateKey) keystore.getKey(keystoreAlias, password.toCharArray()));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Certificate[] certChain = null;
            try {
                certChain = keystore.getCertificateChain(alias);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (certChain != null) {
                signingCredentials.setCertificateChain(certChain);
                cert = certChain[0];
                if (cert instanceof X509Certificate) {
                    // avoid expired certificate
                    try {
                        ((X509Certificate) cert).checkValidity();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    try {
                        checkCertificateUsage((X509Certificate) cert);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        if (cert == null) {
            try {
                throw new IOException("Could not find certificate");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        credentials = signingCredentials;
    }

    private void checkCertificateUsage(X509Certificate x509Certificate)
            throws CertificateParsingException {
        // Check whether signer certificate is "valid for usage"
        // https://stackoverflow.com/a/52765021/535646
        // https://www.adobe.com/devnet-docs/acrobatetk/tools/DigSig/changes.html#id1
        boolean[] keyUsage = x509Certificate.getKeyUsage();
        if (keyUsage != null && !keyUsage[0] && !keyUsage[1]) {
            // (unclear what "signTransaction" is)
            // https://tools.ietf.org/html/rfc5280#section-4.2.1.3
            log.error(
                    "Certificate key usage does not include "
                            + "digitalSignature nor nonRepudiation");
        }
        List<String> extendedKeyUsage = x509Certificate.getExtendedKeyUsage();
        if (extendedKeyUsage != null
                && !extendedKeyUsage.contains(KeyPurposeId.id_kp_emailProtection.toString())
                && !extendedKeyUsage.contains(KeyPurposeId.id_kp_codeSigning.toString())
                && !extendedKeyUsage.contains(KeyPurposeId.anyExtendedKeyUsage.toString())
                && !extendedKeyUsage.contains("1.2.840.113583.1.1.5")
                &&
                // not mentioned in Adobe document, but tolerated in practice
                !extendedKeyUsage.contains("1.3.6.1.4.1.311.10.3.12")) {
            log.error(
                    "Certificate extended key usage does not include "
                            + "emailProtection, nor codeSigning, nor anyExtendedKeyUsage, "
                            + "nor 'Adobe Authentic Documents Trust'");
        }
    }

    @Data
    private class SigningCredentials {
        private PrivateKey privateKey;
        private Certificate[] certificateChain;

        public boolean isValid() {
            return privateKey != null && certificateChain != null;
        }
    }
}
