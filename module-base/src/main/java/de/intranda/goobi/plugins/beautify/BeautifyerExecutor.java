package de.intranda.goobi.plugins.beautify;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;

import de.unigoettingen.sub.search.opac.ConfigOpacCatalogueBeautifier;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogueBeautifierElement;

public class BeautifyerExecutor {

    private static final Namespace SRW = Namespace.getNamespace("srw", "http://www.loc.gov/zing/srw/");
    private static final Namespace MARC = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");

    private final Namespace marcNamespace;

    public BeautifyerExecutor() {
        marcNamespace = MARC;
    }

    public BeautifyerExecutor(Namespace marc) {
        marcNamespace = marc;
    }

    public void executeBeautifier(List<ConfigOpacCatalogueBeautifier> beautifySetList, Element record) {

        // run through all configured beautifier
        if (beautifySetList != null && !beautifySetList.isEmpty()) {
            for (ConfigOpacCatalogueBeautifier beautifier : beautifySetList) {
                List<ConfigOpacCatalogueBeautifierElement> conditionList = new ArrayList<>(beautifier.getTagElementsToProof());
                String newValue = null;

                // first, check if the current rule has conditions, check if conditions apply
                if (!conditionList.isEmpty()) {
                    for (ConfigOpacCatalogueBeautifierElement condition : beautifier.getTagElementsToProof()) {
                        for (Element field : record.getChildren()) {
                            // check if condition was configured for a leader position (tag starts with leader)
                            /*
                             * <condition tag="leader6" subtag="" value="e" />
                             */
                            if (field.getName().equalsIgnoreCase("leader")) {
                                if (condition.getTag().startsWith("leader")) {
                                    int pos = Integer.parseInt(condition.getTag().replace("leader", ""));
                                    // get value from pos, compare it with expected value
                                    String value = field.getValue();
                                    if (value.length() < 24) {
                                        value = "00000" + value;
                                    }
                                    char c = value.toCharArray()[pos];
                                    if (condition.getValue().equals("*") || condition.getValue().equals(Character.toString(c))) {
                                        conditionList.remove(condition);
                                    }
                                }
                            } else if (field.getName().equalsIgnoreCase("controlfield")) {
                                // check if a condition was defined (tag is numeric, but no subtag is defined)
                                /*
                                 * <condition tag="008" subtag="" value="*lat*" />
                                 */
                                if (condition.getTag().equals(field.getAttributeValue("tag"))) {
                                    // found field, now check if content matched
                                    String value = field.getValue();
                                    if (condition.getValue().equals("*") || value.matches(condition.getValue())) {
                                        conditionList.remove(condition);
                                        newValue = value;
                                    }
                                }
                                // check if a condition was defined for datafield / subfield (tag and subtag are defined)
                                /*
                                 * <condition tag="041" subtag="a" value="lat" />
                                 */
                            } else if (field.getName().equalsIgnoreCase("datafield")) {
                                if (condition.getTag().equals(field.getAttributeValue("tag"))) {
                                    // found main field, check subfields
                                    List<Element> subelements = field.getChildren();
                                    for (Element subfield : subelements) {
                                        String subtag = subfield.getAttributeValue("code");
                                        if (condition.getSubtag().equals(subtag)) {
                                            // found subfield, now check if content matched
                                            if (condition.getValue().equals("*") || subfield.getText().matches(condition.getValue())) {
                                                conditionList.remove(condition);
                                                newValue = subfield.getText();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Element mainField = null;
                Element subField = null;
                // if conditions are fulfilled, search for field to change
                if (conditionList.isEmpty()) {
                    for (Element field : record.getChildren()) {
                        if (field.getName().equalsIgnoreCase("leader")) {
                            if (beautifier.getTagElementToChange().getTag().startsWith("leader")) {
                                int pos = Integer.parseInt(beautifier.getTagElementToChange().getTag().replace("leader", ""));
                                // create new leader, replace position with configured value
                                String value = field.getText();
                                value = value.substring(0, pos) + beautifier.getTagElementToChange().getValue().replace("\\u0020", " ")
                                        + value.substring(pos + 1);
                                newValue = value;
                                mainField = field;
                            }
                        } else if (field.getName().equalsIgnoreCase("controlfield")) {
                            if (beautifier.getTagElementToChange().getTag().equals(field.getAttributeValue("tag"))) {
                                // found field to change
                                mainField = field;

                            }

                        } else if (field.getName().equalsIgnoreCase("datafield")) {
                            if (beautifier.getTagElementToChange().getTag().equals(field.getAttributeValue("tag"))) {
                                // found main field, check subfields
                                mainField = field;
                                List<Element> subelements = field.getChildren();
                                for (Element subfield : subelements) {
                                    String subtag = subfield.getAttributeValue("code");
                                    if (beautifier.getTagElementToChange().getSubtag().equals(subtag)) {
                                        // found subfield to change
                                        subField = subfield;
                                    }
                                }
                            }
                        }
                    }
                }
                // replace existing field or create a new field
                if (beautifier.getTagElementToChange().getTag().startsWith("leader") && mainField != null) {
                    mainField.setText(newValue);
                } else if (newValue != null) {
                    // if '*' was used, replace current value with value from condition, otherwise use value from configuration
                    //
                    if (!"*".equals(beautifier.getTagElementToChange().getValue())) {
                        newValue = beautifier.getTagElementToChange().getValue().replace("\\u0020", " ");
                    }
                    if (StringUtils.isNotBlank(beautifier.getTagElementToChange().getTag())
                            && StringUtils.isBlank(beautifier.getTagElementToChange().getSubtag())) {
                        if (mainField == null) {
                            mainField = new Element("controlfield", marcNamespace);
                            mainField.setAttribute("tag", beautifier.getTagElementToChange().getTag());
                            record.addContent(mainField);
                        }
                        mainField.setText(newValue);
                    } else {
                        if (mainField == null) {
                            mainField = new Element("datafield", marcNamespace);
                            mainField.setAttribute("tag", beautifier.getTagElementToChange().getTag());
                            mainField.setAttribute("ind1", " ");
                            mainField.setAttribute("ind2", " ");
                            record.addContent(mainField);
                        }
                        if (subField == null) {
                            subField = new Element("subfield", marcNamespace);
                            subField.setAttribute("code", beautifier.getTagElementToChange().getSubtag());
                            mainField.addContent(subField);
                        }
                        subField.setText(newValue);
                    }
                }

            }
        }
    }

}
