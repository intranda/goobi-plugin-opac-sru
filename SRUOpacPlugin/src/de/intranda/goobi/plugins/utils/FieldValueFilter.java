package de.intranda.goobi.plugins.utils;

public class FieldValueFilter {

	private String picaTag;
	private String regex;
	private String replacement;

	public FieldValueFilter(String picaTag, String regex, String replacement) {
		this.picaTag = picaTag;
		this.regex = regex;
		setReplacement(replacement);
	}

	/**
	 * @return the picaTag
	 */
	public String getPicaTag() {
		return picaTag;
	}

	/**
	 * @param picaTag the picaTag to set
	 */
	public void setPicaTag(String picaTag) {
		this.picaTag = picaTag;
	}

	/**
	 * @return the regex
	 */
	public String getRegex() {
		return regex;
	}

	/**
	 * @param regex the regex to set
	 */
	public void setRegex(String regex) {
		this.regex = regex;
	}

	/**
	 * @return the replacement
	 */
	public String getReplacement() {
		return replacement;
	}

	/**
	 * @param replacement the replacement to set
	 */
	public void setReplacement(String replacement) {
		this.replacement = replacement;
		if (this.replacement == null) {
			this.replacement = "";
		}
	}

	@Override
	public String toString() {
		return picaTag + ": '" + regex + "' -> '" + replacement + "'";
	}
}
