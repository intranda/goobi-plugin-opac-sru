package de.intranda.goobi.plugins.utils;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class GoobiMetadataValue {

    private String value;
    private List<String> authorityIds = Collections.EMPTY_LIST;
    private String identifier = "";
    
    public GoobiMetadataValue() {
        this.value = "";
    }
    
    public GoobiMetadataValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    
    public void setValue(String value) {
        this.value = value;
    }
    
    public List<String> getAuthorityIds() {
        return authorityIds;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public void setAuthorityIds(List<String> authorityIds) {
        this.authorityIds = authorityIds;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public int hashCode() {
        if(value != null) {            
            return value.hashCode();
        } else {
            return 0;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if(o != null && o.getClass().equals(GoobiMetadataValue.class)) {
            return this.getValue().equals(((GoobiMetadataValue) o).getValue());
        } else {
            return false;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(value);
        if(StringUtils.isNotBlank(identifier)) {
            sb.append(" (").append(identifier).append(")");
        }
        for (String string : authorityIds) {
            if(StringUtils.isNotBlank(string)) {
                sb.append(" (").append(string).append(")");
            }
        }
        return sb.toString();
    }
}
