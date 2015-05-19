package de.intranda.goobi.plugins;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ugh.dl.Fileformat;
import ugh.dl.Prefs;

public class SruOpacImportTest {
    
    private static final String ruleset = "resources/ruleset-ubwien.xml";
    private static final File output = new File("output");
    
    private Prefs prefs;

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
        FileUtils.deleteDirectory(output);
        output.mkdir();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSearch() throws Exception {
        SruOpacImport importer = new SruOpacImport();
        Fileformat ff = importer.search("", "", null, prefs);
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
    }

}
