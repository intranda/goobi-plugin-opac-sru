package de.intranda.goobi.plugins;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.goobi.plugins.utils.SRUClient.SRUException;
import de.intranda.ugh.extension.MarcFileformat;
import de.intranda.utils.DocumentUtils;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;

public class SruOpacImportTest {

	private static final String ruleset = "resources/ruleset-ubwien.xml";
	private static final String rulesetHU = "resources/HU-monographie.xml";
	private static final String rulesetHUMarc = "resources/HU-monographie-marc.xml";
	private static final String configPath = "resources/plugin_SruOpacImport.xml";
	private static final File output = new File("output");

	private Prefs prefs;
	private ConfigOpacCatalogue catalogueFU;
	private ConfigOpacCatalogue catalogueBVB;
	private ConfigOpacCatalogue catalogueHU;
	private XMLConfiguration config;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		prefs = new Prefs();
		prefs.loadPrefs(ruleset);
		catalogueFU = new ConfigOpacCatalogue("FU-BERLIN (ALMA)", "SRU-Schnittstelle der FU-Berlin",
				"fu-berlin.alma.exlibrisgroup.com", "view/sru/49KOBV_FUB", null, 80, null, "SRU", null);
		catalogueBVB = new ConfigOpacCatalogue("BVB", "BVB", "bvbr.bib-bvb.de", "bvb01sru", null, 5661, null, "SRU", null);
		catalogueHU = new ConfigOpacCatalogue("HU-BERLIN Alma (SRU)", "HU-Berlin", "hu-berlin.alma.exlibrisgroup.com", "view/sru/49KOBV_HUB", null, 443, "utf-8",
				null, null, "SRU", "https://", null);
		config = new XMLConfiguration(new File(configPath));
		FileUtils.deleteDirectory(output);
		output.mkdir();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSearchFU() throws Exception {
		try {
			SruOpacImport importer = new SruOpacImport(config);
			Fileformat ff = importer.search("12", "BV011023566", catalogueFU, prefs);
			File outputFile = new File(output, "meta.xml");
			ff.write(outputFile.getAbsolutePath());
		} catch (SRUException e) {
			Assume.assumeFalse("Cannot perform test: " + e.getMessage(),
					"System temporarily unavailable".equals(e.getMessage()));
			fail(e.getMessage());
		}

	}

	@Test
	public void testSearchBVB() throws Exception {
		try {
			SruOpacImport importer = new SruOpacImport(config);
			Fileformat ff = importer.search("12", "BV006015701", catalogueBVB, prefs);
			File outputFile = new File(output, "meta.xml");
			ff.write(outputFile.getAbsolutePath());
		} catch (SRUException e) {
			Assume.assumeFalse("Cannot perform test: " + e.getMessage(),
					"System temporarily unavailable".equals(e.getMessage()));
			fail(e.getMessage());
		}

	}

	@Test
	public void testSearchHU() throws Exception {
		try {
			prefs.loadPrefs(rulesetHUMarc);
			SruOpacImport importer = new SruOpacImport(config);

			Fileformat ff = importer.search("12", "BV045903998", catalogueHU, prefs);
			DocumentUtils.getFileFromDocument(new File("output", "marc.xml"), importer.marcXmlDoc);
			if (importer.marcXmlDocVolume != null) {
				DocumentUtils.getFileFromDocument(new File("output", "marc-volume.xml"), importer.marcXmlDocVolume);
			}
			File outputFile = new File(output, "meta.xml");
			ff.write(outputFile.getAbsolutePath());
			assertTrue(outputFile.exists());
			
		} catch (SRUException e) {
			Assume.assumeFalse("Cannot perform test: " + e.getMessage(),
					"System temporarily unavailable".equals(e.getMessage()));
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testInit() throws Exception {
		SruOpacImport importer = new SruOpacImport(config);

	      assertEquals("other_system_number", importer.getMappedSearchField("12", catalogueHU.getTitle()));
	      assertEquals("4", importer.getMappedSearchField("4", catalogueHU.getTitle()));
		
		assertEquals("other_system_number", importer.getMappedSearchField("12", "FU-BERLIN (ALMA)"));
		assertEquals("dc.title", importer.getMappedSearchField("4", "FU-BERLIN (ALMA)"));

		assertEquals("marcxml.idn", importer.getMappedSearchField("12", "BVB"));
		assertEquals("marcxml.title", importer.getMappedSearchField("4", "BVB"));
	}

}
