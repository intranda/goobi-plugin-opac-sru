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

package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IOpacPluginVersion2;
import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.intranda.goobi.plugins.utils.MarcXmlParser;
import de.intranda.goobi.plugins.utils.MarcXmlParser.RecordInformation;
import de.intranda.goobi.plugins.utils.MarcXmlParserFU;
import de.intranda.goobi.plugins.utils.MarcXmlParserHU;
import de.intranda.goobi.plugins.utils.SRUClient;
import de.intranda.goobi.plugins.utils.SRUClient.SRUException;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
public class SruOpacImport implements IOpacPluginVersion2 {
    private static final Logger myLogger = Logger.getLogger(SruOpacImport.class);

    private XMLConfiguration config;
    private int hitcount;
    private String gattung = "Aa";
    private String docType = null;
    private String atstsl;
    private ConfigOpacCatalogue coc;
    private Prefs prefs;
    //    private String inputEncoding;
    protected Document marcXmlDoc;
    protected Document marcXmlDocVolume;
    //    private File marcMappingFile = new File(ConfigurationHelper.getInstance().getXsltFolder() + "marc_map.xml");
    //    private String marcXmlParserType = null;

    private Map<String, Map<String, String>> searchFieldMap;

    private DocStruct originalAnchor = null;

    private boolean saveOriginalMetadata = false;
    private String originalMetadataFolder;

    private List<Path> pathToMarcRecord = new ArrayList<>();

    /**
     * Constructor using the default plugin configuration profived by Goobi
     *
     * @throws ImportPluginException if any configuration files could not be found or read
     */
    public SruOpacImport() throws ImportPluginException {
        this.config = ConfigPlugins.getPluginConfig("SruOpacImport");
        init();
    }

    /**
     * Constructor for using a custom plugin configuration
     *
     * @param config the config to use
     * @throws ImportPluginException if any configuration files could not be found or read
     */
    public SruOpacImport(XMLConfiguration config) throws ImportPluginException {
        this.config = config;
        init();
    }

    /**
     * Reads the plugin configuration
     *
     * @throws ImportPluginException no metadata mapping file could be found
     */
    private void init() throws ImportPluginException {
        //        this.inputEncoding = config.getString("charset", "utf-8");
        //        this.marcXmlParserType = config.getString("marcXmlParserType", "");
        this.config.setExpressionEngine(new XPathExpressionEngine());
        initSearchFieldMap();

        saveOriginalMetadata = config.getBoolean("storeMetadata", false);
        if (saveOriginalMetadata) {
            originalMetadataFolder = config.getString("metadataFolder");
        }

    }

    /**
     * Reads the search fields to use from the plugin configuration. If no fields are configured, a default configuration is used: 12 (identifier) >>
     * rec.id The search field is used in the sru query like so: query=<search field>=123345
     *
     */
    private void initSearchFieldMap() {
        searchFieldMap = new HashMap<>();
        SubnodeConfiguration mappings = null;
        try {
            mappings = config.configurationAt("searchFields");
        } catch (IllegalArgumentException e) {
            myLogger.warn(e.getMessage());
        }

        if (mappings == null || mappings.isEmpty()) {
            HashMap<String, String> catalogMap = new HashMap<>();
            catalogMap.put(null, "rec.id");
            searchFieldMap.put("12", catalogMap);
        } else {
            List<HierarchicalConfiguration> fieldConfigs = mappings.configurationsAt("field");
            for (HierarchicalConfiguration fieldConfig : fieldConfigs) {
                String key = fieldConfig.getString("id");
                Map<String, String> catalogFieldMap = new LinkedHashMap<>();
                List<HierarchicalConfiguration> searchFields = fieldConfig.configurationsAt("searchField");
                for (HierarchicalConfiguration searchFieldConfig : searchFields) {
                    String value = searchFieldConfig.getString("");
                    String catalogue = searchFieldConfig.getString("@catalogue");
                    catalogFieldMap.put(catalogue.replaceAll("\\s", "").toLowerCase(), value);
                }
                searchFieldMap.put(key, catalogFieldMap);
            }
        }

    }

