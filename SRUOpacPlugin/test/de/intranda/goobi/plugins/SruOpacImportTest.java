package de.intranda.goobi.plugins;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;

public class SruOpacImportTest {
    
    private static final String ruleset = "resources/ruleset-ubwien.xml";
    private static final String configPath = "resources/plugin_SruOpacImport.xml";
    private static final File output = new File("output");
    
    private Prefs prefs;
    private ConfigOpacCatalogue catalogue;
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
        catalogue = new ConfigOpacCatalogue("FU-Berlin (SRU)", "SRU-Schnittstelle der FU-Berlin", "aleph-www.ub.fu-berlin.de", "fub01_usm", null, 80, "utf-8", null, null, "SRU");
        config = new XMLConfiguration(new File(configPath));
        FileUtils.deleteDirectory(output);
        output.mkdir();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSearch() throws Exception {
        SruOpacImport importer = new SruOpacImport(config);
        Fileformat ff = importer.search("12", "BV006015701", catalogue, prefs);
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
    }
    
    @Test 
    public void testInit() throws Exception {
        SruOpacImport importer = new SruOpacImport(config);
        assertEquals(importer.getMappedSearchField("12"), "dc.id");
        assertEquals(importer.getMappedSearchField("4"), "dc.title");
    }

}
