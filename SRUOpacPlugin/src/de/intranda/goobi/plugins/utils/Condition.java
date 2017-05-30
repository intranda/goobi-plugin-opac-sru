package de.intranda.goobi.plugins.utils;

import java.util.List;

import org.jdom2.Element;

public class Condition {
    
    private final String subfield;
    private final String match;
    
    public Condition(Element ele) {
        this.subfield = ele.getAttributeValue("subfield");
        this.match = ele.getAttributeValue("matches");
    }
    
    /**
     * @return the subfield
     */
    public String getSubfield() {
        return subfield == null ? "" : subfield;
    }
    /**
     * @return the matches
     */
    public String getMatch() {
        return match == null ? "" : match;
    }
    
    public boolean matches(Element node) {
        List<Element> subfields = node.getChildren("subfield", null);
        if(subfields != null) {
            for (Element element : subfields) {
                String code = element.getAttributeValue("code");
                String value = element.getText() == null ? "" : element.getText();
                if(getSubfield().contains(code) && value.matches(getMatch())) {
                    return true;
                }
            }
        }
        return false;
    }

}