    /**
     *
     * get the search field to use for this query
     *
     * @param fieldCode the field code seleced in goobi (e.g. 12=identifier, 4=title)
     * @param catalogue the catalog title used. May be null, in which case the first configured search field is used
     * @return the query search field
     * @throws IllegalStateException if the map has not previously been initialized with @initSearchFieldMap
     */
    protected String getMappedSearchField(String fieldCode, String catalogue) throws IllegalStateException {
        if (searchFieldMap == null) {
            throw new IllegalStateException("Field mappings must be loaded before evaluating request");
        }
        Map<String, String> catalogueFieldMap = searchFieldMap.get(fieldCode);
        String fieldName = catalogueFieldMap.values().iterator().next();
        if (catalogue != null) {
            fieldName = catalogueFieldMap.get(catalogue.replaceAll("\\s", "").toLowerCase());
        }
        if (fieldName != null) {
            return fieldName;
        } else {
            return fieldCode;
        }
    }

    /**
     * Called from Goobi. Gets the search field to use from configuration, performs the query and if possible creates a new Goobi fileformat
     * representing the result
     *
     */
    @Override
    public Fileformat search(String inSuchfeld, String inSuchbegriff, ConfigOpacCatalogue catalogue, Prefs inPrefs) throws Exception {
        //        initSearchFieldMap();
        pathToMarcRecord.clear();
        inSuchfeld = getMappedSearchField(inSuchfeld, catalogue.getTitle());
        String marcParserType = getConfigString("marcXmlParserType", catalogue.getTitle(), null, "");
        MarcXmlParser parser;
        if ("HU".equalsIgnoreCase(marcParserType)) {
            parser = new MarcXmlParserHU(inPrefs);
        } else if ("FU".equalsIgnoreCase(marcParserType)) {
            parser = new MarcXmlParserFU(inPrefs);
        } else {
            parser = new MarcXmlParser(inPrefs) {

                @Override
                protected String getDocType(Document doc) {
                    return null;
                }

                @Override
                protected String createCurrentNoSort(String value) {
                    value = value.replaceAll("\\D", "");
                    return value;
                }
            };
        }

        return search(inSuchfeld, inSuchbegriff, catalogue, inPrefs, parser, null);
    }

    private File initMappingFile(ConfigOpacCatalogue catalogue, Document marcDoc, Namespace namespace) throws ImportPluginException {
        //    	String mappingPath = getConfigString("mapping", catalogue.getTitle(), null, "marc_map");

        List<HierarchicalConfiguration> configs = getConfigs("mapping", catalogue.getTitle(), null);
        String mappingPath = null;
        for (HierarchicalConfiguration mappingConfig : configs) {
            mappingConfig.setExpressionEngine(new XPathExpressionEngine());
            String catalogType = mappingConfig.getString("@type", "");
            if (StringUtils.isNotBlank(catalogType)) {
                String query = mappingConfig.getString("@typeXPath", "");
                if (StringUtils.isBlank(query)) {
                    throw new ImportPluginException("All mapping configs with a type must also have a 'typeXPath' attribute");
                }
                String foundType = getCatalogType(marcDoc, query, namespace);
                if (catalogType.equalsIgnoreCase(foundType)) {
                    mappingPath = mappingConfig.getString(".");
                    break;
                }
            } else {
                mappingPath = mappingConfig.getString("");
            }
        }

        File marcMappingFile;
        if (mappingPath == null) {
            throw new ImportPluginException("No mapping file configured in configuration file " + config.getFileName());
        } else if (mappingPath.startsWith("/")) {
            marcMappingFile = new File(mappingPath);
        } else {
            marcMappingFile = new File(ConfigurationHelper.getInstance().getXsltFolder(), mappingPath);
        }
        if (!marcMappingFile.isFile()) {
            throw new ImportPluginException("Cannot locate mods mapping file " + marcMappingFile.getAbsolutePath());
        }
        //        initSearchFieldMap();
        return marcMappingFile;
    }

    private List<HierarchicalConfiguration> getConfigs(String query, String catalogue, String subQuery) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(query).append("[@catalogue='").append(catalogue).append("']");
        if (StringUtils.isNotBlank(subQuery)) {
            queryBuilder.append("/").append(subQuery);
        }

        StringBuilder defaultQueryBuilder = new StringBuilder();
        defaultQueryBuilder.append(query).append("[not(@catalogue)]");
        if (StringUtils.isNotBlank(subQuery)) {
            defaultQueryBuilder.append("/").append(subQuery);
        }

