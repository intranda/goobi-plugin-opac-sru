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
                    new ConfigOpacCatalogue("test", "none", "aleph20.ub.hu-berlin.de", "hub01", null, 5661, "utf-8", null, null, "SRU", "http://");

            String query = "BV040552415";
            String recordSchema = "marcxml";
            String ret = SRUClient.querySRU(cat, query, recordSchema);
            System.out.println(ret);
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
