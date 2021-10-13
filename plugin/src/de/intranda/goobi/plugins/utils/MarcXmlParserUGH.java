package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.intranda.ugh.extension.MarcFileformat;
import de.schlichtherle.io.FileOutputStream;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.fileformats.mets.MetsMods;

public class MarcXmlParserUGH extends MarcXmlParser {

    private static final Logger logger = Logger.getLogger(MarcXmlParserUGH.class);
    private static final String ANCHOR_ID_TYPE = "_anchorIdentifier";
    
    private final ConfigOpac configOpac;
    
    private String anchorId = null;
    
    public MarcXmlParserUGH(Prefs prefs) throws ParserException {
        super(prefs);
        this.configOpac = ConfigOpac.getInstance();
    }
    
    public MarcXmlParserUGH(Prefs prefs, ConfigOpac configOpac) throws ParserException {
        super(prefs);
        if(configOpac != null) {            
            this.configOpac = configOpac;
        } else {
            this.configOpac = ConfigOpac.getInstance();
        }
    }

    @Override
    public DigitalDocument parseMarcXml(Document marcDoc, DocStruct originalAnchor, DocStruct mappedDocStruct)
            throws ParserException, TypeNotAllowedAsChildException, MetadataTypeNotAllowedException, DocStructHasNoTypeException {
        this.marcDoc = marcDoc;
        DigitalDocument dd;
        this.anchorId = null;
        try {
            dd = readMarc(marcDoc, mappedDocStruct).getDigitalDocument();
            RecordInformation info = new RecordInformation(dd.getLogicalDocStruct(), this.configOpac);
            setInfo(info);
            
            String dsTypePhysical = "BoundBook";
            dsPhysical = dd.createDocStruct(prefs.getDocStrctTypeByName(dsTypePhysical));
            dd.setPhysicalDocStruct(dsPhysical);
        } catch (PreferencesException | TypeNotAllowedForParentException  e) {
            throw new ParserException(e);
        }

        if (originalAnchor != null) {
            if(originalAnchor.getType().isAnchor()) {                
                dd.getLogicalDocStruct().addChild(originalAnchor.getAllChildren().get(0));
                this.dsLogical = dd.getLogicalDocStruct().getAllChildren().get(0);
                this.dsAnchor = dd.getLogicalDocStruct();
            } else {
                this.dsLogical = originalAnchor;
                this.dsAnchor = dd.getLogicalDocStruct();
                this.dsAnchor.addChild(dsLogical);
            }
        } else {            
            this.dsLogical = dd.getLogicalDocStruct();
            if(this.dsLogical.getType().isAnchor()) {
                this.dsAnchor = this.dsLogical;
                this.dsLogical = null;
                if(this.dsAnchor.getAllChildren() != null && !this.dsAnchor.getAllChildren().isEmpty()) {                    
                    this.dsLogical = this.dsAnchor.getAllChildren().get(0);
                } else {
                    this.dsLogical = createChildForAnchor(this.dsAnchor, dd);
                    this.dsAnchor.addChild(this.dsLogical);
                }
            } else {                
                this.anchorId = parseAnchorId(this.dsLogical);
            }
        }
        

        
        addMissingMetadata(dd);
        return dd;
    }

    @Override
    public String getAchorID() {
        return this.anchorId;
    }
    
    private String parseAnchorId(DocStruct ds)  {
        if(!getMetadataValues(ds, ANCHOR_ID_TYPE).isEmpty()) {
            String anchorID = getMetadataValues(ds, ANCHOR_ID_TYPE).get(0);
            return anchorID;
        }
        return null;
    }
    
    private List<String> getMetadataValues(DocStruct ds, String metadataTypeName) {
        MetadataType type = prefs.getMetadataTypeByName(metadataTypeName);
        if(type != null) {
            return ds.getAllMetadataByType(type).stream().map(md -> md.getValue()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private DocStruct createChildForAnchor(DocStruct dsAnchor, DigitalDocument dd) {
        DocStructType anchorType = dsAnchor.getType();
        return anchorType.getAllAllowedDocStructTypes().stream().findFirst()
                .map(typeName -> this.prefs.getDocStrctTypeByName(typeName))
                .map(type -> {
                    try {
                        DocStruct ds = dd.createDocStruct(type);
                        Fileformat ffChild = readMarc(this.marcDoc, ds);
                        Metadata id = ds.getAllMetadataByType(this.prefs.getMetadataTypeByName("CatalogIDDigital")).get(0);
                        id.setValue(Long.toString(System.currentTimeMillis()));
                        return ds;
                    } catch (TypeNotAllowedForParentException  | ParserException e) {
                        logger.error(e);
                        return null;
                    }
                })
                .orElse(null);
    }

    private Fileformat readMarc(Document marcDoc, DocStruct docStruct) throws ParserException {
        try {
            MarcFileformat marc = new MarcFileformat(prefs);
            File tempFile = File.createTempFile("marc-import", "xml");
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            try(FileOutputStream fos = new FileOutputStream(tempFile)) {            
                xmlOutputter.output(marcDoc, fos);
            }
            marc.read(tempFile.getAbsolutePath(), docStruct);
            return marc;
        } catch (IOException | ReadException  e) {
            throw new ParserException(e);
        }
        
    }
    
}