        List<HierarchicalConfiguration> configs = config.configurationsAt(queryBuilder.toString());
        if (configs == null || configs.isEmpty()) {
            configs = config.configurationsAt(defaultQueryBuilder.toString());
        }
        return configs == null ? Collections.EMPTY_LIST : configs;
    }

    private String getConfigString(String query, String catalogue, String subQuery, String defaultValue) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(query).append("[@catalogue='").append(catalogue).append("']");
        if (StringUtils.isNotBlank(subQuery)) {
            queryBuilder.append("/").append(subQuery);
        }

        StringBuilder defaultQueryBuilder = new StringBuilder();
        defaultQueryBuilder.append(query).append("[not(@catalogue)]");
        if (StringUtils.isNotBlank(subQuery)) {
            defaultQueryBuilder.append("/").append(subQuery);
        }

        return config.getString(queryBuilder.toString(), config.getString(defaultQueryBuilder.toString(), defaultValue));
    }

    /**
     * Performs the sru search and creates a Goobi fileformat representing the result Also used internally to create an anchor fileformat for a part
     * of a work
     *
     * @param inSuchfeld The field to search
     * @param inSuchbegriff The search term
     * @param catalogue The catalogue to use
     * @param inPrefs The Goobi ruleset to use in order to create the fileformat
     * @param info Only used when retrieving anchor info. Contains information about the type of resource used
     * @return The query result as Goobi fileformat
     * @throws Exception If no unique query result could be found and parsed successfully
     */
    public Fileformat search(String inSuchfeld, String inSuchbegriff, ConfigOpacCatalogue catalogue, Prefs inPrefs, MarcXmlParser parser,
            RecordInformation info) throws Exception {
        this.coc = catalogue;
        this.prefs = inPrefs;

        SRUClient client = new SRUClient();
        String version = this.config.getString("sru[@catalogue='" + catalogue.getTitle() + "']/version",
                this.config.getString("sru[not(@catalogue)]/version", client.getSruVersion()));
        client.setSruVersion(version);

        //create a new empty fileformat
        Fileformat ff = new MetsMods(inPrefs);

        //query the catalogue, first without using a search field. recordSchema is always marcxml
        String recordSchema = "marcxml";
        String answer = client.querySRU(catalogue, inSuchfeld + "=" + inSuchbegriff, recordSchema);
        //retrieve the marcXml document from the answer
        try {
            marcXmlDoc = SRUClient.retrieveMarcRecord(answer);
        } catch (SRUException e) {
            //If no record was found, search again using the search field
            answer = client.querySRU(catalogue, inSuchbegriff, recordSchema);
            marcXmlDoc = SRUClient.retrieveMarcRecord(answer);

        }

        //throw exception if not exactly one record was found
        if (marcXmlDoc != null) {
            hitcount = 1;
        } else {
            throw new Exception("Unable to find record");
        }
        if (saveOriginalMetadata) {
            // save marcXmlDoc into file, overwrite existing
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());

            Path destination = Paths.get(originalMetadataFolder, inSuchbegriff.replaceAll("\\W", "") + "_marc.xml");
            xmlOutput.output(marcXmlDoc, new FileWriter(destination.toString()));
            if (!pathToMarcRecord.contains(destination)) {
                pathToMarcRecord.add(destination);
            }
        }

        String prefix = this.config.getString("namespace[@catalogue='" + catalogue.getTitle() + "']/prefix",
                this.config.getString("namespace[not(@catalogue)]/prefix", MarcXmlParser.NS_DEFAULT.getPrefix()));
        String uri = this.config.getString("namespace[@catalogue='" + catalogue.getTitle() + "']/uri",
                this.config.getString("namespace[not(@catalogue)]/uri", MarcXmlParser.NS_DEFAULT.getURI()));
        parser.setNamespace(prefix, uri);
        parser.setInfo(info); //Pass record type if this is an anchor
        parser.setIndividualIdentifier(inSuchbegriff.trim()); //not used
        File marcMappingFile = initMappingFile(catalogue, marcXmlDoc, parser.getNamespace());
        myLogger.debug("Using mapping file " + marcMappingFile);
        parser.setMapFile(marcMappingFile);

        //parse the marcXml record
        DigitalDocument dd = parser.parseMarcXml(marcXmlDoc, this.originalAnchor);
        //Set the gattung from the parsed result. Used to assign a Document type for the new Goobi process
        gattung = parser.getInfo().getGattung();
        docType = parser.getInfo().getDocStructType();

        //If the record contains a reference to an anchor, retrieve the anchor record
        String anchorId = parser.getAchorID();
        if (!parser.isTreatAsPeriodical() && anchorId == null && info == null) {
            if (dd.getLogicalDocStruct().getType().isAnchor()) {
                MetadataType catalogId = prefs.getMetadataTypeByName("CatalogIDDigital");
                if (catalogId != null) {
                    List<? extends Metadata> mds = dd.getLogicalDocStruct().getAllMetadataByType(catalogId);
                    if (!mds.isEmpty()) {
                        anchorId = mds.get(0).getValue();
                    }
                }
            }
        }
        if (anchorId != null) {
            RecordInformation anchorInfo = new RecordInformation(parser.getInfo());
            this.marcXmlDocVolume = this.marcXmlDoc;
            this.originalAnchor = dd.getLogicalDocStruct();
            try {
                ff = search(inSuchfeld, anchorId, catalogue, inPrefs, parser, anchorInfo);
                dd = ff.getDigitalDocument();
                //            attachToAnchor(dd, af);
            } catch (SRUException e) {
                myLogger.warn("No anchor entry found for identifier " + anchorId);
            }
        }

        createAtstsl(dd);
        ff.setDigitalDocument(dd);

        return ff;
    }

    /**
     * @return the catalog type found under the given query. Or null if the query yields no results
     */
    public String getCatalogType(Document doc, String query, Namespace namespace) {
        query = getStringQuery(query, namespace);
        XPathExpression<String> xpath = createXPath(query, namespace);
        String catalogType = xpath.evaluateFirst(marcXmlDoc);
        return catalogType;
    }

    /**
     * @param query
     * @param namespace
     * @return
     */
    public XPathExpression<String> createXPath(String query, Namespace namespace) {
        XPathExpression<String> xpath;
        if (namespace != null) {
            xpath = XPathFactory.instance().compile(query, Filters.fstring(), null, namespace);
        } else {
            xpath = XPathFactory.instance().compile(query, Filters.fstring(), null);
        }
        return xpath;
    }

    /**
     * @param query
     * @param namespace
     */
    public String getStringQuery(String query, Namespace namespace) {
        if (namespace != null && StringUtils.isNotBlank(namespace.getPrefix())) {
            query = query.replaceAll("\\/(\\w)", "/" + namespace.getPrefix() + ":" + "$1");
        }
        if (query.endsWith("/")) {
            query = query.substring(0, query.length() - 1);
        }
        query = "string(" + query + ")";
        return query;
    }

    /**
     * Attaches the digital document to the fileformat containing the anchor record
     *
     * @param dd The digital document containing the volume record
     * @param anchorFormat the fileformat containing the anchor record
     * @throws PreferencesException If the anchor digital document could not be retireved from the fileformat
     * @throws TypeNotAllowedAsChildException If the record doctype is not allowed as child of the anchor doctype
     */
    private void attachToAnchor(DigitalDocument dd, Fileformat anchorFormat) throws PreferencesException, TypeNotAllowedAsChildException {
        if (anchorFormat != null && anchorFormat.getDigitalDocument().getLogicalDocStruct().getType().isAnchor()) {
            myLogger.info("Retrieved anchor record ");
            DocStruct topStruct = dd.getLogicalDocStruct();
            if (topStruct.getType().isAnchor()) {
                topStruct = topStruct.getAllChildren().get(0);
            }
            DocStruct anchor = anchorFormat.getDigitalDocument().getLogicalDocStruct();
            anchor.addChild(topStruct);
            dd.setLogicalDocStruct(anchor);
        } else {
            myLogger.error("Failed to retrieve anchor record ");
        }
    }

    /**
     * Create a shorthand string from title and author of the record to create an identifier string
     *
     * @param dd
     */
    private void createAtstsl(DigitalDocument dd) {
        DocStruct logStruct = dd.getLogicalDocStruct();
        if (logStruct.getType().isAnchor() && logStruct.getAllChildren() != null && !logStruct.getAllChildren().isEmpty()) {
            logStruct = logStruct.getAllChildren().get(0);
        }

        String author = "";
        String title = "";

        List<? extends Metadata> authorList = logStruct.getAllMetadataByType(prefs.getMetadataTypeByName("Author"));
        if (authorList != null && !authorList.isEmpty()) {
            author = ((Person) authorList.get(0)).getLastname();
        }
        List<? extends Metadata> titleShortList = logStruct.getAllMetadataByType(prefs.getMetadataTypeByName("TitleDocMainShort"));
        if (titleShortList != null && !titleShortList.isEmpty()) {
            title = titleShortList.get(0).getValue();
        } else {
            List<? extends Metadata> titleList = logStruct.getAllMetadataByType(prefs.getMetadataTypeByName("TitleDocMain"));
            if (titleList != null && !titleList.isEmpty()) {
                title = titleList.get(0).getValue();
            }
        }
        this.atstsl = createAtstsl(title, author).toLowerCase();
    }

    /**
     * Create a shorthand string from the provided title and author to create an identifier string
     *
     */
    @Override
    public String createAtstsl(String myTitle, String autor) {
        String myAtsTsl = "";
        myTitle = Normalizer.normalize(myTitle, Form.NFC);
        autor = Normalizer.normalize(autor, Form.NFC);
        if (autor != null && !autor.equals("")) {
            /* autor */
            if (autor.length() > 4) {
                myAtsTsl = autor.substring(0, 4);
            } else {
                myAtsTsl = autor;
                /* titel */
            }

            if (myTitle.length() > 4) {
                myAtsTsl += myTitle.substring(0, 4);
            } else {
                myAtsTsl += myTitle;
            }
        }

        /*
         * -------------------------------- bei Zeitschriften Tsl berechnen --------------------------------
         */
        // if (gattung.startsWith("ab") || gattung.startsWith("ob")) {
        if (autor == null || autor.equals("")) {
            myAtsTsl = "";
            StringTokenizer tokenizer = new StringTokenizer(myTitle);
            int counter = 1;
            while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                if (counter == 1) {
                    if (tok.length() > 4) {
                        myAtsTsl += tok.substring(0, 4);
                    } else {
                        myAtsTsl += tok;
                    }
                }
                if (counter == 2 || counter == 3) {
                    if (tok.length() > 2) {
                        myAtsTsl += tok.substring(0, 2);
                    } else {
                        myAtsTsl += tok;
                    }
                }
                if (counter == 4) {
                    if (tok.length() > 1) {
                        myAtsTsl += tok.substring(0, 1);
                    } else {
                        myAtsTsl += tok;
                    }
                }
                counter++;
            }
        }
        /* im ATS-TSL die Umlaute ersetzen */

        myAtsTsl = myAtsTsl.replaceAll("[\\W]", "");
        return myAtsTsl;
    }

    /* (non-Javadoc)
     * @see de.sub.goobi.Import.IOpac#getHitcount()
     */
    @Override
    public int getHitcount() {
        return this.hitcount;
    }

    /* (non-Javadoc)
     * @see de.sub.goobi.Import.IOpac#getAtstsl()
     */
    @Override
    public String getAtstsl() {
        return this.atstsl;
    }

    @Override
    public PluginType getType() {
        return PluginType.Opac;
    }

    @Override
    public String getTitle() {
        return "SRU";
    }

    public String getDescription() {
        return "intranda_opac_sru";
    }

    /**
     * Gets the DocType for the record from the gattung-term
     *
     */
    @Override
    public ConfigOpacDoctype getOpacDocType() {
        ConfigOpac co;
        try {
            co = ConfigOpac.getInstance();
        } catch (Throwable e) {
            myLogger.error(e.getMessage(), e);
            return null;
        }
        ConfigOpacDoctype cod = null;
        if (this.gattung != null) {
            cod = co.getDoctypeByMapping(this.gattung.substring(0, 2), this.coc.getTitle());
        } else {
            cod = co.getDoctypeByName(this.docType);
            if (cod == null) {
                cod = co.getDoctypeByName(this.docType.toLowerCase());
            }
        }
        if (cod == null) {
            cod = co.getAllDoctypes().get(0);
            this.gattung = cod.getMappings().get(0);

        }
        return cod;
    }

    @Override
    public void setAtstsl(String createAtstsl) {
        atstsl = createAtstsl;
    }

    @Override
    public String getGattung() {
        return gattung;
    }

    @Override
    public Map<String, String> getRawDataAsString() {
        return null;
    }

    @Override
    public List<Path> getRecordPathList() {
        return pathToMarcRecord;
    }

}
