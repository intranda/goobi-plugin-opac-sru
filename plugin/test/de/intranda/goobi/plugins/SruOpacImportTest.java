package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import de.intranda.goobi.plugins.utils.SRUClient.SRUException;
import de.intranda.utils.DocumentUtils;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;

public class SruOpacImportTest {

    private static final String ruleset = "resources/ruleset-ubwien.xml";
    private static final String rulesetHU = "resources/HU-monographie.xml";
    private static final String rulesetHUMarc = "resources/HU-monographie-marc.xml";
    private static final String configPath = "resources/plugin_SruOpacImport.xml";
    private static final File output = new File("output");

    private static final String HU_ID_MUTLIVOLUME = "BV047352849";
    private static final String HU_ID_VOLUME = "BV047352867";
    private static final String HU_ID_MONOGRAPH = "BV046323712";
    private static final String HU_ID_MONOGRAPH_SERIES = "BV047628653";
    private static final String HU_ID_PERIODICAL = "BV041382587";
    private static final String HU_ID_SINGLESHEETMATERIAL = "BV041421924";
    private static final String HU_ID_MAPVOLUME = "BV047661105";

    private Prefs prefs;
    private ConfigOpacCatalogue catalogueFU;
    private ConfigOpacCatalogue catalogueBVB;
    private ConfigOpacCatalogue catalogueHU;
    private XMLConfiguration config;

    SruOpacImport importer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        
        ConfigurationHelper.CONFIG_FILE_NAME = "resources/goobi_config.properties";
        ConfigurationHelper.resetConfigurationFile();
        
        prefs = new Prefs();
        catalogueFU = new ConfigOpacCatalogue("FU-BERLIN (ALMA)", "SRU-Schnittstelle der FU-Berlin",
                "fu-berlin.alma.exlibrisgroup.com", "view/sru/49KOBV_FUB", null, 80, null, "SRU", null);
        catalogueBVB = new ConfigOpacCatalogue("BVB", "BVB", "bvbr.bib-bvb.de", "bvb01sru", null, 5661, null, "SRU", null);
        catalogueHU = new ConfigOpacCatalogue("HU-BERLIN Alma (SRU)", "HU-Berlin", "hu-berlin.alma.exlibrisgroup.com", "view/sru/49KOBV_HUB", null,
                443, "utf-8",
                null, null, "SRU", "https://", null);
        config = new XMLConfiguration(new File(configPath));
        FileUtils.deleteDirectory(output);
        output.mkdir();

        importer = new SruOpacImport(config);

        ConfigOpacDoctype monograph = new ConfigOpacDoctype("monograph", "Monograph", "", false, false, false, null, Arrays.asList("AA"), null);
        ConfigOpacDoctype periodical = new ConfigOpacDoctype("periodical", "Periodical", "", true, false, false, null, Arrays.asList("AA"), "PeriodicalVolume");
        ConfigOpacDoctype multiVolume = new ConfigOpacDoctype("multiVolume", "MultiVolumeWork", "", false, true, false, null, Arrays.asList("AA"), "Volume");
        ConfigOpacDoctype volumeMap = new ConfigOpacDoctype("multiVolumeMap", "MapVolume", "", false, true, false, null, Arrays.asList("Kf"), null);
        ConfigOpacDoctype singleMap = new ConfigOpacDoctype("singleMap", "SingleMap", "", false, true, false, null, Arrays.asList("Ka"), null);

        ConfigOpac configOpac = Mockito.mock(ConfigOpac.class);
        ConfigOpacDoctype configOpacDoctype = Mockito.mock(ConfigOpacDoctype.class);
        Mockito.when(configOpac.getAllDoctypes()).thenReturn(Arrays.asList(monograph, periodical, multiVolume, singleMap, volumeMap));
        Mockito.when(configOpac.getDoctypeByName(Mockito.anyString())).thenReturn(configOpacDoctype);
        Mockito.when(configOpacDoctype.getMappings()).thenReturn(Collections.singletonList("AA"));
        importer.setConfigOpac(configOpac);
        
    }

    @After
    public void tearDown() throws Exception {
    }

