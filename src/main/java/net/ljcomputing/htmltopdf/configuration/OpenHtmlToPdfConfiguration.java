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

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.nio.file.Path;
import org.jsoup.helper.W3CDom;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class OpenHtmlToPdfConfiguration {
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public PdfRendererBuilder pdfRendererBuilder() {
        return new PdfRendererBuilder();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public W3CDom w3cDom() {
        return new W3CDom();
    }

    @Bean
    public Path outputDirectory() {
        String homeDir = System.getProperty("user.home");
        Path outPath = Path.of(homeDir, "out");
        return outPath;
    }
}
