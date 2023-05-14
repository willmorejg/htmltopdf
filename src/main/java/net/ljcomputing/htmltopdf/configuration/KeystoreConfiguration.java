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
package net.ljcomputing.htmltopdf.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class KeystoreConfiguration {
    @Value("${app.password}")
    private String password;

    @Value("${classpath:keystores/keystore.p12}")
    private File keystoreFile;

    @Bean
    public KeyStore keystore() {
        KeyStore keystore = null;

        try (InputStream is = new FileInputStream(keystoreFile)) {
            keystore = KeyStore.getInstance("PKCS12");
            keystore.load(is, password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error loading keystore bean: ", e);
            throw new RuntimeException("keystore is null");
        }

        return keystore;
    }
}