//    @Test
    public void testSearchFU() throws Exception {
        prefs.loadPrefs(ruleset);
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

//    @Test
    public void testSearchBVB() throws Exception {
        prefs.loadPrefs(ruleset);
        try {
            Fileformat ff = importer.search("12", "BV006015701", catalogueBVB, prefs);
            File outputFile = new File(output, "meta.xml");
            ff.write(outputFile.getAbsolutePath());
        } catch (SRUException e) {
            Assume.assumeFalse("Cannot perform test: " + e.getMessage(),
                    "System temporarily unavailable".equals(e.getMessage()));
            fail(e.getMessage());
        }

    }

//    @Test
    public void testSearchHU() throws Exception {
        try {
            prefs.loadPrefs(rulesetHUMarc);
            SruOpacImport importer = new SruOpacImport(config);

            Fileformat ff = importer.search("12", HU_ID_MONOGRAPH, catalogueHU, prefs);
            DocumentUtils.getFileFromDocument(new File("output", "marc.xml"), importer.marcXmlDoc);
            if (importer.marcXmlDocVolume != null) {
                DocumentUtils.getFileFromDocument(new File("output", "marc-volume.xml"), importer.marcXmlDocVolume);
            }
            File outputFile = new File(output, "meta.xml");
            assertEquals("Monograph", ff.getDigitalDocument().getLogicalDocStruct().getType().getName());

        } catch (SRUException e) {
            Assume.assumeFalse("Cannot perform test: " + e.getMessage(),
                    "System temporarily unavailable".equals(e.getMessage()));
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testHU_Monograph() throws Exception {

        String id = HU_ID_MONOGRAPH;

        Fileformat ff = importData(id);
        assertEquals("Monograph", ff.getDigitalDocument().getLogicalDocStruct().getType().getName());
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
        assertTrue(outputFile.exists());

    }
    
    @Test
    public void testHU_Monograph_Series() throws Exception {

        String id = HU_ID_MONOGRAPH_SERIES;

        Fileformat ff = importData(id);
        assertEquals("Monograph", ff.getDigitalDocument().getLogicalDocStruct().getType().getName());
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
        assertTrue(outputFile.exists());

    }

//    @Test
    public void testHU_Map_Volume() throws Exception {

        String id = HU_ID_MAPVOLUME;

        Fileformat ff = importData(id);
        assertEquals("MapVolume", ff.getDigitalDocument().getLogicalDocStruct().getType().getName());
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
        assertTrue(outputFile.exists());

    }

    
    @Test
    public void testHU_Volume() throws Exception {

        String id = HU_ID_VOLUME;

        Fileformat ff = importData(id);
        assertEquals("MultiVolumeWork", ff.getDigitalDocument().getLogicalDocStruct().getType().getName());
        assertEquals("Volume", ff.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0).getType().getName());
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
        assertTrue(outputFile.exists());

    }

    @Test
    public void testHU_Periodical() throws Exception {

        String id = HU_ID_PERIODICAL;

        Fileformat ff = importData(id);
        assertEquals("Periodical", ff.getDigitalDocument().getLogicalDocStruct().getType().getName());
        assertEquals("PeriodicalVolume", ff.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0).getType().getName());
        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
        assertTrue(outputFile.exists());

    }

    @Test
    public void testHU_Corporate() throws Exception {

        String id = "BV045903998";

        Fileformat ff = importData(id);
        assertNotNull(ff.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0).getAllCorporates().get(0).getMainName());

        File outputFile = new File(output, "meta.xml");
        ff.write(outputFile.getAbsolutePath());
        assertTrue(outputFile.exists());

    }

    private Fileformat importData(String id) throws PreferencesException, ImportPluginException, Exception, IOException {
        prefs.loadPrefs(rulesetHU);

        Fileformat ff = importer.search("12", id, catalogueHU, prefs);
        DocumentUtils.getFileFromDocument(new File("output", "marc.xml"), importer.marcXmlDoc);
        if (importer.marcXmlDocVolume != null) {
            DocumentUtils.getFileFromDocument(new File("output", "marc-volume.xml"), importer.marcXmlDocVolume);
        }
        return ff;
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


    @Test
    public void findMappedValue() throws PreferencesException, JDOMException, IOException {
        prefs.loadPrefs(rulesetHUMarc);
        
        SAXBuilder builder = new SAXBuilder(); 
        Document origDoc = builder.build(new File("samples/BV045903998.xml"));
        Element record = origDoc.getRootElement()
                .getChild("records", null)
                .getChild("record", null)
                .getChild("recordData", null)
                .getChild("record", null);
        Document doc = new Document(record.clone());
        String ds = importer.getMappedDocStructType(Collections.singletonMap("//marc:datafield[@tag='650']/marc:subfield[@code='a'][text()='Vorlesungsverzeichnis.']", "SingleSheetMaterial"),
                doc, Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim"));
        assertEquals("SingleSheetMaterial", ds);
    }
    
    //@Test
    public void findMappedValue2() throws PreferencesException, JDOMException, IOException {
        prefs.loadPrefs(rulesetHUMarc);
        
        SAXBuilder builder = new SAXBuilder(); 
        Document origDoc = builder.build(new URL("https://obv-at-ubww.alma.exlibrisgroup.com/view/sru/43ACC_WUW?version=1.2&operation=searchRetrieve&query=alma.local_control_field_009=AC16876952&maximumRecords=5&recordSchema=marcxml"));
        Element record = origDoc.getRootElement()
                .getChild("records", null)
                .getChild("record", null)
                .getChild("recordData", null)
                .getChild("record", null);
        Document doc = new Document(record.clone());
        String ds = importer.getMappedDocStructType(Collections.singletonMap("//marc:controlfield[@tag='008'][substring(text(),4,1) = '6']", "SingleSheetMaterial"),
                doc, Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim"));
        assertEquals("SingleSheetMaterial", ds);
    }
    
}
