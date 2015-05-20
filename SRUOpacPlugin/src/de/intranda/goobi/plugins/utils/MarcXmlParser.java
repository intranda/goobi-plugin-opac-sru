package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.IncompletePersonObjectException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import de.intranda.utils.DocumentUtils;

public abstract class MarcXmlParser {

    private static final Logger logger = Logger.getLogger(MarcXmlParser.class);

    public static class RecordInformation {
        private Date recordDate;
        private String ds;
        private String anchorDs;
        private String childDs;
        private String gattung;
        private String recordIdentifier;

        public RecordInformation(Element element) {
            super();
            String gattung = element.getAttributeValue("gattung");
            String anchor = element.getAttributeValue("anchorType");
            String child = element.getAttributeValue("childType");
            String ds = element.getAttributeValue("mapTo");

            this.gattung = gattung;
            this.ds = ds;
            this.anchorDs = anchor;
            this.childDs = child;
        }

        public RecordInformation(RecordInformation child) {
            this.gattung = child.gattung;
            this.ds = child.anchorDs;
            this.childDs = child.ds;
            this.anchorDs = null;
        }

        private Date createDate(String dateString) {
            String year = dateString.substring(0, 4);
            String month = dateString.substring(4, 6);
            String day = dateString.substring(6);
            try {
                GregorianCalendar calendar = new GregorianCalendar(Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(day));
                return calendar.getTime();
            } catch (NumberFormatException e) {
                LOGGER.error("Unable to convert date String into actual date. Using current date");
                return new Date();
            }
        }

        public String getGattung() {
            return gattung;
        }

        public String getDocStructType() {
            return ds;
        }

        public boolean isAnchor() {
            return childDs == null;
        }

        public Date getRecordDate() {
            return recordDate;
        }

        public void setRecordDate(String dateString) {
            this.recordDate = createDate(dateString);
        }

        public String getDs() {
            return ds;
        }

        public String getAnchorDs() {
            return anchorDs;
        }

        public String getChildDs() {
            return childDs;
        }

        public boolean hasAnchor() {
            return anchorDs != null;
        }

        public String getRecordIdentifier() {
            return recordIdentifier;
        }

        public void setRecordIdentifier(String recordIdentifier) {
            this.recordIdentifier = recordIdentifier;
        }
        
    }

    protected static final Logger LOGGER = Logger.getLogger(MarcXmlParser.class);
    private static final Namespace NS_MARC = Namespace.getNamespace("slim", "http://www.loc.gov/MARC21/slim");
    private static final NumberFormat noSortingFormat = new DecimalFormat("0000");
    private static final NumberFormat noSubSortingFormat = new DecimalFormat("00");

    private Document marcDoc;
    private Document mapDoc;
    private Prefs prefs;
    private boolean writeLogical;
    private boolean writePhysical;
    private boolean writeToAnchor;
    private boolean writeToChild;
    private DocStruct dsLogical;
    private DocStruct dsAnchor;
    private DocStruct dsPhysical;
    // private List<String> anchorMetadataList = new ArrayList<String>();
    private String separator;
    private RecordInformation info;
    private String individualIdentifier = null;
    private boolean treatAsPeriodical = false;

    public MarcXmlParser(Prefs prefs, File mapFile) throws ParserException {
        this.prefs = prefs;
        loadMap(mapFile);
    }

