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

import java.security.KeyStore;
import java.security.PrivateKey;
import lombok.extern.slf4j.Slf4j;
import net.ljcomputing.htmltopdf.model.SignatureCredentials;
import net.ljcomputing.htmltopdf.service.KeyStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeyStoreServiceImpl implements KeyStoreService {
    @Value("${app.keystoreAlias}")
    private String keystoreAlias;

    @Value("${app.password}")
    private String password;

    @Autowired private KeyStore keystore;

    @Override
    public SignatureCredentials retrieveCredentials() {
        SignatureCredentials signatureCredentials = new SignatureCredentials();

        try {
            signatureCredentials.setPrivateKey(
                    (PrivateKey) keystore.getKey(keystoreAlias, password.toCharArray()));
            signatureCredentials.setCertificateChain(keystore.getCertificateChain(keystoreAlias));
        } catch (Exception e) {
            log.error("Error retrieving signature credentials: ", e);
        }

        return signatureCredentials;
    }
}
