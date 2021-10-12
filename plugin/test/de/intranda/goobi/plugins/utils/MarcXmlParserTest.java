package de.intranda.goobi.plugins.utils;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.intranda.goobi.plugins.utils.MarcXmlParser.ParserException;
import ugh.dl.DigitalDocument;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;

public class MarcXmlParserTest {
	
	File sampleFile = new File("samples/AC14446261.xml");
	File rulesetFile = new File("resources/HU-monographie.xml");
	File mappingFile = new File("resources/marc_map.xml");
	File outputFolder = new File("output");
	
	Prefs prefs = new Prefs();

	@Before
	public void setUp() throws Exception {
		prefs.loadPrefs(rulesetFile.getAbsolutePath());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testParse() throws ParserException, JDOMException, IOException, PreferencesException, WriteException, TypeNotAllowedAsChildException, MetadataTypeNotAllowedException, DocStructHasNoTypeException {
		MarcXmlParser parser = new MarcXmlParser(prefs);
		parser.setMapFile(mappingFile);
		
		SAXBuilder builder = new SAXBuilder();
		Document marcDoc = builder.build(sampleFile);
		if(!marcDoc.getRootElement().getName().equalsIgnoreCase("record")) {
		    Element record = marcDoc.getRootElement().getChild("records", null).getChild("record", null).getChild("recordData", null).getChild(
                    "record", null);
		    marcDoc = new Document((Element) record.detach());
		}
		
		parser.setNamespace(Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim"));
		parser.setNamespace(Namespace.NO_NAMESPACE);
		DigitalDocument digDoc = parser.parseMarcXml(marcDoc, null, null);
		
		Fileformat ff = new MetsMods(prefs);
		ff.setDigitalDocument(digDoc);
		ff.write(outputFolder.getAbsolutePath() + "/" + sampleFile.getName());
	}

}
