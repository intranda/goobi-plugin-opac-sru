package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.IncompletePersonObjectException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import de.intranda.utils.DocumentUtils;

public class Marc21Parser {

    public class RecordInformation {
        private MarcRecordType recordType;
        private MarcBibliographicLevel bibLevel;
        private MarcMultipartLevel partLevel;
        private Date recordDate;
        private String ds;
        private String gattung;

        public RecordInformation(MarcRecordType recordType, MarcBibliographicLevel bibLevel, MarcMultipartLevel partLevel, String dateString) {
            super();
            this.recordType = recordType;
            this.bibLevel = bibLevel;
            this.partLevel = partLevel;
            this.recordDate = createDate(dateString);
            setDs();
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

        private void setDs() {
            switch (recordType) {
                case CARTOGRAPHIC:
                case MANUSCRIPTCARTOGRAPHIC:
                    ds = "Cartographic";
                    gattung = "Ka";
                    break;
                case NOTATEDMUSIC:
                case MANUSCRIPTNOTATEDMUSIC:
                    ds = "SheetMusic";
                    gattung = "Ma";
                    break;
                case LANGUAGEMATERIAL:
                case MIXEDMATERIALS:
                    ds = "Monograph";
                    gattung = "Za";
                    break;
                case MANUSCRIPTLANGUAGEMATERIAL:
                    ds = "Manuscript";
                    gattung = "Ha";
                    break;
                default:
                    ds = "Monograph";
                    gattung = "Aa";
            }

            switch (bibLevel) {
                case SERIAL:
                    ds = "Periodical";
                    gattung = "Ab";
                    break;
                case SERIALPART:
                    ds = "PeriodicalPart";
                    gattung = "Av";
                    break;
                case MONOGRAPHICPART:
                case SUBUNIT:
                    ds = "MultiVolumePart";
                    gattung = "Af";
                    break;
                case COLLECTION:
                    ds = "Series";
                    gattung = "Ad";
                    break;
                case INTEGRATINGRESOURCE:
                case MONOGRAPH:
                default:
            }

            if (ds.equals("Monograph")) {
                switch (partLevel) {
                    case DEPENDENTPART:
                    case INDEPENDENTPART:
                        ds = "SerialMonograph";
                        gattung = "Av";
                        break;
                    case SET:
                        ds = "Series";
                        gattung = "Ad";
                    default:
                        break;
                }
            }
        }

        public String getGattung() {
            return gattung;
        }

        public String getDocStructType() {
            return ds;
        }

        public boolean isAnchor() {
            if (ds.equals("Series") || ds.equals("MultiVolume") || ds.equals("Periodical")) {
                return true;
            } else {
                return false;
            }
        }

        public MarcRecordType getRecordType() {
            return recordType;
        }

        public MarcBibliographicLevel getBibLevel() {
            return bibLevel;
        }

        public MarcMultipartLevel getPartLevel() {
            return partLevel;
        }

        public Date getRecordDate() {
            return recordDate;
        }

        public String getDs() {
            return ds;
        }

        public String getAnchorDs() {
            if (ds.equals("MultiVolumePart") || ds.equals("Volume")) {
                return "MultiVolumeWork";
            } else if (ds.equals("SerialMonograph")) {
                return "Series";
            } else if (ds.equals("PeriodicalVolume") || ds.equals("PeriodicalPart")) {
                return "Periodical";
            } else {
                return null;
            }
        }
        
        public String getChildDs() {
        	if(ds.equals("Periodical")) {
        		return "PeriodicalVolume";
        	} else if(ds.equals("MultiVolumeWork")) {
        		return "Volume";
        	} else if(ds.equals("Series")) {
        		return "SerialMonograph";
        	}
        	return null;
        }

        public void setDocStruct(DocStructType structType) {
            ds = structType.getName();
            if ("Periodical".equals(ds)) {
                gattung = "Ab";
            } else if ("PeriodicalPart".equals(ds) || "PeriodicalVolume".equals(ds)) {
                gattung = "Av";
            } else if ("MultiVolumeWork".equals(ds)) {
                gattung = "Ac";
            } else if ("Volume".equals(ds) || "MultiVolumePart".equals(ds)) {
                gattung = "Af";
            } else if ("Monograph".equals(ds)) {
                gattung = "Aa";
            } else if ("SerialMonograph".equals(ds)) {
                gattung = "Av";
            } else if ("Series".equals(ds)) {
                gattung = "Ad";
            }

        }
    }

    public enum MarcRecordType {

