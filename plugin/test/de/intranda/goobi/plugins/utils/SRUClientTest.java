package de.intranda.goobi.plugins.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.goobi.plugins.utils.SRUClient;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;

public class SRUClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testQuerySru() {
        try {
            ConfigOpacCatalogue cat =
                    new ConfigOpacCatalogue("test", "none", "hu-berlin.alma.exlibrisgroup.com", "view/sru/49KOBV_HUB", null, 80, "utf-8", null, null, "SRU", "http://", null);
            String query = "BV040552415";
//            String query = "BV041701500";
            String recordSchema = "marcxml";
            SRUClient client = new SRUClient();
            client.setSruVersion("1.2");
            String ret = client.querySRU(cat, query, recordSchema);
            System.out.println(ret);
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
