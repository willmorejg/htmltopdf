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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import net.ljcomputing.htmltopdf.service.Html5ParsingService;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
public class Html5ParsingServiceImpl implements Html5ParsingService {
    @Autowired W3CDom w3cDom;

    /**
     * Parse an HTML 5 Document from a URL.
     *
     * <p>Original code: https://github.com/danfickle/openhtmltopdf/wiki/Integration-Guide
     *
     * @param urlStr
     * @param timeoutMs
     * @throws IOException
     */
    @Override
    public Document html5ParseDocument(String urlStr, int timeoutMs) throws IOException {
        URL url = new URL(urlStr);
        org.jsoup.nodes.Document doc;

        if (url.getProtocol().equalsIgnoreCase("file")) {
            doc = Jsoup.parse(new File(url.getPath()), "UTF-8");
        } else {
            doc = Jsoup.parse(url, timeoutMs);
        }

        return w3cDom.fromJsoup(doc);
    }
}
