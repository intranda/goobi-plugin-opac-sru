/**
 * This file is part of the SRU opac import plugin for the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - http://digiverso.com 
 *          - http://www.intranda.com
 * 
 * Copyright 2013, intranda GmbH, Göttingen
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */

package de.intranda.goobi.plugins.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import de.intranda.goobi.plugins.SruOpacImport;
import de.intranda.utils.DocumentUtils;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;

public class SRUClient {

    private static final Logger logger = Logger.getLogger(SruOpacImport.class);
    private static final String ENCODING = "UTF-8";

    /**
     * Queries the given catalog via Z.3950 (SRU) and returns its response.
     * 
     * @param cat The catalog to query.
     * @param query The query.
     * @param recordSchema The expected record schema.
     * @return Query result XML string.
     */
    public static String querySRU(ConfigOpacCatalogue cat, String query, String recordSchema) {
        String ret = null;
        if (query != null && !query.isEmpty()) {
            query = query.trim();
        }

        if (cat != null) {
            String url = "http://";

            url += cat.getAddress();
            url += ":" + cat.getPort();
            url += "/" + cat.getDatabase();
            url += "?version=1.1";
            url += "&operation=searchRetrieve";
            url += "&query=" + query;
            url += "&maximumRecords=5";
            url += "&recordSchema=" + recordSchema;

            logger.debug("SRU URL: " + url);

            
            HttpGet httpGet = new HttpGet(url);
            ResponseHandler<String> handler = new BasicResponseHandler();
            try (CloseableHttpClient httpclient = HttpClients.createDefault()){
                CloseableHttpResponse response = httpclient.execute(httpGet);
                ret = handler.handleResponse(response);
                Charset charset = getCharset(response);
                charset = Charset.forName("ISO-8859-15");
                if(charset != null) {                    
                    ret = convertStringEncoding(ret, charset.name() , ENCODING);
                }
//                if (!method.getResponseCharSet().equalsIgnoreCase(ENCODING)) {
//                    // If response XML is not UTF-8, re-encode
//                    ret = convertStringEncoding(ret, method.getResponseCharSet(), ENCODING);
//                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return ret;
    }

    private static Charset getCharset(CloseableHttpResponse response) {
        HttpEntity entity = response.getEntity();
        ContentType contentType = ContentType.getOrDefault(entity);
        Charset charset = contentType.getCharset();
        return charset;
    }

    /**
     * Converts a <code>String</code> from one given encoding to the other.
     * 
     * @param string The string to convert.
     * @param from Source encoding.
     * @param to Destination encoding.
     * @return The converted string.
     */
    public static String convertStringEncoding(String string, String from, String to) {
        try {
            Charset charsetFrom = Charset.forName(from);
            Charset charsetTo = Charset.forName(to);
            CharsetEncoder encoder = charsetFrom.newEncoder();
            CharsetDecoder decoder = charsetTo.newDecoder();
            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(string));
            CharBuffer cbuf = decoder.decode(bbuf);
            return cbuf.toString();
        } catch (CharacterCodingException e) {
            logger.error(e.getMessage(), e);
        }

        return string;
    }

    public static Document retrieveMarcRecord(String input) throws JDOMException, IOException {
        Document wholeDoc = DocumentUtils.getDocumentFromString(input, null);
        try {
            Element record =
                    wholeDoc.getRootElement().getChild("records", null).getChild("record", null).getChild("recordData", null)
                            .getChild("record", null);
            Document outDoc = new Document((Element) record.detach());
            return outDoc;
        } catch (NullPointerException e) {
            return null;
        }
    }
}
