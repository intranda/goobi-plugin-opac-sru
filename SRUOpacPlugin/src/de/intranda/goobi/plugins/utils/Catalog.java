/*************************************************************************
 * 
 * Copyright intranda GmbH
 * 
 * ************************* CONFIDENTIAL ********************************
 * 
 * [2003] - [2012] intranda GmbH, Bertha-von-Suttner-Str. 9, 37085 Göttingen, Germany 
 * 
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is protected by copyright. 
 * The source code contained herein is proprietary of intranda GmbH. 
 * The dissemination, reproduction, distribution or modification of 
 * this source code, without prior written permission from intranda GmbH, 
 * is expressly forbidden and a violation of international copyright law.
 * 
 *************************************************************************/

package de.intranda.goobi.plugins.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog configuration object.
 */
public class Catalog {

	private String id;
	private String name;
	private String protocol;
	private String url;
	private String port;
	private String database;
	private String login;
	private String password;
	private String default12;
	private String identifierPrefix;
	private String encoding;
	/** Format that shall be parsed. */
	private String format;
	/** Format with which the catalog is queried (optional). */
	private String formatString;
	private boolean addQuotationMarksToIdentifier = false;
	private Map<String, String> searchIdentifiers = new HashMap<String, String>();
	private Map<String, String> searchOperators = new HashMap<String, String>();
	private Map<String, List<FieldValueFilter>> filters = new HashMap<String, List<FieldValueFilter>>();

	/**
	 * Getters / setters.
	 * 
	 * @return id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param id The value to set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param name The value to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return protocol.
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param protocol The value to set.
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return url.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param url The value to set.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return port.
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param port The value to set.
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return database.
	 */
	public String getDatabase() {
		return database;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param database The value to set.
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return login.
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param login The value to set.
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param password The value to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return default12.
	 */
	public String getDefault12() {
		return default12;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param default12 The value to set.
	 */
	public void setDefault12(String default12) {
		this.default12 = default12;
	}

	/**
	 * @return the identifierPrefix
	 */
	public String getIdentifierPrefix() {
		return identifierPrefix;
	}

	/**
	 * @param identifierPrefix the identifierPrefix to set
	 */
	public void setIdentifierPrefix(String identifierPrefix) {
		this.identifierPrefix = identifierPrefix;
	}

	/**
	 * @return the encoding
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding the encoding to set
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * @return the format
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * @return the formatString
	 */
	public String getFormatString() {
		return formatString;
	}

	/**
	 * @param formatString the formatString to set
	 */
	public void setFormatString(String formatString) {
		this.formatString = formatString;
	}

	/**
	 * @return the addQuotationMarksToIdentifier
	 */
	public boolean isAddQuotationMarksToIdentifier() {
		return addQuotationMarksToIdentifier;
	}

	/**
	 * @param addQuotationMarksToIdentifier the addQuotationMarksToIdentifier to set
	 */
	public void setAddQuotationMarksToIdentifier(boolean addQuotationMarksToIdentifier) {
		this.addQuotationMarksToIdentifier = addQuotationMarksToIdentifier;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return searchIdentifiers.
	 */
	public Map<String, String> getSearchIdentifiers() {
		return searchIdentifiers;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param searchIdentifiers The value to set.
	 */
	public void setSearchIdentifiers(Map<String, String> searchIdentifiers) {
		this.searchIdentifiers = searchIdentifiers;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return searchOperators.
	 */
	public Map<String, String> getSearchOperators() {
		return searchOperators;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param searchOperators The value to set.
	 */
	public void setSearchOperators(Map<String, String> searchOperators) {
		this.searchOperators = searchOperators;
	}

	/**
	 * @return the filters
	 */
	public Map<String, List<FieldValueFilter>> getFilters() {
		return filters;
	}

	/**
	 * @param filters the filters to set
	 */
	public void setFilters(Map<String, List<FieldValueFilter>> filters) {
		this.filters = filters;
	}
}
