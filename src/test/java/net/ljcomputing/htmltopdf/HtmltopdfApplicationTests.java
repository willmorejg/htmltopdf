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
package net.ljcomputing.htmltopdf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Calendar;
import net.ljcomputing.htmltopdf.service.Html5ParsingService;
import net.ljcomputing.htmltopdf.service.PdfSigningService;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Document;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class HtmltopdfApplicationTests {
    @Autowired private Path outputDirectory;
    @Autowired private Html5ParsingService html5ParsingService;
    @Autowired private PdfRendererBuilder pdfRendererBuilder;
    @Autowired private PdfSigningService pdfSigningService;

    @Test
    @Order(1)
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    @Order(10)
    @Disabled
    void createPdf() {
        Path testOutFile = outputDirectory.resolve("htmltopdftest.pdf");

        try (OutputStream os = new FileOutputStream(testOutFile.toFile())) {
            Path testFile = outputDirectory.resolve("html").resolve("test.html");
            String url = testFile.toUri().toURL().toExternalForm(); // "https://ljcomputing.net";
            Document doc = html5ParsingService.html5ParseDocument(url, 6000);
            pdfRendererBuilder.withW3cDocument(doc, url);
            pdfRendererBuilder.toStream(os);
            pdfRendererBuilder.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(11)
    // @Disabled
    void signPdf() throws Exception {
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("James G Willmore");
        signature.setLocation("Union, NJ");
        signature.setReason("Test Signature");
        signature.setSignDate(Calendar.getInstance());

        Path testOutFile = outputDirectory.resolve("htmltopdftest.pdf");
        pdfSigningService.signPdf(testOutFile, signature);
    }
}