        LANGUAGEMATERIAL("Language material", "a"), NOTATEDMUSIC("Notated music", "c"), MANUSCRIPTNOTATEDMUSIC("Manuscript notated music", "d"),
        CARTOGRAPHIC("Cartographic marterial", "e"), MANUSCRIPTCARTOGRAPHIC("Manuscript cartographic material", "f"), MIXEDMATERIALS(
                "Mixed materials", "p"), MANUSCRIPTLANGUAGEMATERIAL("Manuscript language material", "t");

        private String label, identifyingCharacter;

        private MarcRecordType(String label, String identifyingCharacter) {
            this.label = label;
            this.identifyingCharacter = identifyingCharacter;
        }

        public String getLabel() {
            return label;
        }

        public String getIdentifyingCharacter() {
            return identifyingCharacter;
        }

        public static MarcRecordType getByChar(String c) {
            for (MarcRecordType type : MarcRecordType.values()) {
                if (type.identifyingCharacter.equals(c)) {
                    return type;
                }
            }
            return LANGUAGEMATERIAL;
        }
    }

    public enum MarcBibliographicLevel {

        MONOGRAPHICPART("Monographic component part", "a"), SERIALPART("Serial component part", "b"), COLLECTION("Collection", "c"), SUBUNIT(
                "Subunit", "d"), INTEGRATINGRESOURCE("Integrating resource", "i"), MONOGRAPH("Monograph/Item", "m"), SERIAL("Serial", "s");

        private String label, identifyingCharacter;

        private MarcBibliographicLevel(String label, String identifyingCharacter) {
            this.label = label;
            this.identifyingCharacter = identifyingCharacter;
        }

        public String getLabel() {
            return label;
        }

        public String getIdentifyingCharacter() {
            return identifyingCharacter;
        }

        public static MarcBibliographicLevel getByChar(String c) {
            for (MarcBibliographicLevel type : MarcBibliographicLevel.values()) {
                if (type.identifyingCharacter.equals(c)) {
                    return type;
                }
            }
            return MONOGRAPH;
        }
    }

    public enum MarcMultipartLevel {

        NAN("Not specified or not applicable", "0"), SET("Set", "a"), INDEPENDENTPART("Part with independent title", "b"), DEPENDENTPART(
                "Part with dependent title", "c");

        private String label, identifyingCharacter;

        private MarcMultipartLevel(String label, String identifyingCharacter) {
            this.label = label;
            this.identifyingCharacter = identifyingCharacter;
        }

        public String getLabel() {
            return label;
        }

        public String getIdentifyingCharacter() {
            return identifyingCharacter;
        }

        public static MarcMultipartLevel getByChar(String c) {
            for (MarcMultipartLevel type : MarcMultipartLevel.values()) {
                if (type.identifyingCharacter.equals(c)) {
                    return type;
                }
            }
            return NAN;
        }
    }

    protected static final Logger LOGGER = Logger.getLogger(Marc21Parser.class);
    private static final Namespace NS_MARC = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");

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
    //    private List<String> anchorMetadataList = new ArrayList<String>();
    private String separator;
    private RecordInformation info;
    private String docType = null;
    private String individualIdentifier = null;
    private boolean treatAsPeriodical = false;

    public Marc21Parser(Prefs prefs, File mapFile) throws ParserException {
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
		if(dsLogical.hasMetadataType(prefs.getMetadataTypeByName("CurrentNo")) && !dsLogical.hasMetadataType(prefs.getMetadataTypeByName("CurrentNoSorting"))) {
			try {
				Metadata md = new Metadata(prefs.getMetadataTypeByName("CurrentNoSorting"));
				md.setValue(dsLogical.getAllMetadataByType(prefs.getMetadataTypeByName("CurrentNo")).get(0).getValue());
				dsLogical.addMetadata(md);
			} catch (MetadataTypeNotAllowedException e) {
				LOGGER.error("Cannot add CurrentNoSorting: Not allowed for ds " + dsLogical.getType().getName());
			}
			
		}
		
	}

	private DigitalDocument generateDD() throws ParserException {
        DigitalDocument dd = new DigitalDocument();
        info = getRecordInfo(this.marcDoc);
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
            if(docType == null && dsLogical.getType().isAnchor()) {
            	//This is the main entry, but an anchor
            	dsAnchor = dsLogical;
            	dsLogical = dd.createDocStruct(prefs.getDocStrctTypeByName(info.getChildDs()));
            	dsAnchor.addChild(dsLogical);
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
                //              03315    a2200733   450 
                String typeString = leaderStr.substring(6, 7);
                String bibLevelString = leaderStr.substring(7, 8);
                String partLevelString = leaderStr.trim().substring(leaderStr.trim().length() - 1);
                String dateString = "";
                Element controlField005 = getControlfield(marcDoc, "005");
                if (controlField005 != null) {
                    dateString = controlField005.getText().substring(0, 8);
                }
                RecordInformation info =
                        new RecordInformation(MarcRecordType.getByChar(typeString), MarcBibliographicLevel.getByChar(bibLevelString),
                                MarcMultipartLevel.getByChar(partLevelString), dateString);

                //set docStruct type from field 959
                DocStructType logStructType = getDocTypeFrom959(marcDoc);
                info.setDocStruct(logStructType);

                return info;
            }
        }
        return null;
    }

