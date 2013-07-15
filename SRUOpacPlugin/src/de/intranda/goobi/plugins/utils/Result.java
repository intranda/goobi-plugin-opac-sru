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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;

/**
 * Objects that contain the query result data.
 */
public class Result {

	/** JDOM representation of the query result XML document. */
	private Document doc;

	/** Map of Pica values retrieved from the source document via mapping (Map<picaMainTag, Map<picaSubTag, value>>). */
	private Map<String, List<Map<String, String>>> data = new HashMap<String, List<Map<String, String>>>();

	/** Main record ID. */
	private String mainId = "";

	/** Type of the main record ID to display (ISBN/ISSN/PPN/...). */
	private String mainIdType = "";

	/** true if the search produced a result. */
	private boolean hits = false;

	/** Creation date, used for TTL. */
	private Date timestamp;

	/**
	 * Constructor.
	 * 
	 * @param doc The result JDOM Document to set.
	 **/
	public Result() {
		this.timestamp = new Date();
	}

	/**
	 * Constructor with a JDOM Document.
	 * 
	 * @param doc The result JDOM Document to set.
	 **/
	public Result(Document doc) {
		this.doc = doc;
		this.timestamp = new Date();
	}

	/**
	 * Getters / setters.
	 * 
	 * @return doc.
	 */
	public Document getDoc() {
		return doc;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param doc The value to set.
	 */
	public void setDoc(Document doc) {
		this.doc = doc;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return data.
	 */
	public Map<String, List<Map<String, String>>> getData() {
		return data;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param data The value to set.
	 */
	public void setData(Map<String, List<Map<String, String>>> data) {
		this.data = data;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return mainId.
	 */
	public String getMainId() {
		return mainId;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param mainId The value to set.
	 */
	public void setMainId(String mainId) {
		this.mainId = mainId;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return mainIdType.
	 */
	public String getMainIdType() {
		return mainIdType;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param mainIdType The value to set.
	 */
	public void setMainIdType(String mainIdType) {
		this.mainIdType = mainIdType;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return hits.
	 */
	public boolean isHits() {
		return hits;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param hits The value to set.
	 */
	public void setHits(boolean hits) {
		this.hits = hits;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return timestamp.
	 */
	public Date getTimestamp() {
		return timestamp;
	}
}
