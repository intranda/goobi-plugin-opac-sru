package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import ugh.dl.MetadataType;
import ugh.dl.Prefs;

public class MarcXmlParserHU extends MarcXmlParser {
    
    private static final Logger logger = Logger.getLogger(MarcXmlParserHU.class);
    protected static final Namespace NS_MARC = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");
    protected static final Namespace NS_SLIM = Namespace.getNamespace("slim", "http://www.loc.gov/MARC21/slim");
    private static final NumberFormat[] currentNoSortingFormats = {new DecimalFormat("0000"), new DecimalFormat("000"), new DecimalFormat("00")};


    public MarcXmlParserHU(Prefs prefs) throws ParserException {
        super(prefs);
    }

    @Override
    protected String getDocType(Document doc) {
        String nsPrefix = "";
        if(getNamespace() != null && StringUtils.isNotBlank(getNamespace().getPrefix())) {
            nsPrefix = getNamespace().getPrefix() + ":";
        }
        String query = "/"+nsPrefix+"record/"+nsPrefix+"datafield[@tag=\"959\"]/"+nsPrefix+"subfield[@code=\"a\"]";
            XPathExpression<Element> xpath = XPathFactory.instance().compile(query, Filters.element(), null, getNamespace());
            List<Element> nodeList = xpath.evaluate(marcDoc);//selectNodes(marcDoc);
            if(nodeList == null || nodeList.isEmpty()) {
                query = "/"+nsPrefix+"record/"+nsPrefix+"datafield[@tag=\"655\"]/"+nsPrefix+"subfield[@code=\"a\"]";
                xpath = XPathFactory.instance().compile(query, Filters.element(), null, getNamespace());
                nodeList = xpath.evaluate(marcDoc);//selectNodes(marcDoc);
            }
            if(nodeList != null && !nodeList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Element element : nodeList) {
                    sb.append(element.getValue()).append("|");
                }
                String typeName = sb.toString();
                if(typeName.endsWith("|")) {
                    typeName = typeName.substring(0, typeName.length()-1);
                }
                return typeName;
            }
        return "";
    }
    
    @Override
    protected void cleanValue(MetadataType mdType, GoobiMetadataValue mdValue) {
        String separator = "\\|";
        String separatorReplacement = ", ";
        String value = mdValue.getValue();
        if(mdType.getName().equals("CurrentNo")) {
           String[] tokens = value.split(separator);
           if(tokens.length > 1 && !tokens[tokens.length-1].trim().matches("\\d+") && tokens[tokens.length-2].trim().matches("\\d+")) {
               tokens = Arrays.copyOfRange(tokens, 0, tokens.length-1);
           }
           String ret = StringUtils.join(tokens, separator);
           ret = ret.replace(separator, separatorReplacement);
           mdValue.setValue(value);
        } else {            
            super.cleanValue(mdType, mdValue);
        }
    }

    @Override
    protected String createCurrentNoSort(String value) {
        String sortingValue = value;
        if (dsLogical != null && dsLogical.getType().getName().toLowerCase().contains("periodical")) {
            StringBuilder builder = new StringBuilder();
            String[] parts = value.split(separator);
            for (int i = 0; i < 3; i++) {
                NumberFormat sortingFormat = currentNoSortingFormats[Math.min(i, currentNoSortingFormats.length-1)];
                String partSort = sortingFormat.format(0);
                if (parts.length > i && !parts[i].isEmpty()) {
                    try {
                        if (parts[i].contains("/") || parts[i].contains("-")) {
                            String[] subparts = parts[i].split("[/-]");
                            if (subparts.length > 0) {
                                partSort = sortingFormat.format(Integer
                                        .valueOf(subparts[0].replaceAll("\\D",
                                                "")));
                            }
                        } else {
                            String sortedPart = parts[i].replaceAll("\\D", "");
                            int no = Integer.valueOf(sortedPart);
                            partSort = sortingFormat.format(no);
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.error(e);
                    }
                }
                builder.append(partSort);
            }
            sortingValue = builder.toString();
        } else {
            sortingValue = value.replaceAll("\\D", "");
        }
        return sortingValue;
    }
    
    @Override
    protected List<Element> getXpathNodes(String query) throws JDOMException {
        XPathExpression<Element> xpath = XPathFactory.instance().compile(query, Filters.element(), null, NS_MARC, NS_SLIM);
        List<Element> nodeList = new ArrayList<>(xpath.evaluate(marcDoc));
        if (individualIdentifier != null
                && nodeList != null
                && nodeList.size() > 1
                && ("999".equals(nodeList.get(0).getParentElement()
                        .getAttributeValue("tag")) || "999".equals(nodeList
                        .get(0).getAttributeValue("tag")))) {
            return selectMatching999(nodeList);
        } else {
            return nodeList;
        }
    }

    private List<Element> selectMatching999(List<Element> nodeList) {
        List<Element> newList = new ArrayList<Element>();
        for (Element element : nodeList) {
            List<Element> children = element.getChildren();
            for (Element child : children) {
                if (individualIdentifier.equals(child.getValue().trim())) {
                    newList.add(element);
                    break;
                }
            }
        }
        if (newList.isEmpty()) {
            newList.add(nodeList.get(0));
        }
        return newList;
    }

}
