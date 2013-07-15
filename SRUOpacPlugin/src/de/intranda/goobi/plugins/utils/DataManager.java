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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;


/**
 * DataManager singleton.
 */
public final class DataManager {

	private static final Logger logger = Logger.getLogger(DataManager.class);

	public static final String version = "1.1.20130626";

	/** Result set TTL in miliseconds. */
	private static final long RESULT_TTL = 30000;

	private static final DataManager INSTANCE = new DataManager();

	/** Timer that checks for changes in the config files and repopulates the maps. */
	private Timer reloadTimer = new Timer();

	private XMLConfiguration configCatalogs;

	private PropertiesConfiguration configTagsMarc21;

	private PropertiesConfiguration configTagsMab2;

	/** Cached responses from the catalog, keyed via a fake session id. */
	private Map<String, Result> results = new HashMap<String, Result>();

	private Map<String, Catalog> catalogs;

	/** Maps MARC21 tags to PicaPlus tags. */
	private Map<String, String> tagsMarc21;

	/** Maps MAB2 tags to PicaPlus tags. */
	private Map<String, String> tagsMab2;

	private String iktList;

	/** Private constructor. */
	private DataManager() {
		// Check every 10 seconds for changed config files and refresh maps if necessary
		reloadTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if ((getConfigCatalogs() != null) && (getConfigCatalogs().getReloadingStrategy() != null)
						&& getConfigCatalogs().getReloadingStrategy().reloadingRequired()) {
					getConfigCatalogs().reload();
					refreshCatalogs();
				}
				if ((getConfigTagsMarc21() != null) && (getConfigTagsMarc21().getReloadingStrategy() != null)
						&& getConfigTagsMarc21().getReloadingStrategy().reloadingRequired()) {
					getConfigTagsMarc21().reload();
					refreshTagsMarc21();
				}
				if ((getConfigTagsMab2() != null) && (getConfigTagsMab2().getReloadingStrategy() != null)
						&& getConfigTagsMab2().getReloadingStrategy().reloadingRequired()) {
					getConfigTagsMab2().reload();
					refreshTagsMab2();
				}

				// Drop old result sets
				Date now = new Date();
				List<String> resultsToRemove = new ArrayList<String>();
				for (String s : getResults().keySet()) {
					Result result = getResults().get(s);
					if (result != null) {
						if ((now.getTime() - result.getTimestamp().getTime()) > RESULT_TTL) {
							getResults().remove(s);
						}
					}
				}
				for (String s : resultsToRemove) {
					logger.debug("Result set " + s + " has exceeded its TTL, dropping...");
					getResults().remove(s);
				}
			}
		}, 0, 10000);
	}

	/**
	 * Getters / setters.
	 * 
	 * @return INSTANCE.
	 **/
	public static DataManager getInstance() {
		return INSTANCE;
	}

	public static String getAppHome() {
		if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
			return "D:/digiverso/uci/";
		}

		return "/opt/digiverso/uci/";
	}

	/**
	 * Getters / setters.
	 * 
	 * @return configCatalogs.
	 **/
	public XMLConfiguration getConfigCatalogs() {
		return configCatalogs;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param configCatalogs The value to set.
	 **/
	public void setConfigCatalogs(XMLConfiguration configCatalogs) {
		this.configCatalogs = configCatalogs;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return configTagsMarc21.
	 **/
	public PropertiesConfiguration getConfigTagsMarc21() {
		return configTagsMarc21;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param configTagsMarc21 The value to set.
	 **/
	public void setConfigTagsMarc21(PropertiesConfiguration configTagsMarc21) {
		this.configTagsMarc21 = configTagsMarc21;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return configTagsMarc21.
	 **/
	public PropertiesConfiguration getConfigTagsMab2() {
		return configTagsMab2;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param configTagsMab2 The value to set.
	 **/
	public void setConfigTagsMab2(PropertiesConfiguration configTagsMab2) {
		this.configTagsMab2 = configTagsMab2;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return results.
	 **/
	public Map<String, Result> getResults() {
		return results;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param results The value to set.
	 **/
	public void setResults(Map<String, Result> results) {
		this.results = results;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return catalogs.
	 **/
	public Map<String, Catalog> getCatalogs() {
		return catalogs;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return tagsMarc21.
	 **/
	public Map<String, String> getTagsMarc21() {
		return tagsMarc21;
	}

	/**
	 * Getters / setters.
	 * 
	 * @return iktList.
	 **/
	public String getIktList() {
		return iktList;
	}

	/**
	 * Getters / setters.
	 * 
	 * @param iktList The value to set.
	 **/
	public void setIktList(String iktList) {
		this.iktList = iktList;
	}

	/**
	 * Returns the Pica translation of <code>marcTag</code>; null if no translation exists.
	 * 
	 * @param marcTag The MARC21 tag.
	 * @return The corresponding MARC21 tag; null if none found.
	 */
	public String getPicaPlusTagByMARC21(String marcTag) {
		return tagsMarc21.get(marcTag);
	}

	/**
	 * Returns the Pica translation of <code>mabTag</code>; null if no translation exists.
	 * 
	 * @param mabTag The MAB2 tag.
	 * @return The corresponding MAB2 tag; null if none found.
	 */
	public String getPicaPlusTagByMAB2(String mabTag) {
		return tagsMab2.get(mabTag);
	}

	/**
	 * Reads the catalogs XML file and refreshes the corresponding HashMap.
	 * 
	 * @param fileName The XML file to load.
	 * @throws ConfigurationException in case of errors.
	 * @should throw ConfigurationException if file does not exist
	 * @should load catalogs into DataManager successfully
	 * @should load load all catalogs from the file
	 * @should write correct values for all catalog properties
	 */
	public void loadCatalogs(final String fileName) throws ConfigurationException {
		configCatalogs = new XMLConfiguration(fileName);
		configCatalogs.setReloadingStrategy(new FileChangedReloadingStrategy());
		refreshCatalogs();
	}

	/**
	 * Populates the Pica main tag translation map from the given properties file.
	 * 
	 * @param fileName The .properties file to load.
	 * @return true if successful; false otherwise.
	 */
	public boolean loadTagsMarc21(final String fileName) {
		try {
			configTagsMarc21 = new PropertiesConfiguration(fileName);
			configTagsMarc21.setReloadingStrategy(new FileChangedReloadingStrategy());
			refreshTagsMarc21();
		} catch (ConfigurationException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Populates the Pica main tag translation map from the given properties file.
	 * 
	 * @param fileName The .properties file to load.
	 * @return true if successful; false otherwise.
	 */
	public boolean loadTagsMab2(final String fileName) {
		try {
			configTagsMab2 = new PropertiesConfiguration(fileName);
			configTagsMab2.setReloadingStrategy(new FileChangedReloadingStrategy());
			refreshTagsMab2();
		} catch (ConfigurationException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Populates the <code>catalogs</code> HashMap from <code>configCatalogs</code>.
	 */
	@SuppressWarnings("rawtypes")
	public void refreshCatalogs() {
		catalogs = new HashMap<String, Catalog>();
		List libs = configCatalogs.getList("catalogs.catalog[@id]");
		for (int i = 0; i < libs.size(); ++i) {
			Catalog cat = new Catalog();
			cat.setId(configCatalogs.getString("catalogs.catalog(" + i + ")[@id]"));
			cat.setName(configCatalogs.getString("catalogs.catalog(" + i + ").name"));
			cat.setProtocol(configCatalogs.getString("catalogs.catalog(" + i + ").protocol"));
			cat.setUrl(configCatalogs.getString("catalogs.catalog(" + i + ").url"));
			cat.setPort(configCatalogs.getString("catalogs.catalog(" + i + ").port"));
			cat.setDatabase(configCatalogs.getString("catalogs.catalog(" + i + ").db"));
			cat.setLogin(configCatalogs.getString("catalogs.catalog(" + i + ").login"));
			cat.setPassword(configCatalogs.getString("catalogs.catalog(" + i + ").password"));
			cat.setEncoding(configCatalogs.getString("catalogs.catalog(" + i + ").encoding"));
			cat.setFormat(configCatalogs.getString("catalogs.catalog(" + i + ").format"));
			cat.setFormatString(configCatalogs.getString("catalogs.catalog(" + i + ").formatString"));
			cat.setAddQuotationMarksToIdentifier(configCatalogs.getBoolean("catalogs.catalog(" + i + ").addQuotationMarksToIdentifier", false));
			cat.setIdentifierPrefix(configCatalogs.getString("catalogs.catalog(" + i + ").identifierPrefix"));
			cat.setDefault12(configCatalogs.getString("catalogs.catalog(" + i + ").default12"));
			List searchtags = configCatalogs.getList("catalogs.catalog(" + i + ").searchtag[@nr]");
			for (int j = 0; j < searchtags.size(); ++j) {
				cat.getSearchIdentifiers().put(configCatalogs.getString("catalogs.catalog(" + i + ").searchtag(" + j + ")[@nr]"),
						configCatalogs.getString("catalogs.catalog(" + i + ").searchtag(" + j + ")[@identifier]"));
				cat.getSearchOperators().put(configCatalogs.getString("catalogs.catalog(" + i + ").searchtag(" + j + ")[@nr]"),
						configCatalogs.getString("catalogs.catalog(" + i + ").searchtag(" + j + ")[@operator]"));
			}
			List filters = configCatalogs.getList("catalogs.catalog(" + i + ").filter[@pica]");
			for (int j = 0; j < filters.size(); ++j) {
				String picaTag = configCatalogs.getString("catalogs.catalog(" + i + ").filter(" + j + ")[@pica]");
				String regex = configCatalogs.getString("catalogs.catalog(" + i + ").filter(" + j + ")[@regex]");
				if (regex != null) {
					regex = regex.replace("_SPACE_", " ");
				}
				String replacement = configCatalogs.getString("catalogs.catalog(" + i + ").filter(" + j + ")");
				if (replacement != null) {
					replacement = replacement.replace("_SPACE_", " ");
				}
				FieldValueFilter filter = new FieldValueFilter(picaTag, regex, replacement);
				if (cat.getFilters().get(picaTag) == null) {
					List<FieldValueFilter> newList = new ArrayList<FieldValueFilter>();
					cat.getFilters().put(picaTag, newList);
				}
				cat.getFilters().get(picaTag).add(filter);
				// logger.debug("added filter: " + filter.toString());
			}
			getCatalogs().put(cat.getId(), cat);
		}
	}

	/**
	 * Populates the <code>tagsMarc21</code> HashMap from <code>configTagsMarc21</code>.
	 */
	@SuppressWarnings("rawtypes")
	public void refreshTagsMarc21() {
		tagsMarc21 = new HashMap<String, String>();
		Iterator keys = getConfigTagsMarc21().getKeys();
		while (keys.hasNext()) {
			String rawKey = ((String) keys.next());
			if (rawKey.equals("???")) {
				continue;
			}
			getTagsMarc21().put(rawKey, getConfigTagsMarc21().getString(rawKey));
			if (getTagsMarc21().get(rawKey).length() == 0) {
				getTagsMarc21().put(rawKey, null);
			}
		}
		logger.info("Loaded " + getTagsMarc21().size() + " MARC21 tags.");
	}

	/**
	 * Populates the <code>tagsMab2</code> HashMap from <code>configTagsMab2</code>.
	 */
	@SuppressWarnings("rawtypes")
	public void refreshTagsMab2() {
		tagsMab2 = new HashMap<String, String>();
		Iterator keys = getConfigTagsMab2().getKeys();
		while (keys.hasNext()) {
			String rawKey = ((String) keys.next());
			if (rawKey.equals("???")) {
				continue;
			}
			tagsMab2.put(rawKey, getConfigTagsMab2().getString(rawKey));
			if (tagsMab2.get(rawKey).length() == 0) {
				tagsMab2.put(rawKey, null);
			}
		}
		logger.info("Loaded " + tagsMab2.size() + " MAB2 tags.");
	}

    public Catalog getCatalogByName(String string) {
        for (String  key : catalogs.keySet()) {
            Catalog cat = catalogs.get(key);
            if(cat.getName().contains(string)) {
                return cat;
            }
        }
        return null;
    }
}
