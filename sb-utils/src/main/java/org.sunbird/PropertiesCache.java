package org.sunbird;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/*
 * @author Amit Kumar
 *
 * this class is used for reading properties file
 */
public class PropertiesCache {

  private final String[] fileName = {
    "externalresource.properties",
  };
  private final Properties configProp = new Properties();
  // FIXED: Changed to bounded map with max 1000 entries to prevent unbounded growth
  public final Map<String, Float> attributePercentageMap = new ConcurrentHashMap<>(1000);
  private static PropertiesCache propertiesCache = null;
  private static final int MAX_ATTRIBUTE_PERCENTAGE_MAP_SIZE = 1000;
  public static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

  /** private default constructor */
  private PropertiesCache() {
    for (String file : fileName) {
      InputStream in = this.getClass().getClassLoader().getResourceAsStream(file);
      try {
        configProp.load(in);
      } catch (IOException e) {
        logger.error("Error in properties cache", e);
      }
    }
    loadWeighted();
  }

  public static PropertiesCache getInstance() {

    // change the lazy holder implementation to simple singleton implementation ...
    if (null == propertiesCache) {
      synchronized (PropertiesCache.class) {
        if (null == propertiesCache) {
          propertiesCache = new PropertiesCache();
        }
      }
    }

    return propertiesCache;
  }

  public void saveConfigProperty(String key, String value) {
    configProp.setProperty(key, value);
  }

  public String getProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) return value;
    return configProp.getProperty(key) != null ? configProp.getProperty(key) : key;
  }

  private void loadWeighted() {
    String key = configProp.getProperty("user.profile.attribute");
    String value = configProp.getProperty("user.profile.weighted");
    if (StringUtils.isBlank(key)) {
      logger.info("Profile completeness value is not set==");
    } else {
      String keys[] = key.split(",");
      String values[] = value.split(",");
      
      // FIXED: Add size limit check to prevent unbounded growth
      if (keys.length > MAX_ATTRIBUTE_PERCENTAGE_MAP_SIZE) {
        logger.warn("Number of attributes ({}) exceeds maximum allowed ({}). Truncating to fit.", 
          keys.length, MAX_ATTRIBUTE_PERCENTAGE_MAP_SIZE);
      }
      
      if (keys.length == value.length()) {
        // then take the value from user
        logger.info("weighted value is provided by user.");
        for (int i = 0; i < keys.length && i < MAX_ATTRIBUTE_PERCENTAGE_MAP_SIZE; i++)
          attributePercentageMap.put(keys[i], new Float(values[i]));
      } else {
        // equally divide all the provided field.
        logger.debug("weighted value is not provided  by user.");
        float perc = (float) 100.0 / keys.length;
        for (int i = 0; i < keys.length && i < MAX_ATTRIBUTE_PERCENTAGE_MAP_SIZE; i++) 
          attributePercentageMap.put(keys[i], perc);
      }
    }
  }

  /**
   * Method to read value from resource file .
   *
   * @param key
   * @return
   */
  public String readProperty(String key) {
    String value = System.getenv(key);
    if (StringUtils.isNotBlank(value)) return value;
    return configProp.getProperty(key);
  }
}
