package de.intranda.goobi.plugins.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

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
            // ?version=1.1&operation=searchRetrieve&query=dinosaur&maximumRecords=5&recordSchema=marcxml
            String url = "http://";
            //            if ((cat.getLogin() != null) && (cat.getLogin().length() > 0)) {
            //                url += cat.getLogin();
            //                if ((cat.getPassword() != null) && (cat.getPassword().length() > 0)) {
            //                    url += ":" + cat.getPassword();
            //                }
            //                url += "@";
            //            }
            url += cat.getAddress();
            url += ":" + cat.getPort();
            url += "/" + cat.getDatabase();
            url += "?version=1.1";
            url += "&operation=searchRetrieve";
            url += "&query=" + query;
            url += "&maximumRecords=5";
            url += "&recordSchema=" + recordSchema;

            logger.debug("SRU URL: " + url);

            HttpClient client = new HttpClient();
            GetMethod method = new GetMethod(url);
            try {
                client.executeMethod(method);
                ret = method.getResponseBodyAsString();
                if (!method.getResponseCharSet().equalsIgnoreCase(ENCODING)) {
                    // If response XML is not UTF-8, re-encode
                    ret = convertStringEncoding(ret, method.getResponseCharSet(), ENCODING);
                }
            } catch (HttpException e) {
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } finally {
                method.releaseConnection();
            }
        }

        return ret;
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
            throw new JDOMException("Unable to find marcxml record");
        }
    }

    public static void main(String[] args) throws ConfigurationException, JDOMException, IOException {
        //        DataManager manager = DataManager.getInstance();
        //        manager.loadCatalogs(new File("resources/catalogs.xml").getAbsolutePath());
        //        Catalog cat = manager.getCatalogByName("HU Berlin Aleph");
        //        String query = "BV000388474";
        //        String recordSchema = cat.getFormatString();
        //        String answer = queryZ3950SRU(cat, query, recordSchema);
        ////        System.out.println(answer);
        //        Document marcXmlDoc = retrieveMarcRecord(answer);
        ////        System.out.println(DocumentUtils.getStringFromDocument(marcXmlDoc, null));
        //        File ruleset = new File("/opt/digiverso/goobi/rulesets/gdz.xml");
        //        File mapFile = new File("resources/marc_map.xml");
        //        try {
        //            Marc21Parser parser = new Marc21Parser(ruleset, mapFile);
        //            parser.parseMarcXml(marcXmlDoc);
        //        } catch (ParserException e) {
        //            logger.error(e);
        //        }
    }

}
