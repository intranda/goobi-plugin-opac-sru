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
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IOpacPlugin;
import org.jdom2.Document;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.fileformats.mets.MetsMods;
import de.intranda.goobi.plugins.utils.MarcXmlParser;
import de.intranda.goobi.plugins.utils.MarcXmlParser.ParserException;
import de.intranda.goobi.plugins.utils.MarcXmlParser.RecordInformation;
import de.intranda.goobi.plugins.utils.MarcXmlParserHU;
import de.intranda.goobi.plugins.utils.SRUClient;
import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;

@PluginImplementation
public class SruOpacImport implements IOpacPlugin {
    private static final Logger myLogger = Logger.getLogger(SruOpacImport.class);

    private XMLConfiguration config;
    private int hitcount;
    private String gattung = "Aa";
    private String atstsl;
    private ConfigOpacCatalogue coc;
    private Prefs prefs;
    private String inputEncoding;
    private File marcMappingFile = new File(ConfigurationHelper.getInstance().getXsltFolder() + "marc_map.xml");

    private Map<String, Map<String, String>> searchFieldMap;

    /**
     * Constructor using the default plugin configuration profived by Goobi
     * 
     * @throws ImportPluginException if any configuration files could not be found or read
     */
    public SruOpacImport() throws ImportPluginException {
        this.config = ConfigPlugins.getPluginConfig(this);
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
        this.inputEncoding = config.getString("charset", "utf-8");
        String mappingPath = config.getString("mapping", "marc_map.xml");
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
        initSearchFieldMap();
    }

    /**
     * Reads the search fields to use from the plugin configuration. If no fields are configured, a default configuration is used: 12 (identifier) >>
     * rec.id The search field is used in the sru query like so: query=<search field>=123345
     * 
     */
    private void initSearchFieldMap() {
        searchFieldMap = new HashMap<String, Map<String, String>>();
        SubnodeConfiguration mappings = null;
        try {
            mappings = config.configurationAt("searchFields");
        } catch (IllegalArgumentException e) {
            myLogger.warn(e.getMessage());
        }

        if (mappings == null || mappings.isEmpty()) {
            HashMap<String, String> catalogMap = new HashMap<String, String>();
            catalogMap.put(null, "rec.id");
            searchFieldMap.put("12", catalogMap);
        } else {
            List<SubnodeConfiguration> fieldConfigs = mappings.configurationsAt("field");
            for (SubnodeConfiguration fieldConfig : fieldConfigs) {
                String key = fieldConfig.getString("id");
                Map<String, String> catalogFieldMap = new LinkedHashMap<String, String>();
                int countFields = fieldConfig.getMaxIndex("searchField");
                for (int i = 0; i <= countFields; i++) {
                    String value = fieldConfig.getString("searchField(" + i + ")");
                    String catalogue = fieldConfig.getString("searchField(" + i + ")[@catalogue]");
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
        inSuchfeld = getMappedSearchField(inSuchfeld, catalogue.getTitle());
        return search(inSuchfeld, inSuchbegriff, catalogue, inPrefs, null);
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
    public Fileformat search(String inSuchfeld, String inSuchbegriff, ConfigOpacCatalogue catalogue, Prefs inPrefs, RecordInformation info)
            throws Exception {
        this.coc = catalogue;
        this.prefs = inPrefs;
      
        //create a new empty fileformat
        Fileformat ff = new MetsMods(inPrefs);
        
        //query the catalogue, first without using a search field. recordSchema is always marcxml
        String recordSchema = "marcxml";
        String answer = SRUClient.querySRU(catalogue, inSuchbegriff, recordSchema);
        try(FileOutputStream fos = new FileOutputStream(new File("output/marc.xml"))) {            
            IOUtils.write(answer, fos);
            System.out.println("Writen file " + new File("output/marc.xml").getAbsolutePath());
        }
        //retrieve the marcXml document from the answer
        Document marcXmlDoc = SRUClient.retrieveMarcRecord(answer);
        //If no record was found, search again using the search field
        if (marcXmlDoc == null) {
            answer = SRUClient.querySRU(catalogue, inSuchfeld + "=" + inSuchbegriff, recordSchema);
            marcXmlDoc = SRUClient.retrieveMarcRecord(answer);

        }
        
        //throw exception if not exactly one record was found
        if (marcXmlDoc != null) {
            hitcount = 1;
        } else {
            throw new Exception("Unable to find record");
        }
        
        //set up a default MarcXmlParser.
        MarcXmlParser parser = new MarcXmlParserHU(inPrefs, marcMappingFile);
        parser.setInfo(info);   //Pass record type if this is an anchor
        parser.setIndividualIdentifier(inSuchbegriff.trim());   //not used
        //parse the marcXml record
        DigitalDocument dd = parser.parseMarcXml(marcXmlDoc);
        //Set the gattung from the parsed result. Used to assign a Document type for the new Goobi process
        gattung = parser.getInfo().getGattung();

        //If the record contains a reference to an anchor, retrieve the anchor record
        String anchorId = parser.getAchorID();
        if (anchorId != null) {
            RecordInformation anchorInfo = new RecordInformation(parser.getInfo());
            Fileformat af = search(inSuchfeld, anchorId, catalogue, inPrefs, anchorInfo);
            attachToAnchor(dd, af);
        }

        createAtstsl(dd);
        ff.setDigitalDocument(dd);
        
        return ff;
    }

    /**
     * Attaches the digital document to the fileformat containing the anchor record
     * 
     * @param dd    The digital document containing the volume record
     * @param anchorFormat  the fileformat containing the anchor record
     * @throws PreferencesException     If the anchor digital document could not be retireved from the fileformat
     * @throws TypeNotAllowedAsChildException   If the record doctype is not allowed as child of the anchor doctype
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

    @Override
    public String getDescription() {
        return "SRU";
    }

    /**
     * Gets the DocType for the record from the gattung-term
     * 
     */
    @Override
    public ConfigOpacDoctype getOpacDocType() {
        try {
            ConfigOpac co = new ConfigOpac();
            ConfigOpacDoctype cod = co.getDoctypeByMapping(this.gattung.substring(0, 2), this.coc.getTitle());
            if (cod == null) {

                cod = new ConfigOpac().getAllDoctypes().get(0);
                this.gattung = cod.getMappings().get(0);

            }
            return cod;
        } catch (IOException e) {
            myLogger.error("OpacDoctype unknown", e);

            return null;
        }
    }

    @Override
    public void setAtstsl(String createAtstsl) {
        atstsl = createAtstsl;
    }

    public String getGattung() {
        return gattung;
    }

}
