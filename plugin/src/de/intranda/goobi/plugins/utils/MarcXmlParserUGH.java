package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.intranda.ugh.extension.MarcFileformat;
import de.schlichtherle.io.FileOutputStream;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;

public class MarcXmlParserUGH extends MarcXmlParser {

    public MarcXmlParserUGH(Prefs prefs) throws ParserException {
        super(prefs);
        // TODO Auto-generated constructor stub
    }

    @Override
    public DigitalDocument parseMarcXml(Document marcDoc, DocStruct originalAnchor)
            throws ParserException, TypeNotAllowedAsChildException, MetadataTypeNotAllowedException, DocStructHasNoTypeException {
        this.marcDoc = marcDoc;
        DigitalDocument dd = generateDD();
        
        
        
        if (originalAnchor != null) {
            dd.getLogicalDocStruct().addChild(originalAnchor.getAllChildren().get(0));
            this.dsLogical = dd.getLogicalDocStruct().getAllChildren().get(0);
        }
        
        this.dsLogical = readMarc(marcDoc);
        if(this.dsAnchor != null) {
            if(this.dsAnchor.getAllChildren() != null && !this.dsAnchor.getAllChildren().isEmpty()) {                
                this.dsAnchor.removeChild(this.dsAnchor.getAllChildren().get(0));
            }
            this.dsAnchor.addChild(this.dsLogical);
            dd.setLogicalDocStruct(this.dsAnchor);
        } else {
            dd.setLogicalDocStruct(this.dsLogical);
        }
        
        addMissingMetadata(dd);
        return dd;
    }

    private DocStruct readMarc(Document marcDoc) throws ParserException {
        try {
            Fileformat marc = new MarcFileformat(prefs);
            File tempFile = File.createTempFile(getInfo().getRecordIdentifier(), "xml");
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            try(FileOutputStream fos = new FileOutputStream(tempFile)) {            
                xmlOutputter.output(marcDoc, fos);
            }
            marc.read(tempFile.getAbsolutePath());
            return marc.getDigitalDocument().getLogicalDocStruct();
        } catch (IOException | ReadException | PreferencesException e) {
            throw new ParserException(e);
        }
        
    }
    
}
