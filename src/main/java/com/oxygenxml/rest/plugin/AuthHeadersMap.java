package com.oxygenxml.rest.plugin;

import java.util.Collections;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Stores authentication headers for each user.
 * 
 * @author cristi_talau
 */
public class AuthHeadersMap {
  
  private final Cache<String, Map<String, String>> headersBySessionId =
      CacheBuilder.newBuilder()
        .concurrencyLevel(10)
        .maximumSize(10000)
        .build();
  
  
  public void setCookiesHeader(String sessionId, String cookies) {
    headersBySessionId.put(sessionId, Collections.singletonMap("Cookie", cookies));
  }
  
  public void clearCookiesHeader(String sessionId) {
    headersBySessionId.put(sessionId, Collections.emptyMap());  
  }

  public Map<String, String> getHeaders(String sessionId) {
    return headersBySessionId.getIfPresent(sessionId);
  }
}