    private DocStructType getDocTypeFrom959(Document marcDoc) {
        if (this.docType != null) {
            return prefs.getDocStrctTypeByName(getDocStructType(this.docType));
        } else {
            XPath xpath;
            String query = "/marc:record/marc:datafield[@tag=\"959\"]/marc:subfield[@code=\"a\"]";
            try {
                xpath = XPath.newInstance(query);
                if (NS_MARC != null) {
                    xpath.addNamespace(NS_MARC);
                }
                @SuppressWarnings("unchecked")
                List<Element> nodeList = xpath.selectNodes(marcDoc);
                if (nodeList != null && !nodeList.isEmpty() && nodeList.get(0) instanceof Element) {
                    Element node = nodeList.get(0);
                    String typeName = node.getValue();
                    DocStructType docStruct = prefs.getDocStrctTypeByName(getDocStructType(typeName));
                    return docStruct;
                } else {
                    throw new JDOMException("No datafield 959 with subfield a found in marc document");
                }
            } catch (JDOMException e) {
                LOGGER.error("Unable to retrieve document type information from datafield 959", e);
            }
            return null;
        }
    }

    public String getAchorID() {
        XPath xpath;
        String query = "/marc:record/marc:datafield[@tag=\"453\"]/marc:subfield[@code=\"a\"]";
        try {
            xpath = XPath.newInstance(query);
            if (NS_MARC != null) {
                xpath.addNamespace(NS_MARC);
            }
            @SuppressWarnings("unchecked")
            List<Element> nodeList = xpath.selectNodes(marcDoc);
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

    private String getDocStructType(String typeName) {
        if ("Monographie".equals(typeName)) {
            return "Monograph";
        } else if ("Stücktitel".equals(typeName) || "Stuecktitel".equals(typeName)) {
            return "Volume";
        } else if ("Bandaufführung".equals(typeName) || "Bandauffuehrung".equals(typeName)) {
            return "MultiVolumeWork";
        } else if ("Zeitschrift".equals(typeName)) {
            return "Periodical";
        }
        return typeName;
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
        MetadataType mdType = prefs.getMetadataTypeByName(mdTypeName);
        if(mdType == null) {
        	LOGGER.error("Unable To create metadata type " + mdTypeName);
        }
        if (mdType.getIsPerson()) {
            writePersonXPaths(getXPaths(metadataElement), mdType);
        } else {
            writeMetadataXPaths(getXPaths(metadataElement), mdType, !separateXPaths(metadataElement));
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
        if(treatAsPeriodical) {
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
       if(treatAsPeriodical) {
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
                XPath xpath;
                xpath = XPath.newInstance(query);
                if (NS_MARC != null) {
                    xpath.addNamespace(NS_MARC);
                }
                @SuppressWarnings("rawtypes")
                List nodeList = xpath.selectNodes(marcDoc);

                //read values
                if (nodeList != null && !nodeList.isEmpty()) {
                    List<String> nodeValueList = getMetadataNodeValues(nodeList, subfields, mdType);
                    if (mergeOccurances) {
                        StringBuilder sb = new StringBuilder();
                        for (String string : nodeValueList) {
                            sb.append(string);
                            sb.append(separator);
                        }
                        valueList.add(sb.substring(0, sb.length() - separator.length()));
                    } else {
                        if (mergeXPaths) {
                            int count = 0;
                            for (String value : nodeValueList) {
                                if (value != null && valueList.size() <= count) {
                                    valueList.add(value);
                                } else if (value != null) {
                                    value = valueList.get(count) + separator + value;
                                    valueList.set(count, value);
                                }
                                count++;
                            }
                        } else {
                            valueList.addAll(nodeValueList);
                        }
                    }
                }

            } catch (JDOMException e) {
                LOGGER.error("Error parsing mods section for node " + eleXpath.getTextTrim(), e);
                continue;
            }
        }

        //create and write medadata
        for (String value : valueList) {
            try {
                Metadata md = new Metadata(mdType);
                md.setValue(value);
                writeMetadata(md);

            } catch (MetadataTypeNotAllowedException e) {
                LOGGER.error("Failed to create metadata " + mdType.getName());
            }
            if(mdType.getName().equals("TitleDocMain")) {
            	writeSortingTitle(value);
            }
        }

    }

    private void writeSortingTitle(String value) {
		Pattern pattern = Pattern.compile("<<.+>>");
		Matcher matcher = pattern.matcher(value);
		String sortedValue = value;
		if(matcher.find()) {
			String artikel = matcher.group();
			sortedValue = value.replace(artikel, "").trim();
		}
		try {
			Metadata md = new Metadata(prefs.getMetadataTypeByName("TitleDocMainShort"));
			md.setValue(sortedValue);
			writeMetadata(md);
		} catch (MetadataTypeNotAllowedException e) {
			LOGGER.error("Failed to create metadata TitleDocMainShort");
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
                            //no condition: just check if node exists
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

    private void writePersonXPaths(List<Element> eleXpathList, MetadataType mdType) {

        for (Element eleXpath : eleXpathList) {
            try {
                String query = generateQuery(eleXpath);
                XPath xpath;
                xpath = XPath.newInstance(query);
                if (NS_MARC != null) {
                    xpath.addNamespace(NS_MARC);
                }
                @SuppressWarnings("unchecked")
                List<Element> nodeList = xpath.selectNodes(marcDoc);
                if (nodeList != null) {
                    writePersonNodeValues(nodeList, mdType);
                }

            } catch (JDOMException e) {
                LOGGER.error("Error parsing mods section for node " + eleXpath.getTextTrim(), e);
                continue;
            }
        }
    }

    private String generateQuery(Element eleXpath) {
        String tag = eleXpath.getAttributeValue("tag");
        String ind1 = eleXpath.getAttributeValue("ind1");
        String ind2 = eleXpath.getAttributeValue("ind2");
        String subfields = eleXpath.getAttributeValue("subfields");
        StringBuilder query = new StringBuilder("/marc:record/marc:datafield");
        if (tag != null) {
            query.append("[@tag=\"" + tag + "\"]");
        }
        if (ind1 != null) {
            query.append("[@ind1=\"" + ind1 + "\"]");
        }
        if (ind2 != null) {
            query.append("[@ind2=\"" + ind2 + "\"]");
        }
        return query.toString();
    }

    private List<Element> getXpathNodes(String query) throws JDOMException {
        XPath xpath;
        xpath = XPath.newInstance(query);
        if (NS_MARC != null) {
            xpath.addNamespace(NS_MARC);
        }
        @SuppressWarnings("unchecked")
        List<Element> nodeList = xpath.selectNodes(marcDoc);
        if(individualIdentifier != null && nodeList != null && nodeList.size() > 1 && "999".equals(nodeList.get(0).getParentElement().getAttributeValue("tag"))) {
        	return selectMatching999(nodeList);
        } else {        	
        	return nodeList;
        }
    }

    private List<Element> selectMatching999(List<Element> nodeList) {
		List<Element> newList = new ArrayList<Element>();
		for (Element element : nodeList) {
			List<Element> children  = element.getChildren();
			for (Element child : children) {
				if(individualIdentifier.equals(child.getValue().trim())) {
					newList.add(element);
					break;
				}
			}
		}
		if(newList.isEmpty()) {
			newList.add(nodeList.get(0));
		}
		return newList;
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
            String roleTerm = "";
            String typeName = mdType.getName();

            //get subelements of person
            for (Object o : node.getChildren()) {
                if (o instanceof Element) {
                    Element eleSubField = (Element) o;
                    if ("a".equals(eleSubField.getAttributeValue("code"))) {
                        //name
                        String[] name = getNameParts(eleSubField.getValue());
                        firstName = name[0];
                        lastName = name[1];
                    } else if ("b".equals(eleSubField.getAttributeValue("code"))) {
                        //numeration
                        nameNumeration = eleSubField.getValue();
                    } else if ("c".equals(eleSubField.getAttributeValue("code"))) {
                        termsOfAddress = eleSubField.getValue();
                    } else if ("d".equals(eleSubField.getAttributeValue("code"))) {
                        date = eleSubField.getValue();
                    } else if ("e".equals(eleSubField.getAttributeValue("code"))) {
                        roleTerm = eleSubField.getValue();
                    } else if ("u".equals(eleSubField.getAttributeValue("code"))) {
                        affiliation = eleSubField.getValue();
                    } else if ("0".equals(eleSubField.getAttributeValue("code"))) {
                        authorityID = eleSubField.getValue();
                    } else if ("5".equals(eleSubField.getAttributeValue("code"))) {
                        institution = eleSubField.getValue();
                    } else if ("q".equals(eleSubField.getAttributeValue("code"))) {
                        //name
                        String[] name = getNameParts(eleSubField.getValue());
                        firstName = name[0];
                        lastName = name[1];
                    } else if ("p".equals(eleSubField.getAttributeValue("code"))) {
                        //name
                        String[] name = getNameParts(eleSubField.getValue());
                        firstName = name[0];
                        lastName = name[1];
                    } else if ("9".equals(eleSubField.getAttributeValue("code"))) {
                        identifier = eleSubField.getValue();
                        ;
                    }

                }
            }

            //get displayName
            displayName = (firstName + " " + lastName + " " + nameNumeration).trim();
            if (!termsOfAddress.isEmpty()) {
                displayName += ", " + termsOfAddress;
            }

            //create and write metadata
            if (StringUtils.isNotEmpty(lastName)) {
                Person person = null;
                try {
                    person = new Person(mdType);
                    person.setFirstname(firstName);
                    person.setLastname(lastName);
                    person.setDisplayname(displayName);
                    person.setAffiliation(affiliation);
                    person.setAutorityFileID(authorityID);
                    person.setInstitution(institution);
                    person.setIdentifier(identifier);
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
            if (writeToChild) {
                try {
                    dsLogical.addMetadata(metadata);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.error("Failed to write metadata " + metadata.getType().getName() + " to logical topStruct: " + e.getMessage());
                } catch (IncompletePersonObjectException e) {
                    LOGGER.error("Failed to write metadata " + metadata.getType().getName() + " to logical topStruct: " + e.getMessage());
                }
            }
            if (writeToAnchor && dsAnchor != null) {
                //                if (dsAnchor != null && (anchorMetadataList == null || anchorMetadataList.contains(metadata.getType().getName()))) {
                try {
                    dsAnchor.addMetadata(metadata);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.warn("Failed to write metadata " + metadata.getType().getName() + " to logical anchor: " + e.getMessage());

                } catch (IncompletePersonObjectException e) {
                    LOGGER.warn("Failed to write metadata " + metadata.getType().getName() + " to logical anchor: " + e.getMessage());

                }
                //                }
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
            if (writeToChild) {
                try {
                    dsLogical.addPerson(person);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.error("Failed to write person " + person.getType().getName() + " to logical topStruct: " + e.getMessage());
                } catch (IncompletePersonObjectException e) {
                    LOGGER.error("Failed to write person " + person.getType().getName() + " to logical topStruct: " + e.getMessage());
                }
            }
            if (writeToAnchor && dsAnchor != null) {
                //                if (dsAnchor != null && (anchorMetadataList == null || anchorMetadataList.contains(person.getType().getName()))) {
                try {
                    dsAnchor.addPerson(person);
                } catch (MetadataTypeNotAllowedException e) {
                    LOGGER.warn("Failed to write person " + person.getType().getName() + " to logical anchor: " + e.getMessage());

                } catch (IncompletePersonObjectException e) {
                    LOGGER.warn("Failed to write person " + person.getType().getName() + " to logical anchor: " + e.getMessage());

                }
                //                }
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

    private List<String> getMetadataNodeValues(@SuppressWarnings("rawtypes") List nodeList, String subfields, MetadataType mdType) {

        List<String> valueList = new ArrayList<String>();

        for (Object objValue : nodeList) {
            String value = "";
            if (objValue instanceof Element) {
                Element eleValue = (Element) objValue;
                LOGGER.debug("mdType: " + mdType.getName() + "; Value: " + eleValue.getTextTrim());
                for (Element subfield : getChildElements(eleValue, "subfield")) {
                    if (subfields != null && subfields.contains(subfield.getAttributeValue("code"))) {
                        value += subfield.getValue() + separator;
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
            valueList.add(value);
        }

        return valueList;
    }

    private boolean separateXPaths(Element metadataElement) {
        if (metadataElement.getAttribute("separateXPaths") != null && metadataElement.getAttribute("separateXPaths").getValue().equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean separateOccurances(Element xpathElement) {
        if (xpathElement.getAttribute("separateOccurances") != null && xpathElement.getAttribute("separateOccurances").getValue().equals("true")) {
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

    public void setDocType(String docTypeName) {
        this.docType = docTypeName;

    }

	public String getIndividualIdentifier() {
		return individualIdentifier;
	}

	public void setIndividualIdentifier(String individualIdentifier) {
		this.individualIdentifier = individualIdentifier;
	}
    
    
}
