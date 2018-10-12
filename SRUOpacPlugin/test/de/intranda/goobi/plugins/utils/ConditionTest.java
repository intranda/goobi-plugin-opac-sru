package de.intranda.goobi.plugins.utils;

import org.jdom2.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ConditionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testHasSubfield() {
       Condition condition = new Condition("x", "a");
       
       Element ele = new Element("marcfield");
       
       Assert.assertFalse(condition.matches(ele));
       
       Element subY = new Element("subfield");
       subY.setAttribute("code", "y");
       subY.setText("a");
       ele.addContent(subY);
       
       Assert.assertFalse(condition.matches(ele));
       
       Element subX = new Element("subfield");
       subX.setAttribute("code", "x");
       subX.setText("test");
       ele.addContent(subX);
       
       Assert.assertFalse(condition.matches(ele));
       
       subX.setText("a");
       
       Assert.assertTrue(condition.matches(ele));
    }
    
    
    @Test
    public void testNotHasSubfield() {
       Condition condition = new Condition("x", "");
       
       Element ele = new Element("marcfield");
       
       Assert.assertTrue(condition.matches(ele));
       
       Element subY = new Element("subfield");
       subY.setAttribute("code", "y");
       subY.setText("test");
       ele.addContent(subY);
       Assert.assertTrue(condition.matches(ele));

       
       Element subX = new Element("subfield");
       subX.setAttribute("code", "x");
       subX.setText("test");
       ele.addContent(subX);
       Assert.assertFalse(condition.matches(ele));
    }
    
}
