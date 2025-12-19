package com.oxygenxml.rest.plugin;

import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.extern.slf4j.Slf4j;

/**
 * Stores authentication headers for each user.
 * 
 * @author cristi_talau
 */
@Slf4j
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
	log.debug("Clearing cookies header for session: {}", sessionId);
    getAllHeaders(sessionId).remove(COOKIE_HEADER_NAME);
  }

  public void setBearerToken(String sessionId, String bearerToken) {
	log.debug("Setting bearer token for session: {}, token: {}", sessionId, "Bearer ******");
    getAllHeaders(sessionId).put(AUTHORIZATION_HEADER_NAME, "Bearer " + bearerToken);
  }
  
  /**
   * Adds authentication headers to the URL connection for the specified session.
   * Cookie headers take precedence over authorization headers. If a cookie
   * header is present, authorization headers will not be sent to maintain
   * traditional session-based authentication.
   *
   * @param sessionId The session identifier to retrieve headers for
   * @param connection The URL connection to add headers to
   * @return {@code true} if headers were added to the connection,
   *         {@code false} if no headers were available for this session
   */
  public boolean addHeaders(String sessionId, URLConnection connection) {
	log.debug("Adding headers to URLConnection for session: {}", sessionId);
    Map<String, String> allHeaders = getAllHeaders(sessionId);

    if (allHeaders.containsKey(COOKIE_HEADER_NAME)){
      // Cookie header takes precedence over authorization header.
      // This maintains traditional session-based authentication while
      // still allowing for token-based auth as a fallback.
      log.debug("Setting Cookie header for URL connection", sessionId);
      connection.setRequestProperty(COOKIE_HEADER_NAME, allHeaders.get(COOKIE_HEADER_NAME));
      return true;
    } else if (allHeaders.containsKey(AUTHORIZATION_HEADER_NAME)) {
      log.debug("Setting Authorization header for URL connection", sessionId);
      connection.setRequestProperty(AUTHORIZATION_HEADER_NAME, allHeaders.get(AUTHORIZATION_HEADER_NAME));
      return true;
    }
    return false;
  }
}
