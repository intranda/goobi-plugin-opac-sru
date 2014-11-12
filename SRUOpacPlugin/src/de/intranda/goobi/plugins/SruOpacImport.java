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
import java.util.List;
import java.util.StringTokenizer;

import net.xeoh.plugins.base.annotations.PluginImplementation;

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
import de.intranda.goobi.plugins.utils.Marc21Parser;
import de.intranda.goobi.plugins.utils.Marc21Parser.ParserException;
import de.intranda.goobi.plugins.utils.Marc21Parser.RecordInformation;
import de.intranda.goobi.plugins.utils.SRUClient;
import de.sub.goobi.config.ConfigurationHelper;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;

@PluginImplementation
public class SruOpacImport implements IOpacPlugin {
    private static final Logger myLogger = Logger.getLogger(SruOpacImport.class);

    private int hitcount;
    private String gattung = "Aa";
    private String atstsl;
    private ConfigOpacCatalogue coc;
    private Prefs prefs;
    public static final String MARC_MAPPING_FILE = ConfigurationHelper.getInstance().getXsltFolder() + "marc_map.xml";

    @Override
    public Fileformat search(String inSuchfeld, String inSuchbegriff, ConfigOpacCatalogue catalogue, Prefs inPrefs) throws Exception {
        return search(inSuchfeld, inSuchbegriff, catalogue, inPrefs, null);
    }

    public Fileformat search(String inSuchfeld, String inSuchbegriff, ConfigOpacCatalogue catalogue, Prefs inPrefs, RecordInformation info)
            throws Exception {
        this.coc = catalogue;
        this.prefs = inPrefs;
        Fileformat ff = new MetsMods(inPrefs);
        String recordSchema = "marcxml";
        String answer = SRUClient.querySRU(catalogue, inSuchbegriff, recordSchema);

        Document marcXmlDoc = SRUClient.retrieveMarcRecord(answer);
        if (marcXmlDoc == null) {
            answer = SRUClient.querySRU(catalogue, "rec.id=" + inSuchbegriff, recordSchema);
            marcXmlDoc = SRUClient.retrieveMarcRecord(answer);
        }
        if (marcXmlDoc != null) {
            hitcount = 1;
        } else {
            throw new Exception("Unable to find record");
        }
        File mapFile = new File(MARC_MAPPING_FILE);
        try {
            Marc21Parser parser = new Marc21Parser(inPrefs, mapFile);
            parser.setInfo(info);
            parser.setIndividualIdentifier(inSuchbegriff.trim());
            DigitalDocument dd = parser.parseMarcXml(marcXmlDoc);

            gattung = parser.getInfo().getGattung();

            String anchorId = parser.getAchorID();
            if (anchorId != null) {
                RecordInformation anchorInfo = new RecordInformation(parser.getInfo());
                Fileformat af = search(inSuchfeld, anchorId, catalogue, inPrefs, anchorInfo);
                attachToAnchor(dd, af);
            }

            createAtstsl(dd);
            ff.setDigitalDocument(dd);
            //            File tempFile = new File("/opt/digiverso/logs/aesorne.xml");
            //            ff.write(tempFile.getAbsolutePath());
        } catch (ParserException e) {
            myLogger.error(e);
        }
        return ff;
    }

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
    
    @Override
    public void setAtstsl(String createAtstsl) {
        atstsl = createAtstsl;
    }

    
    public String getGattung() {
        return gattung;
    }

}
