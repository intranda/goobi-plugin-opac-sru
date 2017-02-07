package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import ugh.dl.Prefs;

public class MarcXmlParserFU extends MarcXmlParser {
    
    private static final Logger logger = Logger.getLogger(MarcXmlParserFU.class);


    public MarcXmlParserFU(Prefs prefs, File mapFile) throws ParserException {
        super(prefs, mapFile);
    }

    @Override
    protected String getDocType(Document doc) {
        String query = generateQuery("245", null, null, "n");
        try {
			List<Element> nodes = getXpathNodes(query);
			if(!nodes.isEmpty()) {
				return "Volume";
			}
		} catch (JDOMException e) {
			logger.error(e);
			
		}
        return null;
    }

    @Override
    protected String createCurrentNoSort(String value) {
    	 value = value.replaceAll("\\D", "");
         return value;
    }

}
