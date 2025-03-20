package org.sunbird.helper;

import org.sunbird.common.Constants;

import java.util.HashMap;
import java.util.Map;

/** Created by arvind on 10/10/17. */
public class CassandraConnectionMngrFactory {

  private static Map<String, CassandraConnectionManager> connectionFactoryMap = new HashMap<>();

  /**
   * Factory method to get the cassandra connection manager oject on basis of mode name pass in
   * argument . default mode is standalone mode .
   *
   * @param name
   * @return
   */
  public static CassandraConnectionManager getObject(String name) {

    if (!connectionFactoryMap.containsKey(name)) {
      // create object
      synchronized (CassandraConnectionMngrFactory.class) {
          if (null == connectionFactoryMap.get(Constants.STANDALONE_MODE)) {
            String mode = Constants.STANDALONE_MODE;
            CassandraConnectionManager standaloneCassandraConnectionManager =
                new CassandraConnectionManagerImpl(mode);
            connectionFactoryMap.put(Constants.STANDALONE_MODE, standaloneCassandraConnectionManager);
          }
        }
      }
    return connectionFactoryMap.get(name);
  }
}
