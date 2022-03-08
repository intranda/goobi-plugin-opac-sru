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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import de.intranda.goobi.plugins.SruOpacImport;
import de.intranda.utils.DocumentUtils;
import de.sub.goobi.helper.HttpClientHelper;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;

public class SRUClient {

    private static final Logger logger = Logger.getLogger(SruOpacImport.class);
    private static final String ENCODING = "UTF-8";
    
    private String sruVersion  = "1.1";

    /**
     * Queries the given catalog via Z.3950 (SRU) and returns its response.
     * 
     * @param cat The catalog to query.
     * @param query The query.
     * @param recordSchema The expected record schema.
     * @return Query result XML string.
     * @throws IOException If connecting to the catalog failed
     */
    public String querySRU(ConfigOpacCatalogue cat, String query, String recordSchema) throws IOException {
        String ret = null;
        if (query != null && !query.isEmpty()) {
            query = query.trim();
        }

        if (cat != null) {
            String url = cat.getProtocol();

            url += cat.getAddress();
            url += ":" + cat.getPort();
            url += "/" + cat.getDatabase();
            url += "?version=" + sruVersion;
            url += "&operation=searchRetrieve";
            url += "&query=" + query;
            url += "&maximumRecords=5";
            url += "&recordSchema=" + recordSchema;

            logger.debug("SRU URL: " + url);
            
            ret = HttpClientHelper.getStringFromUrl(url);
//            ret = StringEscapeUtils.unescapeHtml(ret);
            return ret;
        }

        return ret;
    }

    private static byte[] getBytes(CloseableHttpResponse response) throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            response.getEntity().writeTo(baos);
            return baos.toByteArray();
        }
    }

    private static Charset getCharset(CloseableHttpResponse response) {
        HttpEntity entity = response.getEntity();
        ContentType contentType = ContentType.getOrDefault(entity);
        Charset charset = contentType.getCharset();
        return charset;
    }

    public static String encodeAsString(byte[] bytes, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer cbuf = decoder.decode(ByteBuffer.wrap(bytes));
            return cbuf.toString();
        } catch (CharacterCodingException e) {
            logger.error(e.getMessage(), e);
        }

        return new String(bytes);
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

    public static Document retrieveMarcRecord(String input) throws JDOMException, IOException, SRUException {
        Document wholeDoc = DocumentUtils.getDocumentFromString(input, null);
        try {
            Element record = wholeDoc.getRootElement().getChild("records", null).getChild("record", null).getChild("recordData", null).getChild(
                    "record", null);
            Document outDoc = new Document((Element) record.detach());
            return outDoc;
        } catch (NullPointerException e) {
        	Element diagnosticsEle = wholeDoc.getRootElement().getChild("diagnostics", null);
        	if(diagnosticsEle != null && diagnosticsEle.getChild("diagnostic", null) != null) {
        		throw new SRUException(diagnosticsEle.getChild("diagnostic", null).getChildText("message", null));
        	}
        	Element recordsFoundEle = wholeDoc.getRootElement().getChild("numberOfRecords", null);
        	if(recordsFoundEle != null) {
        		String recordsFoundText = recordsFoundEle.getTextTrim();
        		if(StringUtils.isNotBlank(recordsFoundText) && StringUtils.isNumeric(recordsFoundText) && Integer.parseInt(recordsFoundText) == 0) {
        			throw new SRUException("No records found");
        		}
        	}
            throw new SRUException("No parsable response from SRU server");
        }
    }
    
    public String getSruVersion() {
		return sruVersion;
	}
    
    public void setSruVersion(String sruVersion) {
		this.sruVersion = sruVersion;
	}
    
    public static class SRUException extends Exception {
		public SRUException() {
			super();
		}
		public SRUException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
			super(arg0, arg1, arg2, arg3);
		}
		public SRUException(String arg0, Throwable arg1) {
			super(arg0, arg1);
		}
		public SRUException(String arg0) {
			super(arg0);
		}
		public SRUException(Throwable arg0) {
			super(arg0);
		}
    }
}
