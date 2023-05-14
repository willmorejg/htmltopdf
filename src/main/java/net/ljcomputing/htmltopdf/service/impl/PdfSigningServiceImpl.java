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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import net.ljcomputing.htmltopdf.model.SignatureCredentials;
import net.ljcomputing.htmltopdf.service.KeyStoreService;
import net.ljcomputing.htmltopdf.service.PdfSigningService;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PdfSigningServiceImpl implements PdfSigningService, SignatureInterface {
    @Autowired private KeyStoreService keyStoreService;

    @Override
    public void signPdf(Path pdf, PDSignature signature) {
        Path outPath =
                Path.of(
                        "/" + pdf.subpath(0, pdf.getNameCount() - 1).toString(),
                        pdf.getFileName().toString().replace(".pdf", "-signed.pdf"));

        try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
            PDPage signaturePage = new PDPage();
            PDDocument document = PDDocument.load(pdf.toFile(), "");

            PDFont font = PDType1Font.HELVETICA;
            PDResources resources = new PDResources();
            resources.put(COSName.HELV, font);

            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
            acroForm.setDefaultResources(resources);

            String defaultAppearanceString = "/Helv 0 Tf 0 g";
            acroForm.setDefaultAppearance(defaultAppearanceString);

            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.getCOSObject().setItem(COSName.V, signature);
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            PDRectangle rect = new PDRectangle(50, 650, 200, 50);
            widget.setRectangle(rect);
            widget.setPage(signaturePage);
            widget.setPrinted(true);
            signaturePage.getAnnotations().add(widget);
            acroForm.getFields().add(signatureField);

            document.addPage(signaturePage);

            SignatureOptions signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
            document.addSignature(signature, this, signatureOptions);
            document.saveIncremental(fos);

            log.debug("{}", document.getNumberOfPages());
            document.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        // cannot be done private (interface)
        try {
            SignatureCredentials signatureCredentials = keyStoreService.retrieveCredentials();
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) signatureCredentials.getCertificateChain()[0];
            ContentSigner sha1Signer =
                    new JcaContentSignerBuilder("SHA256WithRSA")
                            .build(signatureCredentials.getPrivateKey());
            gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder().build())
                            .build(sha1Signer, cert));
            gen.addCertificates(
                    new JcaCertStore(Arrays.asList(signatureCredentials.getCertificateChain())));
            CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
            CMSSignedData signedData = gen.generate(msg, false);
            // if (tsaUrl != null && tsaUrl.length() > 0) {
            //     ValidationTimeStamp validation = new ValidationTimeStamp(tsaUrl);
            //     signedData = validation.addSignedTimeStamp(signedData);
            // }
            return signedData.getEncoded();
        } catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
            throw new IOException(e);
        }
    }

    class CMSProcessableInputStream implements CMSTypedData {
        private final InputStream in;
        private final ASN1ObjectIdentifier contentType;

        CMSProcessableInputStream(InputStream is) {
            this(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), is);
        }

        CMSProcessableInputStream(ASN1ObjectIdentifier type, InputStream is) {
            contentType = type;
            in = is;
        }

        @Override
        public Object getContent() {
            return in;
        }

        @Override
        public void write(OutputStream out) throws IOException, CMSException {
            // read the content only one time
            IOUtils.copy(in, out);
            in.close();
        }

        @Override
        public ASN1ObjectIdentifier getContentType() {
            return contentType;
        }
    }
}