    private void loadMap(File mapFile) throws ParserException {
        try {
            mapDoc = DocumentUtils.getDocumentFromFile(mapFile);
        } catch (JDOMException e) {
            throw new ParserException("Failed to read xml-Document from file " + mapFile.getAbsolutePath() + ":" + e.getMessage());
        } catch (IOException e) {
            throw new ParserException("Failed to open file " + mapFile.getAbsolutePath());
        }
        if (mapDoc == null || !mapDoc.hasRootElement() || !mapDoc.getRootElement().getName().equals("map")
                || mapDoc.getRootElement().getChildren("metadata").isEmpty()) {
            mapDoc = null;
            throw new ParserException("Map document is either invalid or empty");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Element> getMetadataList() {
        if (mapDoc != null) {
            return mapDoc.getRootElement().getChildren("metadata");
        } else {
            return new ArrayList<Element>();
        }
    }

    public DigitalDocument parseMarcXml(Document marcDoc) throws ParserException {
        this.marcDoc = marcDoc;
        DigitalDocument dd = generateDD();
        for (Element metadataElement : getMetadataList()) {
            writeElementToDD(metadataElement, dd);
        }
        addMissingMetadata(dd);
        return dd;
    }

    private void addMissingMetadata(DigitalDocument dd) {
        if (dsLogical != null && dsLogical.hasMetadataType(prefs.getMetadataTypeByName("CurrentNo"))
                && !dsLogical.hasMetadataType(prefs.getMetadataTypeByName("CurrentNoSorting"))) {
            try {
                Metadata md = new Metadata(prefs.getMetadataTypeByName("CurrentNoSorting"));
                md.setValue(dsLogical.getAllMetadataByType(prefs.getMetadataTypeByName("CurrentNo")).get(0).getValue());
                dsLogical.addMetadata(md);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Cannot add CurrentNoSorting: Not allowed for ds " + dsLogical.getType().getName());
            }
        }
        if(dsAnchor != null && !dsAnchor.hasMetadataType(prefs.getMetadataTypeByName("CatalogIDDigital"))) {
            try {
                Metadata md = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
                md.setValue(""+System.currentTimeMillis());
                dsAnchor.addMetadata(md);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Cannot add CatalogIDDigital: Not allowed for ds " + dsAnchor.getType().getName());
            }
        }
        if(dsLogical != null && !dsLogical.hasMetadataType(prefs.getMetadataTypeByName("CatalogIDDigital"))) {
            try {
                Metadata md = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
                md.setValue(info.getRecordIdentifier());
                dsLogical.addMetadata(md);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Cannot add CatalogIDDigital: Not allowed for ds " + dsLogical.getType().getName());
            }
        }
    }

    private DigitalDocument generateDD() throws ParserException {
        boolean anchorMode = true;
        DigitalDocument dd = new DigitalDocument();
        if (info == null) {
            info = getRecordInfo(this.marcDoc);
            anchorMode = false;
        }
        String dsTypeLogical = info.getDocStructType();
        String dsTypePhysical = "BoundBook";
        try {
            dsLogical = dd.createDocStruct(prefs.getDocStrctTypeByName(dsTypeLogical));
            dsPhysical = dd.createDocStruct(prefs.getDocStrctTypeByName(dsTypePhysical));
            if (info.getAnchorDs() != null) {
                dsAnchor = dd.createDocStruct(prefs.getDocStrctTypeByName(info.getAnchorDs()));
                dsAnchor.addChild(dsLogical);
                dd.setLogicalDocStruct(dsAnchor);
            } else {
                dd.setLogicalDocStruct(dsLogical);
            }
            if (dsLogical.getType().isAnchor()) {
                // This is not the main entry, but an anchor
                dsAnchor = dsLogical;
                if (!anchorMode) {
                    dsLogical = dd.createDocStruct(prefs.getDocStrctTypeByName(info.getChildDs()));
                    dsAnchor.addChild(dsLogical);
                } else {
                    dsLogical = null;
                }
                dd.setLogicalDocStruct(dsAnchor);
                treatAsPeriodical = true;
            }
            dd.setPhysicalDocStruct(dsPhysical);
        } catch (TypeNotAllowedForParentException e) {
            throw new ParserException("Unable to create digital document: " + e.getMessage());
        } catch (TypeNotAllowedAsChildException e) {
            throw new ParserException("Unable to create digital document: " + e.getMessage());
        }
        return dd;
    }

    private RecordInformation getRecordInfo(Document marcDoc) {
        if (marcDoc != null && marcDoc.hasRootElement()) {
            Element leader = marcDoc.getRootElement().getChild("leader", NS_MARC);
            if (leader != null) {
                String leaderStr = leader.getValue();
                String dateString = "";
                Element controlField005 = getControlfield(marcDoc, "005");
                Element controlField001 = getControlfield(marcDoc, "001");
                if (controlField005 != null) {
                    dateString = controlField005.getText().substring(0, 8);
                }

                String docStructTitle = getDocType(mapDoc);
                if (docStructTitle == null) {
                    docStructTitle = getDocTypeFromLeader(leaderStr);
                }
                Element docStructEle = getDocStructEle(docStructTitle);
                RecordInformation info = new RecordInformation(docStructEle);
                info.setRecordDate(dateString);
                info.setRecordIdentifier(controlField001.getText());
                return info;
            }
        }
        return null;
    }

    private String getDocTypeFromLeader(String leaderStr) {
        char typeOfRecord = leaderStr.charAt(6);
        char bibliographicLevel = leaderStr.charAt(7);
        if (bibliographicLevel == 'm') {
            //monographic
            switch (typeOfRecord) {
                case 'a':
                    return "Monograph";
                case 'c':
                case 'd':
                    return "SheetMusic";
                case 'e':
                case 'f':
                    return "Map";
                case 't':
                    return "Manuscript";
                default:
                    throw new IllegalArgumentException("Cannot associate marc leader character 06 '" + typeOfRecord
                            + "\' with any known type of record");

            }
        } else if (bibliographicLevel == 'a') {
            //Multivolume
            return "Volume";
        } else if (bibliographicLevel == 's') {
            //Periodical
            return "PeriodicalVolume";
        } else if(bibliographicLevel == 'b') {
            //Serial component
            return "ContainedWork";
        } else {
            throw new IllegalArgumentException("Cannot associate marc leader character 07 '" + bibliographicLevel
                    + "\' with any known bibliographic level");
        }
    }

    protected abstract String getDocType(Document doc);

    private Element getDocStructEle(String docStructTitle) {
        String query = "/map/docstruct[text()=\"" + docStructTitle + "\"]";
        XPathExpression<Element> xpath = XPathFactory.instance().compile(query, Filters.element());
        List<Element> nodeList = new ArrayList<Element>(xpath.evaluate(mapDoc));
        if (nodeList != null && !nodeList.isEmpty()) {
            return nodeList.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String getAchorID() {
        if (this.info != null && !this.info.hasAnchor()) {
            return null;
        }
        XPathExpression<Element> xpath;
        String query1 = null;
        String query2 = null;
        if (this.info.anchorDs.equals("MultiVolumeWork")) {
            query1 = "/slim:record/slim:datafield[@tag=\"958\"][@ind2=\"2\"]/slim:subfield[@code=\"a\"]";
            query2 = "/slim:record/slim:datafield[@tag=\"010\"]/slim:subfield[@code=\"a\"]";
        } else {
            query2 = "/slim:record/slim:datafield[@tag=\"958\"][@ind2=\"1\"]/slim:subfield[@code=\"a\"]";
            query1 = "/slim:record/slim:datafield[@tag=\"453\"]/slim:subfield[@code=\"a\"]";
        }
        try {
            xpath = XPathFactory.instance().compile(query1, Filters.element(), null, NS_MARC);
            List<Element> nodeList = new ArrayList<Element>(xpath.evaluate(marcDoc));
            if (nodeList == null || nodeList.isEmpty()) {
                // try again with different field
                xpath = XPathFactory.instance().compile(query2, Filters.element(), null, NS_MARC);
                nodeList = new ArrayList<Element>(xpath.evaluate(marcDoc));
            }
            if (nodeList != null && !nodeList.isEmpty() && nodeList.get(0) instanceof Element) {
                Element node = nodeList.get(0);
                String id = node.getValue();
                return id;
            } else {
                throw new JDOMException("No datafield 453 with subfield a found in marc document");
            }
        } catch (JDOMException e) {
            LOGGER.error("Unable to retrieve anchor record from datafield 453");
        }
        return null;
    }

    private Element getControlfield(Document marcDoc, String tag) {
        if (marcDoc != null && marcDoc.hasRootElement()) {
            @SuppressWarnings("rawtypes")
            List controlfields = marcDoc.getRootElement().getChildren("controlfield", NS_MARC);
            for (Object object : controlfields) {
                if (object instanceof Element) {
                    if (tag.equals(((Element) object).getAttributeValue("tag"))) {
                        return (Element) object;
                    }
                }
            }
        }
        return null;
    }

    private void writeElementToDD(Element metadataElement, DigitalDocument dd) {
        writeLogical = writeLogical(metadataElement);
        writePhysical = writePhysical(metadataElement);
        separator = getSeparator(metadataElement);
        writeToAnchor = writeToAnchor(metadataElement);
        writeToChild = writeToChild(metadataElement);
        String mdTypeName = getMetadataName(metadataElement);
        if (mdTypeName.equals("Person")) {
            @SuppressWarnings("unchecked")
            List<Element> roleList = metadataElement.getChildren("Role");
            for (Element roleElement : roleList) {
                writePersonXPaths(getXPaths(metadataElement), roleElement);
            }
        } else {
            MetadataType mdType = prefs.getMetadataTypeByName(mdTypeName);
            if (mdType == null) {
                LOGGER.error("Unable To create metadata type " + mdTypeName);
                return;
            }
            if (mdType.getIsPerson()) {
                writePersonXPaths(getXPaths(metadataElement), mdType);
            } else {
                writeMetadataXPaths(getXPaths(metadataElement), mdType, !separateXPaths(metadataElement));
            }
        }

    }

    private String getMetadataName(Element metadataElement) {
        return metadataElement.getChildText("name");
    }

    private String getSeparator(Element metadataElement) {
        if (metadataElement.getAttribute("separator") != null) {
            return metadataElement.getAttributeValue("separator");
        } else {
            return " ";
        }
    }

    private boolean writeLogical(Element metadataElement) {
        return ("true".equals(metadataElement.getAttributeValue("logical")));
    }

    private boolean writePhysical(Element metadataElement) {
        return ("true".equals(metadataElement.getAttributeValue("physical")));
    }

    private boolean writeToAnchor(Element metadataElement) {
        if (treatAsPeriodical) {
            if (metadataElement.getAttributeValue("anchor") != null && !metadataElement.getAttributeValue("anchor").isEmpty()) {
                if (metadataElement.getAttributeValue("anchor").equalsIgnoreCase("false")) {
                    return false;
                }
            }
            return true;
        } else {
            if (metadataElement.getAttributeValue("anchor") != null && !metadataElement.getAttributeValue("anchor").isEmpty()) {
                if (metadataElement.getAttributeValue("anchor").equalsIgnoreCase("true")) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean writeToChild(Element metadataElement) {
        if (treatAsPeriodical) {
            if (metadataElement.getAttributeValue("child") != null && !metadataElement.getAttributeValue("child").isEmpty()) {
                if (metadataElement.getAttributeValue("child").equalsIgnoreCase("true")) {
                    return true;
                }
            }
            return false;
        } else {
            if (metadataElement.getAttributeValue("child") != null && !metadataElement.getAttributeValue("child").isEmpty()) {
                if (metadataElement.getAttributeValue("child").equalsIgnoreCase("false")) {
                    return false;
                }
            }
            return true;
        }
    }

    private void writeMetadataXPaths(List<Element> eleXpathList, MetadataType mdType, boolean mergeXPaths) {

        List<String> valueList = new ArrayList<String>();
        for (Element eleXpath : eleXpathList) {
            try {
                boolean mergeOccurances = !separateOccurances(eleXpath);
                String query = generateQuery(eleXpath);
                String subfields = eleXpath.getAttributeValue("subfields");
                List<Element> nodeList = getXpathNodes(query);
                String prefix = eleXpath.getAttributeValue("prefix");
                String suffix = eleXpath.getAttributeValue("suffix");
                String ignoreRegex = eleXpath.getAttributeValue("ignore");
                if (ignoreRegex != null) {
                    ignoreRegex.replace("\\", "\\\\");
                }

                // read values
                if (nodeList != null && !nodeList.isEmpty()) {
                    List<String> nodeValueList = getMetadataNodeValues(nodeList, subfields, mdType, mergeOccurances);

                    List<String> tempList = new ArrayList<String>();
                    StringBuilder sb = new StringBuilder();
                    for (String string : nodeValueList) {
                        if (ignoreRegex != null) {
                            string = string.replaceAll(ignoreRegex, "");
                        }
                        sb.append((prefix != null) ? prefix : "");
                        sb.append(string);
                        sb.append((suffix != null) ? suffix : "");
                        if (mergeOccurances) {
                            sb.append(separator);
                        } else {
                            tempList.add(sb.toString());
                            sb = new StringBuilder();
                        }
                    }
                    if (mergeOccurances) {
                        tempList.add(sb.substring(0, sb.length() - separator.length()));
                    }
                    nodeValueList = tempList;

                    if (mergeXPaths) {
                        int count = 0;
                        for (String value : nodeValueList) {
                            if (value != null && valueList.size() <= count) {
                                valueList.add(value);
                            } else if (value != null && !value.trim().isEmpty()) {
                                value = valueList.get(count) + separator + value;
                                valueList.set(count, value);
                            }
                            count++;
                        }
                    } else {
                        valueList.addAll(nodeValueList);
                    }

                }

            } catch (JDOMException e) {
                LOGGER.error("Error parsing mods section for node " + eleXpath.getTextTrim(), e);
                continue;
            }
        }

        // create and write medadata
        for (String value : valueList) {
            String cleanedValue = cleanValue(mdType, value);
            try {
                Metadata md = new Metadata(mdType);
                md.setValue(cleanedValue);
                writeMetadata(md);

            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Failed to create metadata " + mdType.getName());
            }
            if (mdType.getName().equals("TitleDocMain")) {
                writeSortingTitle(value);
            }
            if (mdType.getName().equals("CurrentNo")) {
                writeCurrentNoSort(value);
            }
        }

    }

    private void writeCurrentNoSort(String value) {
        if (dsLogical != null && !dsLogical.hasMetadataType(prefs.getMetadataTypeByName("CurrentNoSorting"))) {
            String sortingValue = createCurrentNoSort(value);

            try {
                Metadata md = new Metadata(prefs.getMetadataTypeByName("CurrentNoSorting"));
                md.setValue(sortingValue);
                writeMetadata(md);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Failed to create metadata CurrentNoSorting");
            }
        }
    }

    protected abstract String createCurrentNoSort(String value);

    private String cleanValue(MetadataType mdType, String value) {
        String returnValue = value;
        if (mdType.getName().equals("shelfmarksource") || mdType.getName().equals("TitleDocMainShort")) {
            returnValue = value.replaceAll("<<.+?>>", "").trim();
        } else if (mdType.getName().equals("TitleDocMain")) {
            returnValue = value.replaceAll("<<|>>", "").trim();
        } else if (mdType.getName().equals("CurrentNoSorting")) {
            returnValue = createCurrentNoSort(value);
        }
        return returnValue;
    }

    private void writeSortingTitle(String value) {
        DocStruct myDs = dsLogical != null ? dsLogical : dsAnchor;
        if (myDs != null && !myDs.hasMetadataType(prefs.getMetadataTypeByName("TitleDocMainShort"))) {
            String sortedValue = value.replaceAll("<<.+?>>", "").trim();
            try {
                Metadata md = new Metadata(prefs.getMetadataTypeByName("TitleDocMainShort"));
                md.setValue(sortedValue);
                writeMetadata(md);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Failed to create metadata TitleDocMainShort");
            }
        }

    }

    @SuppressWarnings("unused")
    private boolean testConditions(Element eleXpath) {
        boolean ret = true;
        for (Element condition : getChildElements(eleXpath, "conditional")) {
            int test = 0;
            List<Element> xpaths = getChildElements(condition, "xpath");
            for (Element xpath : xpaths) {
                try {
                    List<Element> nodes = getXpathNodes(xpath.getValue().trim());
                    for (Element element : nodes) {
                        if (condition.getChild("value") == null) {
                            // no condition: just check if node exists
                            test++;
                            break;
                        } else if (condition.getChildText("value").trim().equals(element.getValue().trim())) {
                            test++;
                            break;
                        }
                    }
                } catch (JDOMException e) {
                    LOGGER.error("Unable to resolve xpath " + xpath.getValue() + ": " + e.getMessage());
                }
            }
            if ("true".equals(condition.getAttributeValue("trueForAll"))) {
                ret = (ret && test == xpaths.size());
            } else {
                ret = (ret && test > 0);
            }
        }
        return ret;
    }

    private List<Element> getChildElements(Element parent, String name) {
        @SuppressWarnings("rawtypes")
        List childNodes = null;
        if (name != null) {
            childNodes = parent.getChildren(name, NS_MARC);
        } else {
            childNodes = parent.getChildren();
        }

        List<Element> eleList = new ArrayList<Element>(childNodes.size());
        for (Object node : childNodes) {
            if (node instanceof Element) {
                eleList.add((Element) node);
            }
        }
        return eleList;
    }

    private List<Element> getSubfieldsByCode(Element parent, String code) {
        @SuppressWarnings("rawtypes")
        List childNodes = parent.getChildren("subfield", NS_MARC);

        List<Element> eleList = new LinkedList<Element>();
        for (Object node : childNodes) {
            if (node instanceof Element) {
                if (((Element) node).getAttributeValue("code").equals(code)) {
                    eleList.add((Element) node);
                }
            }
        }
        return eleList;
    }

    private void writePersonXPaths(List<Element> eleXpathList, MetadataType mdType) {

        for (Element eleXpath : eleXpathList) {
            try {
                String query = generateQuery(eleXpath);
                List<Element> nodeList = getXpathNodes(query);
                if (nodeList != null) {
                    writePersonNodeValues(nodeList, mdType);
                }

            } catch (JDOMException e) {
                LOGGER.error("Error parsing mods section for node " + eleXpath.getTextTrim(), e);
                continue;
            }
        }
    }

    private void writePersonXPaths(List<Element> eleXpathList, Element roleElement) {

        for (Element eleXpath : eleXpathList) {
            try {
                String query = generateQuery(eleXpath);
                List<Element> nodeList = getXpathNodes(query);
                nodeList = filterByRole(nodeList, roleElement);
                MetadataType mdType = getPersonType(roleElement);
                if (nodeList != null && mdType != null) {
                    writePersonNodeValues(nodeList, mdType);
                }

            } catch (JDOMException e) {
                LOGGER.error("Error parsing mods section for node " + eleXpath.getTextTrim(), e);
                continue;
            }
        }
    }

    private MetadataType getPersonType(Element roleElement) {
        String typeName = roleElement.getValue().trim();
        MetadataType type = prefs.getMetadataTypeByName(typeName);
        return type;
    }

    private List<Element> filterByRole(List<Element> nodeList, Element roleElement) {
        List<Element> returnList = new LinkedList<Element>();
        for (Element node : nodeList) {
            boolean write = false;
            String subfield = roleElement.getAttributeValue("subfield");
            String subfieldValue = roleElement.getAttributeValue("value");
            List<Element> subfieldList = getSubfieldsByCode(node, subfield);
            if (subfieldValue == null || subfieldValue.isEmpty() && subfieldList.isEmpty()) {
                write = true;
            } else {
                for (Element element : subfieldList) {
                    if (element.getValue().trim().equals(subfieldValue.trim())) {
                        write = true;
                        break;
                    }
                }
            }
            if (write) {
                returnList.add(node);
            }
        }
        return returnList;
    }

    private String generateQuery(Element eleXpath) {
        String tag = eleXpath.getAttributeValue("tag");
        String ind1 = eleXpath.getAttributeValue("ind1");
        String ind2 = eleXpath.getAttributeValue("ind2");
        //		String subfields = eleXpath.getAttributeValue("subfields");
        StringBuilder query = new StringBuilder("/slim:record/slim:datafield");
        if (tag != null) {
            query.append("[@tag=\"" + tag + "\"]");
        }
        if (ind1 != null) {
            query.append("[@ind1=\"" + ind1 + "\"]");
        }
        if (ind2 != null) {
            query.append("[@ind2=\"" + ind2 + "\"]");
        } else if (!"true".equals(eleXpath.getParentElement().getAttributeValue("child"))) {
            query.append("[not(@ind2=\"2\")]");
        }

        return query.toString();
    }

    private List<Element> getXpathNodes(String query) throws JDOMException {

        XPathExpression<Element> xpath = XPathFactory.instance().compile(query, Filters.element(), null, NS_MARC);
        List<Element> nodeList = new ArrayList<Element>(xpath.evaluate(marcDoc));
        return nodeList;
    }

    private void writePersonNodeValues(List<Element> xPathNodeList, MetadataType mdType) {
        for (Element node : xPathNodeList) {
            String displayName = "";
            String nameNumeration = "";
            String firstName = "";
            String lastName = "";
            String termsOfAddress = "";
            String date = "";
            String affiliation = "";
            String authorityID = "";
            String institution = "";
            String identifier = "";
            String roleTerm = mdType.getName();

            // get subelements of person
            for (Object o : node.getChildren()) {
                if (o instanceof Element) {
                    Element eleSubField = (Element) o;
                    if ("a".equals(eleSubField.getAttributeValue("code"))) {
                        // name
                        String[] name = getNameParts(eleSubField.getValue());
                        firstName = name[0];
                        lastName = name[1];
                    } else if ("b".equals(eleSubField.getAttributeValue("code"))) {
                        // numeration
                        nameNumeration = eleSubField.getValue();
                    } else if ("c".equals(eleSubField.getAttributeValue("code"))) {
                        termsOfAddress = eleSubField.getValue();
                    } else if ("d".equals(eleSubField.getAttributeValue("code"))) {
                        date = eleSubField.getValue();
                    } else if ("e".equals(eleSubField.getAttributeValue("code"))) {
//                        roleTerm = eleSubField.getValue();
                    } else if ("u".equals(eleSubField.getAttributeValue("code"))) {
                        affiliation = eleSubField.getValue();
                    } else if ("0".equals(eleSubField.getAttributeValue("code"))) {
                        authorityID = eleSubField.getValue();
                    } else if ("5".equals(eleSubField.getAttributeValue("code"))) {
                        institution = eleSubField.getValue();
                    } else if ("q".equals(eleSubField.getAttributeValue("code"))) {
                        // name
                        String[] name = getNameParts(eleSubField.getValue());
                        firstName = name[0];
                        lastName = name[1];
                    } else if ("p".equals(eleSubField.getAttributeValue("code"))) {
                        // name
                        String[] name = getNameParts(eleSubField.getValue());
                        firstName = name[0];
                        lastName = name[1];
                    } else if ("9".equals(eleSubField.getAttributeValue("code"))) {
                        identifier = eleSubField.getValue();
                        ;
                    }

                }
            }

            // get displayName
            displayName = (firstName + " " + lastName + " " + nameNumeration).trim();
            if (!termsOfAddress.isEmpty()) {
                displayName += ", " + termsOfAddress;
            }

            // create and write metadata
            if (StringUtils.isNotEmpty(lastName)) {
                Person person = null;
                try {
                    person = new Person(mdType);
                    person.setFirstname(firstName);
                    person.setLastname(lastName);
                    person.setDisplayname(displayName);
                    person.setAffiliation(affiliation);
                    //					person.setAutorityFileID(authorityID);
                    person.setInstitution(institution);
                    //					person.setIdentifier(identifier);
                    person.setRole(roleTerm);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.error("Failed to create person metadata " + mdType.getName());
                }

                if (person != null) {
                    writePerson(person);
                }
            }
        }

    }

    private String[] getNameParts(String name) {
        String firstName = null;
        String lastName = null;
        if (name.contains(",")) {
            lastName = name.substring(0, name.indexOf(",")).trim();
            firstName = name.substring(name.indexOf(",") + 1).trim();
        } else if (name.contains(" ")) {
            firstName = name.substring(0, name.lastIndexOf(" ")).trim();
            lastName = name.substring(name.lastIndexOf(" ") + 1).trim();
        } else {
            lastName = name;
            firstName = "";
        }
        return new String[] { firstName, lastName };
    }

    private void writeMetadata(Metadata metadata) {

        if (writeLogical) {
            if (writeToChild && dsLogical != null) {
                try {
                    dsLogical.addMetadata(metadata);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.error("Failed to write metadata " + metadata.getType().getName() + " to logical topStruct: " + e.getMessage());
                } catch (IncompletePersonObjectException e) {
                    LOGGER.error("Failed to write metadata " + metadata.getType().getName() + " to logical topStruct: " + e.getMessage());
                }
            }
            if (writeToAnchor && dsAnchor != null) {
                // if (dsAnchor != null && (anchorMetadataList == null ||
                // anchorMetadataList.contains(metadata.getType().getName()))) {
                try {
                    dsAnchor.addMetadata(metadata);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.warn("Failed to write metadata " + metadata.getType().getName() + " to logical anchor: " + e.getMessage());

                } catch (IncompletePersonObjectException e) {
                    LOGGER.warn("Failed to write metadata " + metadata.getType().getName() + " to logical anchor: " + e.getMessage());

                }
                // }
            }
        }

        if (writePhysical) {
            try {
                dsPhysical.addMetadata(metadata);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Failed to write metadata " + metadata.getType().getName() + " to physical topStruct: " + e.getMessage());

            } catch (IncompletePersonObjectException e) {
                LOGGER.error("Failed to write metadata " + metadata.getType().getName() + " to physical topStruct: " + e.getMessage());

            }
        }

    }

    private void writePerson(Person person) {

        if (writeLogical) {
            if (writeToChild && dsLogical != null) {
                try {
                    dsLogical.addPerson(person);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.error("Failed to write person " + person.getType().getName() + " to logical topStruct: " + e.getMessage());
                } catch (IncompletePersonObjectException e) {
                    LOGGER.error("Failed to write person " + person.getType().getName() + " to logical topStruct: " + e.getMessage());
                }
            }
            if (writeToAnchor && dsAnchor != null) {
                // if (dsAnchor != null && (anchorMetadataList == null ||
                // anchorMetadataList.contains(person.getType().getName()))) {
                try {
                    dsAnchor.addPerson(person);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.warn("Failed to write person " + person.getType().getName() + " to logical anchor: " + e.getMessage());

                } catch (IncompletePersonObjectException e) {
                    LOGGER.warn("Failed to write person " + person.getType().getName() + " to logical anchor: " + e.getMessage());

                }
                // }
            }
        }

        if (writePhysical) {
            try {
                dsPhysical.addPerson(person);
            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Failed to write person " + person.getType().getName() + " to physical topStruct: " + e.getMessage());

            } catch (IncompletePersonObjectException e) {
                LOGGER.error("Failed to write person " + person.getType().getName() + " to physical topStruct: " + e.getMessage());

            }
        }

    }

    private List<String> getMetadataNodeValues(@SuppressWarnings("rawtypes") List nodeList, String subfields, MetadataType mdType,
            boolean mergeOccurances) {

        List<String> valueList = new ArrayList<String>();
        Set<String> codes = new HashSet<String>();

        for (Object objValue : nodeList) {
            String value = "";
            if (objValue instanceof Element) {
                Element eleValue = (Element) objValue;
                LOGGER.debug("mdType: " + mdType.getName() + "; Value: " + eleValue.getTextTrim());
                for (Element subfield : getChildElements(eleValue, "subfield")) {
                    String code = subfield.getAttributeValue("code");
                    if (subfields != null && subfields.contains(code)) {
                        if (codes.add(code) || mergeOccurances) {
                            value += subfield.getValue() + separator;
                        } else {
                            valueList.add(subfield.getValue());
                        }
                    }
                }
            } else if (objValue instanceof Attribute) {
                Attribute atrValue = (Attribute) objValue;
                LOGGER.debug("mdType: " + mdType.getName() + "; Value: " + atrValue.getValue());
                value = atrValue.getValue();
            }
            if (value.length() > separator.length()) {
                value = value.substring(0, value.length() - separator.length());
            }
            if (!value.isEmpty()) {
                valueList.add(value);
            }
        }

        return valueList;
    }

    private boolean separateXPaths(Element metadataElement) {
        Attribute attr = metadataElement.getAttribute("separateXPaths");
        // if(attr == null) {
        // attr =
        // metadataElement.getParentElement().getAttribute("separateXPaths");
        // }
        if (attr != null && attr.getValue().equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean separateOccurances(Element xpathElement) {
        Attribute attr = xpathElement.getAttribute("separateOccurances");
        if (attr == null) {
            attr = xpathElement.getParentElement().getAttribute("separateOccurances");
        }
        if (attr != null && attr.getValue().equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    private List<Element> getXPaths(Element metadataElement) {
        @SuppressWarnings("unchecked")
        List<Element> xPathElements = metadataElement.getChildren("marcfield");
        return xPathElements;
    }

    public class ParserException extends Exception {

        private static final long serialVersionUID = 8545796396812133082L;

        public ParserException(String string) {
            super(string);
        }

    }

    public RecordInformation getInfo() {
        return info;
    }

    public void setInfo(RecordInformation info) {
        this.info = info;
    }

    public String getIndividualIdentifier() {
        return individualIdentifier;
    }

    public void setIndividualIdentifier(String individualIdentifier) {
        this.individualIdentifier = individualIdentifier;
    }

}