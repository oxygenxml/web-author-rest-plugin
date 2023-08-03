package com.oxygenxml.rest.plugin;

import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Stores authentication headers for each user.
 * 
 * @author cristi_talau
 */
public class AuthHeadersMap {
  
  private static final String COOKIE_HEADER_NAME = "Cookie";

  private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
  
  private final LoadingCache<String, Map<String, String>> headersBySessionId =
      CacheBuilder.newBuilder()
        .concurrencyLevel(10)
        .maximumSize(10000)
        .build(new CacheLoader<String, Map<String, String>>() {
          @Override
          public Map<String, String> load(String key) throws Exception {
            return new HashMap<>();
          }
        });
  
  
  public void setCookiesHeader(String sessionId, String cookies) {
    Map<String, String> headers = getAllHeaders(sessionId);
    headers.put(COOKIE_HEADER_NAME, cookies);
  }

  public Map<String, String> getAllHeaders(String sessionId) {
    try {
      return headersBySessionId.get(sessionId);
    } catch (ExecutionException e) {
      // Cannot happen
      throw new RuntimeException(e);
    }
  }
  
  public void clearCookiesHeader(String sessionId) {
    getAllHeaders(sessionId).remove(COOKIE_HEADER_NAME);
  }

  public void setBearerToken(String sessionId, String bearerToken) {
    getAllHeaders(sessionId).put(AUTHORIZATION_HEADER_NAME, "Bearer " + bearerToken);
  }
  
  public void addHeaders(String sessionId, URLConnection connection) {
    Map<String, String> allHeaders = getAllHeaders(sessionId);
    
    if (allHeaders.containsKey(AUTHORIZATION_HEADER_NAME)) {
      // The authorization header takes precedence over cookies.
      // This is because we always have some cookies recorded, but if we have an Authorization header,
      // it means that we are calling a state-less REST API. We do not send Cookies in this case.
      connection.setRequestProperty(AUTHORIZATION_HEADER_NAME, allHeaders.get(AUTHORIZATION_HEADER_NAME));
    } else if (allHeaders.containsKey(COOKIE_HEADER_NAME)){
      connection.setRequestProperty(COOKIE_HEADER_NAME, allHeaders.get(COOKIE_HEADER_NAME));
    }
  }
}
