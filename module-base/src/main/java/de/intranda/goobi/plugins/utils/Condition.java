package de.intranda.goobi.plugins.utils;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

public class Condition {

    private final String subfield;
    private final String match;

    public Condition(Element ele) {
        this.subfield = ele.getAttributeValue("subfield");
        this.match = ele.getAttributeValue("matches");
    }

    public Condition(String subfield, String match) {
        this.subfield = subfield;
        this.match = match;
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
        if (StringUtils.isEmpty(getMatch())) {
            //match only if no subfield with the given code(s) exists
            for (Element element : subfields) {
                String code = element.getAttributeValue("code");
                if(getSubfield().contains(code)) {
                    return false;
                }
            }
            return true;
        } else {
            if (subfields != null) {
                for (Element element : subfields) {
                    String code = element.getAttributeValue("code");
                    String value = element.getText() == null ? "" : element.getText();
                    if (getSubfield().contains(code) && value.matches(getMatch())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
